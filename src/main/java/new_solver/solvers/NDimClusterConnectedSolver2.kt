package new_solver.solvers;

import config.Consts.INF
import config.Consts.STEP
import config.Consts.TL
import config.Flags
import ext.CplexKtExt
import ilog.concert.IloNumVar
import ilog.cplex.IloCplex
import ilog.cplex.IloCplex.Callback.Context
import ilog.cplex.IloCplex.Param
import legacy.graph.Graph
import legacy.utils.Matrix
import new_solver.common.computeIf
import new_solver.helpers.angleDeg
import new_solver.helpers.clustering.direction_to_cluster_mapper.Cluster
import new_solver.helpers.clustering.direction_to_cluster_mapper.DirectionsToClustersMapper
import new_solver.helpers.clustering.directions_provider.ClusterDirectionsProvider
import new_solver.helpers.model.NDimClustersVariables.*
import new_solver.helpers.model.solver.NDimClusterConnectedActualVariables
import new_solver.helpers.model.solver.NDimClusterConnectedIloVariables
import new_solver.helpers.model.solver.NDimClusterConnectedSolverConstants
import new_solver.helpers.solution_adapter.NDimClusterConnectedSolutionAdapter
import new_solver.helpers.times
import new_solver.solvers.callbacks.generic.GenCallback
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlin.random.Random

class NDimClusterConnectedSolver2(
    private val maxClusters: Int,
    private val useMipStart: Boolean,
    private val clusteringMinClusterSize: Int,
    private val clusteringAddPriorities: Boolean,
    private val adapter: NDimClusterConnectedSolutionAdapter,
    private val clusterDirectionsProvider: ClusterDirectionsProvider,
    private val directionToClusterMapper: DirectionsToClustersMapper
) : Solver {

    private lateinit var log: PrintWriter

    private lateinit var matrix: Matrix
    private var n: Int = -1
    private var d: Int = -1
    private var e: Int = -1

    private lateinit var graph: Graph
    private val hasGraph get() = e != 0

    lateinit var v: NDimClusterConnectedIloVariables
    lateinit var consts: NDimClusterConnectedSolverConstants

    val cplex: IloCplex = IloCplex()
    private val cplexKtExt: CplexKtExt = CplexKtExt(cplex)

    lateinit var clusters: Array<Cluster>

    fun addInput(matrix: Matrix, graph: Graph) {
        this.log = PrintWriter("./logs/connect_callback_solver.txt", StandardCharsets.UTF_8)

        this.matrix = matrix
        this.n = matrix.numRows()
        this.d = matrix.numCols()
        this.graph = graph
        this.e = graph.edges.size

        this.graph = graph

        val clustersDirection = clusterDirectionsProvider.provide(matrix.entry.data)
        this.clusters = directionToClusterMapper.invoke(clustersDirection, matrix)
            .filter { it.points.size >= clusteringMinClusterSize }
            .sortedByDescending { it.points.size }
            .take(maxClusters)
            .toTypedArray()
            .also { println(it.size) }

        this.cplex.setParam(Param.OptimalityTarget, IloCplex.OptimalityTarget.OptimalGlobal)
        this.cplex.setParam(Param.TimeLimit, TL.toDouble())
        this.cplex.setParam(Param.Threads, 6)
        this.cplex.setParam(Param.Parallel, -1)
//        this.cplex.setParam(Param.MIP.Cuts.BQP, 3) // https://www.ibm.com/docs/en/icos/20.1.0?topic=parameters-boolean-quadric-polytope-cuts
//        this.cplex.setParam(Param.Barrier.Algorithm, 3) // https://www.ibm.com/docs/en/icos/20.1.0?topic=parameters-barrier-algorithm
//        this.cplex.setParam(Param.MIP.Strategy.Dive, 1) // https://www.ibm.com/docs/en/icos/20.1.0?topic=parameters-mip-dive-strategy
//        this.cplex.setParam(Param.MIP.Strategy.Branch, 1) // https://www.ibm.com/docs/en/icos/12.8.0.0?topic=parameters-mip-branching-direction
//        this.cplex.setParam(Param.MIP.Strategy.NodeSelect, 0) // https://www.ibm.com/docs/en/icos/22.1.0?topic=parameters-mip-node-selection-strategy
//        this.cplex.setParam(Param.Preprocessing.Symmetry, 5)
//        this.cplex.setParam(Param.MIP.Cuts.Disjunctive, 2)
//        this.cplex.setParam(Param.MIP.Cuts.Gomory, 2)
//        this.cplex.setParam(Param.Simplex.Tolerances.Markowitz, 0.9999)
//        this.cplex.setParam(Param.RootAlgorithm, 4) //https://www.ibm.com/docs/en/icos/20.1.0?topic=parameters-algorithm-continuous-linear-problems
//        this.cplex.setParam(Param.MIP.Strategy.VariableSelect, 4) // https://www.ibm.com/docs/en/icos/20.1.0?topic=parameters-mip-variable-selection-strategy
//        this.cplex.setParam(Param.MIP.Strategy.Search, 1) // https://www.ibm.com/docs/en/icos/12.8.0.0?topic=parameters-mip-dynamic-search-switch
//        this.cplex.setParam(Param.MIP.Strategy.Probe, -1) // https://www.ibm.com/docs/en/icos/20.1.0?topic=parameters-mip-probing-level
        this.cplex.setParam(Param.Emphasis.MIP, 5)
        this.cplex.setParam(Param.MIP.Strategy.FPHeur, 2)
//        this.cplex.setParam(Param.Emphasis.Numerical, true) // https://www.ibm.com/docs/en/icos/20.1.0?topic=parameters-numerical-precision-emphasis

        this.v = NDimClusterConnectedIloVariables(n, d, e, 2 * (d - 1), clusters, cplex)
        this.consts = NDimClusterConnectedSolverConstants(matrix, n = n, d = d, graph, clusters)

        val clusterEndpointsSize = 2 * (d - 1)
        addOptimisation()

        computeIf(useMipStart) { addMipStart(clustersDirection, clusterEndpointsSize) }

        val availableContexts =
            Context.Id.Relaxation or
                    Context.Id.GlobalProgress or
                    Context.Id.Branching
        this.cplex.use(GenCallback(v, consts, adapter), availableContexts)

        computeIf(clusteringAddPriorities) {
            val allAlphaSigns = v.clusters.map { it.fixedSign }
            cplex.setPriorities(
                allAlphaSigns.toTypedArray(),
                IntArray(allAlphaSigns.size) { 10 }
            )
        }
    }

    fun addMipStart(clusters: List<DoubleArray>, clusterEndpointsSize: Int) {
        clusters.forEach { cluster ->
            val mipStart = NDimClusterConnectedActualVariables(
                a = cluster.toTypedArray(),
                f = Array(n) { 0.0 },
                g = Array(n) { 0.0 },
                alpha = Array(n) { 0.0 },
                r = if (hasGraph) Array(n) { 0.0 } else null,
                q = if (hasGraph) Array(n) { 0.0 } else null,
                s = if (hasGraph) Array(n) { 0.0 } else null,
                t = if (hasGraph) Array(n) { 0.0 } else null,
                x = if (hasGraph) Array(e) { 0.0 } else null,
                y = if (hasGraph) Array(e) { 0.0 } else null,
                clusters = Array(v.clusters.size) {
                    ClusterVars(
                        fixedSign = 0.0,
                        alphaSign = Array(clusterEndpointsSize) { 0.0 })
                },
            )

            adapter.adapt(mipStart, consts)
            cplex.addMIPStart(v.allVariables().toTypedArray(), mipStart.allVariables().toDoubleArray())
        }

        matrix.entry.data.forEach { x ->
            val mipStart = NDimClusterConnectedActualVariables(
                a = x.toTypedArray(),
                f = Array(n) { 0.0 },
                g = Array(n) { 0.0 },
                alpha = Array(n) { 0.0 },
                r = Array(n) { 0.0 },
                q = Array(n) { 0.0 },
                s = Array(n) { 0.0 },
                t = Array(n) { 0.0 },
                x = Array(e) { 0.0 },
                y = Array(e) { 0.0 },
                clusters = Array(v.clusters.size) {
                    ClusterVars(
                        fixedSign = 0.0,
                        alphaSign = Array(clusterEndpointsSize) { 0.0 })
                },
            )
            adapter.adapt(mipStart, consts)
            cplex.addMIPStart(v.allVariables().toTypedArray(), mipStart.allVariables().toDoubleArray())
        }
    }

    fun addOptimisation() {
        fun addObjective() = with(cplexKtExt) {
            val n10 = 1.0 //n * 10.0
            val squares = cplex.sum(Array(d) { i -> (v.a[i] * v.a[i]) * n10 })
            if (hasGraph) {
                val errF = cplex.sum(Array(n) { i -> (v.f[i] - v.q[i]).let { it * it } })
                val errG = cplex.sum(Array(n) { i -> (v.g[i] - v.t[i]).let { it * it } })
                cplex.addMaximize((squares - errF) - errG)
            } else {
                cplex.addMaximize((squares))
            }
        }


        fun addConstraint() = with(cplexKtExt) {
            for (i in 0 until n) {
                cplex.scalProd(matrix.getRow(i), v.a) aEq (v.f[i] - v.g[i])
            }

            (cplex.sum(v.f) + cplex.sum(v.g)) aEq (n.toDouble())
            for (i in 0 until n) {
                v.f[i] aLe (v.alpha[i] * INF)
                v.g[i] aLe ((1.0 - v.alpha[i]) * INF)
            }
        }


        fun addConnectConstraint() = with(cplexKtExt) {
            cplex.sum(v.r) aEq 1.0
            cplex.sum(v.s) aEq 1.0

            for (num in 0 until e step 2) {
                val backNum = Graph.companionEdge(num)

                (v.x[num] + v.x[backNum]) aLe 1.0
                (v.y[num] + v.y[backNum]) aLe 1.0
            }

            for (vertex in 0 until n) {
                val inputEdgesX = Array<IloNumVar?>(graph.edgesOf(vertex).size) { null }
                val inputEdgesY = Array<IloNumVar?>(graph.edgesOf(vertex).size) { null }
                for ((i_1, to) in graph.edgesOf(vertex).withIndex()) {
                    val num = to.second.toInt()
                    val backNum = Graph.companionEdge(num)

                    inputEdgesX[i_1] = (v.x[backNum])
                    inputEdgesY[i_1] = (v.y[backNum])
                }
                (cplex.sum(inputEdgesX) + v.r[vertex]) aEq 1.0
                (cplex.sum(inputEdgesY) + v.s[vertex]) aEq 1.0
            }
            for (num in 0 until e) {
                val edge = graph.edges[num]

                (INF + (v.q[edge.first] - v.q[edge.second])) aGe ((INF * v.x[num]) + STEP)
                (INF + (v.t[edge.first] - v.t[edge.second])) aGe ((INF * v.y[num]) + STEP)
            }
        }

        fun addClusterConstraint() = with(cplexKtExt) {
            println("CLUSTERING")
            println("With average: " + clusters.map { it.points.size }.average())

            for (i in clusters.indices) {

                val cluster = clusters[i]

                println("In The End: " + cluster.points.size)

                val clusterV = v.clusters[i]

                cluster.endpoints.forEachIndexed { endpointI, endpoint ->
                    val scalProd = cplex.scalProd(endpoint, v.a)

                    scalProd aLe (clusterV.alphaSign[endpointI] * INF)
                    scalProd aGe ((1.0 - clusterV.alphaSign[endpointI]) * -INF)
                }

                val clusterPoint = cluster.points.first()
                val alphaOfGroup = v.alpha[clusterPoint]

                val oneSubFs = 1.0 - clusterV.fixedSign
                val fsSubOne = clusterV.fixedSign - 1.0
                cluster.points.forEach { pointIndex ->
                    if (pointIndex != clusterPoint) {
                        val alphaSubtractGroup = (v.alpha[pointIndex] - alphaOfGroup)

                        (alphaSubtractGroup) aLe (oneSubFs)
                        (alphaSubtractGroup) aGe (fsSubOne)
                    }
                }
                val clusterD = clusterV.alphaSign.size.toDouble()

                val alphaSums = cplex.sum(clusterV.alphaSign)

                (alphaSums - clusterD) aLe fsSubOne
                alphaSums aGe oneSubFs
            }
        }

        /*
        fun addOrtantsConstraint() = with(cplexKtExt) {
            val orts = OrtantClustering().provide(matrix)
            var ssum = 0
            orts.forEach {
                ssum += it.value.second.size
                println("Cluster with ${it.value.second.size} points")
            }
            println(ssum)

            v.clusters = Array(orts.size) { i ->
                ClusterVars(
                    fixedSign = cplex.numVar(0.0, 1.0, IloNumVarType.Int, "fixedSign".toVarName(i)),
                    alphaSign = Array(d) {
                        cplex.numVar(0.0, 1.0, IloNumVarType.Int, "alphaSign".toVarName(i, it))
                    }
                )
            }
            var i = -1
            orts.forEach { ort ->
                i++
                val ep = ort.value.first
                val points = ort.value.second

                val epss = ep.mapIndexed { x, dd ->
                    DoubleArray(d) { xx -> if (x == xx) dd else 0.0 }
                }

                epss.forEachIndexed { endpointI, endpoint ->
                    val scalProd = cplex.scalProd(endpoint, v.a)
                    (scalProd) aLe (v.clusters[i].alphaSign[endpointI] * INF)
                    (scalProd) aGe ((1.0 - v.clusters[i].alphaSign[endpointI]) * (-INF))
                }

                val clusterPoint = points.first()
                val alphaOfGroup = v.alpha[clusterPoint]

                points.forEach { pointIndex ->
                    val oneSubFs = 1.0 - v.clusters[i].fixedSign
                    val fsSubOne = v.clusters[i].fixedSign - 1.0

                    val alphaSubtractGroup = (v.alpha[pointIndex] - alphaOfGroup)
                    (alphaSubtractGroup) aLe (oneSubFs)
                    (alphaSubtractGroup) aGe (fsSubOne)

                    val betaSubtractGroup = v.alpha[clusterPoint] - v.alpha[pointIndex]
                    (betaSubtractGroup) aLe oneSubFs
                    (betaSubtractGroup) aGe fsSubOne
                }

                val alphaSums = cplex.sum(v.clusters[i].alphaSign)

                (alphaSums - (d - 1.0)) aLe v.clusters[i].fixedSign
                (1.0 - alphaSums) aLe v.clusters[i].fixedSign
            }
        }
        */

        addObjective()
        addConstraint()
        if (hasGraph) {
            addConnectConstraint()
        }
        addClusterConstraint()
    }

    override fun solve(): Boolean {
        val result = cplex.solve()
        println(cplex.status)
        println(cplex.status == IloCplex.Status.Optimal)
        return result
    }

    fun printResults(writer: PrintWriter) = writer.use { out ->
        for (i in 0 until d) {
            out.println("a$i = ${cplex.getValue(v.a[i])}")
        }
        for (i in 0 until n) {
            print("${cplex.getValue(v.f[i])} ")
        }
        println()
        for (i in 0 until n) {
            print("${cplex.getValue(v.g[i])} ")
        }
        println()



        cplex.getValues(v.a).also { out.println("a"); out.println(it.toList()) }
        cplex.getValues(v.f).also { out.println("f"); out.println(it.toList()) }
        cplex.getValues(v.g).also { out.println("g"); out.println(it.toList()) }
        cplex.getValues(v.alpha).also { out.println("alpha"); out.println(it.toList()) }
        if (hasGraph) {
            cplex.getValues(v.q).also { out.println("q"); out.println(it.toList()) }
            cplex.getValues(v.t).also { out.println("t"); out.println(it.toList()) }
            cplex.getValues(v.r).also { out.println("r"); out.println(it.toList()) }
            cplex.getValues(v.s).also { out.println("s"); out.println(it.toList()) }
            cplex.getValues(v.x).also { out.println("x"); out.println(it.toList()) }
            cplex.getValues(v.y).also { out.println("y"); out.println(it.toList()) }
        }
    }

    override fun writeResult() {
        fun DoubleArray.printIt(title: String) {
            print("$title: ")
            forEach { print(" $it,") }.also { println() }
        }

        println("RESULT")
        println(cplex.objValue)

        for (i in 0 until d) {
            println("a$i = ${cplex.getValue(v.a[i])}")
        }
        for (i in 0 until n) {
            print("${cplex.getValue(v.f[i])} ")
        }
        println()
        for (i in 0 until n) {
            print("${cplex.getValue(v.g[i])} ")
        }
        println()

        cplex.getValues(v.f).printIt("f")
        cplex.getValues(v.g).printIt("g")
        cplex.getValues(v.a).printIt("a")
        cplex.getValues(v.alpha).printIt("Alpha")
        if (hasGraph) {
            cplex.getValues(v.q).printIt("q")
            cplex.getValues(v.t).printIt("t")
            cplex.getValues(v.r).printIt("r")
            cplex.getValues(v.s).printIt("s")
            cplex.getValues(v.x).printIt("x")
            cplex.getValues(v.y).printIt("y")
        }

        println(cplex.status)
        println(cplex.status == IloCplex.Status.Optimal)
    }

    override fun icaDirection(): DoubleArray = cplex.getValues(v.a)
}