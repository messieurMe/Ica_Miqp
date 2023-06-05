//package new_solver.solvers
//
//import config.Flags
//import ilog.concert.IloNumExpr
//import ilog.concert.IloNumVar
//import ilog.concert.IloNumVarType
//import ilog.cplex.IloCplex
//import legacy.algo.MST
//import legacy.graph.Graph
//import legacy.utils.Matrix
//import legacy.utils.Pair
//import config.Consts.INF
//import config.Consts.TL
//import ext.CplexKtExt
//import new_solver.common.toVarName
//import new_solver.helpers.clustering.direction_to_cluster_mapper.DirectionToClusterMapperWithCircle
//import new_solver.solvers.model.Logs
//import java.io.PrintWriter
//import java.util.*
//
//class NDimClusterConnectedSolver : Solver{
//
//    var matrix: Matrix
//    var d: Int
//    var n: Int
//    var inf: Double
//    val cplex: IloCplex
//    val cplexKtExt: CplexKtExt
//    var v: Variable
//    val clusters: Array<DoubleArray>
//
//    val graph: Graph
//    var e: Int
//    val STEP: Double = 1.0
//
//
//    class Variable(
//        val a: Array<IloNumVar>,
//        val f: Array<IloNumVar>,
//        val g: Array<IloNumVar>,
//        val alpha: Array<IloNumVar>,
//        val r: Array<IloNumVar>,
//        val q: Array<IloNumVar>,
//        val x: Array<IloNumVar>,
//        val s: Array<IloNumVar>,
//        val t: Array<IloNumVar>,
//        val y: Array<IloNumVar>,
//
//        val clusters: ArrayList<ClusterVars> = ArrayList()
//    ) {
//        class ClusterVars(
//            val fixedSign: IloNumVar,
//            val fSign: Array<IloNumVar>,
//            val gSign: Array<IloNumVar>,
//            val alphaSign: Array<IloNumVar>,
//
//            )
//
//        val allVars = arrayOf(
//            *a,*f, *g, *alpha, *r, *q, *x, *s, *t, *y
//        )
//    }
//
//    constructor(matrix: Matrix, graph: Graph, clusters: Array<DoubleArray>, step: Double) {
//        this.matrix = matrix
//        this.n = matrix.numRows()
//        this.d = matrix.numCols()
//        this.inf = INF
//        this.graph = graph
//        this.e = graph.edges.size
//        this.clusters = clusters
//        this.cplex = IloCplex()
//        this.cplex.setParam(IloCplex.Param.OptimalityTarget, IloCplex.OptimalityTarget.OptimalGlobal)
//        this.cplex.setParam(IloCplex.Param.TimeLimit, TL.toDouble())
//        this.cplexKtExt = CplexKtExt(cplex)
//
//        this.v = addVariable()
//
//
//        simpleOptimisation()
//    }
//
//    private fun addVariable(): Variable {
//        return Variable(
//            a = Array(d) { cplex.numVar(-inf, inf, IloNumVarType.Float, "a".toVarName(it)) },
//            f = Array(n) { cplex.numVar(0.0, inf, IloNumVarType.Float, "f".toVarName(it)) },
//            g = Array(n) { cplex.numVar(0.0, inf, IloNumVarType.Float, "g".toVarName(it)) },
//            alpha = Array(n) { cplex.numVar(0.0, 1.0, IloNumVarType.Int, "alpha".toVarName(it)) },
//            r = Array(n) { cplex.numVar(0.0, 1.0, IloNumVarType.Int, "r".toVarName(it)) },
//            q = Array(n) { cplex.numVar(0.0, inf, IloNumVarType.Float, "q".toVarName(it)) },
//            s = Array(n) { cplex.numVar(0.0, 1.0, IloNumVarType.Int, "s".toVarName(it)) },
//            t = Array(n) { cplex.numVar(0.0, inf, IloNumVarType.Float, "t".toVarName(it)) },
//            x = Array(e) { cplex.numVar(0.0, 1.0, IloNumVarType.Int, "x".toVarName(it)) },
//            y = Array(e) { cplex.numVar(0.0, 1.0, IloNumVarType.Int, "y".toVarName(it)) }
//        )
//    }
//
//    private fun simpleOptimisation() {
//        fun addObjective() {
//            val squares = Array<IloNumExpr>(d) { i ->
//                cplex.prod(v.a[i], v.a[i])
//            }
//            cplex.addMaximize(cplex.sum(squares))
//        }
//
//        fun addConstraint() = with(cplexKtExt) {
//            for (i in 0 until n) {
//                cplex.addEq(cplex.scalProd(matrix.getRow(i), v.a), v.f[i] - v.g[i])
//            }
//            val l1NormP = Array(n) { i -> (v.f[i] + v.g[i]) }
//            cplex.addEq(cplex.sum(l1NormP), n.toDouble())
//
//            for (i in 0 until n) {
//                cplex.addLe(v.f[i], v.alpha[i] * inf)
//                cplex.addLe(v.g[i], (1.0 - v.alpha[i]) * inf)
//            }
//        }
//
//        fun initClusters() = with(cplexKtExt) {
//            val clusters = DirectionToClusterMapperWithCircle(
//                clusters,
//                matrix,
//                )
//            println("CLUSTERING")
//            println("With average: " + clusters.map { it.points.size }.average())
//
//            for (i in clusters.indices) {
//                val cluster = clusters[i]
//
//                if (cluster.points.size < Flags.clusteringMinClusterSize) {
//                    continue
//                } else {
//                    println("CLUSTER CREATED WITH ${cluster.points.size} POINTS")
//                }
//
//
//                if(cluster.points.isEmpty()) continue
//                val cv = Variable.ClusterVars(
//                    fixedSign = cplex.numVar(0.0, 1.0, IloNumVarType.Int, "fixedSign".toVarName(i)),
//                    fSign = Array(cluster.endpoints.size) {
//                        cplex.numVar(0.0, inf, IloNumVarType.Float, "fSign".toVarName(i * 100 + it))
//                    },
//                    gSign = Array(cluster.endpoints.size) {
//                        cplex.numVar(
//                            0.0,
//                            inf,
//                            IloNumVarType.Float,
//                            "gSign".toVarName(i * 100 + it)
//                        )
//                    },
//                    alphaSign = Array(cluster.endpoints.size) {
//                        cplex.numVar(
//                            0.0,
//                            1.0,
//                            IloNumVarType.Int,
//                            "alphaSign".toVarName(i * 100 + it)
//                        )
//                    }
//                ).also(v.clusters::add)
//
//                cluster.endpoints.forEachIndexed { endpointI, endpoint ->
//                    cplex.addEq(cplex.scalProd(endpoint, v.a), cv.fSign[endpointI] - cv.gSign[endpointI])
//                    cv.fSign[endpointI] aLe (cv.alphaSign[endpointI] * inf)
//                    cv.gSign[endpointI] aLe ((1.0 - cv.alphaSign[endpointI]) * inf)
//                }
//
//                val clusterPoint = cluster.points.first()
//                val alphaOfGroup = v.alpha[clusterPoint]
//                val betaOfGroup = 1.0 - v.alpha[clusterPoint]
//
//                cluster.points.forEach { pointIndex ->
//                    val alphaSubtractGroup = (v.alpha[pointIndex] - alphaOfGroup)
//                    (alphaSubtractGroup) aLe (1.0 - cv.fixedSign)
//                    (alphaSubtractGroup) aLe (cv.fixedSign - 1.0)
//
//                    val betaSubtractGroup = ((1.0 - v.alpha[pointIndex]) - betaOfGroup)
//                    (betaSubtractGroup) aLe (1.0 - cv.fixedSign)
//                    (betaSubtractGroup) aLe (cv.fixedSign - 1.0)
//                }
//                val clusterD = cluster.endpoints.size
//
//                val alphaSums = cplex.sum(cv.alphaSign)
//                (alphaSums - (clusterD - 1.0)) aLe (cv.fixedSign)
//
//                val betaSums = cplex.sum(cv.alphaSign.map { 1.0 - it }.toTypedArray())
//                (betaSums - (clusterD - 1.0)) aLe cv.fixedSign
//            }
//        }
//
//
//        fun addConnectionConstraint() = with(cplexKtExt) {
//            cplex.addEq(cplex.sum(v.r), 1.0)
//            cplex.addEq(cplex.sum(v.s), 1.0)
//
//            for (num in 0 until e step 2) {
//                val backNum = Graph.companionEdge(num)
//                if (backNum != num + 1) {
//                    throw Exception("")
//                }
//
//                Graph.checkEdges(graph, num, backNum)
//
//                (v.x[num] + v.x[backNum]) aLe 1.0
//                (v.y[num] + v.y[backNum]) aLe 1.0
//            }
//
//            for (vertex in 0 until n) {
//                val inputEdgesX = Array<IloNumVar?>(graph.edgesOf(vertex).size) { null }
//                val inputEdgesY = Array<IloNumVar?>(graph.edgesOf(vertex).size) { null }
//
//                for ((i_1, to) in graph.edgesOf(vertex).withIndex()) {
//                    val num = to.second.toInt()
//                    val backNum = Graph.companionEdge(num)
//
//                    Graph.checkEdges(graph, num, backNum)
//                    Graph.checkDest(graph, backNum, vertex)
//
//                    inputEdgesX[i_1] = (v.x[backNum])
//                    inputEdgesY[i_1] = (v.y[backNum])
//                }
//
//                cplex.addEq(cplex.sum(inputEdgesX) + v.r[vertex], 1.0)
//                cplex.addEq(cplex.sum(inputEdgesY) + v.s[vertex], 1.0)
//            }
//
//            for (num in 0 until e) {
//                val edge = graph.edges[num]
//
//                ((v.q[edge.first] - v.q[edge.second]) + inf) aGe ((v.x[num] * inf) + STEP)
//                ((v.t[edge.first] - v.t[edge.second]) + inf) aGe ((v.y[num] * inf) + STEP)
//            }
//        }
//
//        addObjective()
//        addConstraint()
//        initClusters()
//        addConnectionConstraint()
//        cplex.use(
//            ICACallback(
//                v,
//                Constants(
//                    inf,
//                    matrix,
//                    graph,
//                    STEP,
//                    cplex
//                )
//            )
//        )
//    }
//
//    override fun solve(): Boolean {
//        return cplex.solve()
//    }
//
//    override fun icaDirection() = DoubleArray(d) { i -> cplex.getValue(v.a[i]) }
//
//    override fun writeResult() {
//        fun DoubleArray.printIt(title: String) {
//            print("$title: ")
//            forEach { print(" $it,") }.also { println() }
//        }
//
//        println("RESULT")
//        println(cplex.objValue)
//
//        for (i in 0 until d) {
//            println("a$i = ${cplex.getValue(v.a[i])}")
//        }
//        for (i in 0 until n) {
//            print("${cplex.getValue(v.f[i])} ")
//        }
//        println()
//        for (i in 0 until n) {
//            print("${cplex.getValue(v.g[i])} ")
//        }
//        println()
//
//        cplex.getValues(v.f).printIt("f")
//        cplex.getValues(v.g).printIt("g")
//        cplex.getValues(v.a).printIt("a")
//        cplex.getValues(v.alpha).printIt("Alpha")
//
//        v.clusters.forEach { i ->
//            println("CLUSTER")
//            println("fixedSign: " + cplex.getValue(i.fixedSign))
//            cplex.getValues(i.alphaSign).printIt("alphas")
//        }
////        cplex.getValues(v.fixedSign).printIt("FS   ")
//
//
//        println(cplex.status)
//        println(cplex.status == IloCplex.Status.Optimal)
//    }
//
//
//    // callback:
//    class RawSolution constructor(
//        val a: DoubleArray,
//        val f: DoubleArray,
//        val g: DoubleArray,
//        val alpha: DoubleArray,
////        val beta: DoubleArray,
//        val r: DoubleArray,
//        val q: DoubleArray,
//        val x: DoubleArray,
//        val s: DoubleArray,
//        val t: DoubleArray,
//        val y: DoubleArray
//    ) {
//        fun adapt(matrix: Matrix, graph: Graph, STEP: Double): Boolean {
//            val p = mul(matrix, a)
//            val l1norm = calcL1Norm(p)
//            if (l1norm < 0.1) {
//                return false
//            }
//            var cff = 1.0
//            if (Math.abs(l1norm - matrix.numRows()) > eps) {
//                cff = matrix.numRows() / l1norm
//            }
//            for (i in a.indices) {
//                a[i] *= cff
//            }
//            val new_p = mul(matrix, a)
//            for (i in new_p.indices) {
//                if (new_p[i] > 0) {
//                    f[i] = Math.abs(new_p[i])
//                    g[i] = 0.0
//                    alpha[i] = 1.0
//                } else {
//                    f[i] = 0.0
//                    g[i] = Math.abs(new_p[i])
//                    alpha[i] = 0.0
//                }
//            }
//            if (Math.abs(calcL1Norm(new_p) - matrix.numRows()) > eps) {
//                throw RuntimeException("unexpected l1norm after adapt")
//            }
//            System.arraycopy(f, 0, q, 0, f.size)
//            System.arraycopy(g, 0, t, 0, g.size)
//            for (i in graph.getEdges().indices) {
//                val edge: Pair<Int, Int> = graph.getEdges().get(i)
//                x[i] = x[i] + q[edge.first] + q[edge.second]
//            }
//            for (i in graph.getEdges().indices) {
//                val edge: Pair<Int, Int> = graph.getEdges().get(i)
//                y[i] = y[i] + t[edge.first] + t[edge.second]
//            }
//            MST.solve(graph, x, q, r, STEP)
//            MST.solve(graph, y, t, s, STEP)
//            return true
//        }
//
//        override fun toString(): String {
//            return """
//            RawSolution{
//            | l1norm = {calcL1Norm(mul(matrix, a))}
//            | obj = {calcObjective(this)}
//            | a = ${Arrays.toString(a)}
//            | f = ${Arrays.toString(f)}
//            | g = ${Arrays.toString(g)}
//            | alpha = ${Arrays.toString(alpha)}
//            | beta = {Arrays.toString(beta)}
//            | r = ${Arrays.toString(r)}
//            | q = ${Arrays.toString(q)}
//            | x = ${Arrays.toString(x)}
//            | s = ${Arrays.toString(s)}
//            | t = ${Arrays.toString(t)}
//            | y = ${Arrays.toString(y)}
//            }
//            """.trimIndent()
//        }
//
//        companion object {
//            private const val eps = 1e-6
//            private fun calcL1Norm(p: DoubleArray): Double {
//                var l1norm = 0.0
//                for (`val` in p) {
//                    l1norm += Math.abs(`val`)
//                }
//                return l1norm
//            }
//
//            private fun mul(matrix: Matrix, a: DoubleArray): DoubleArray {
//                return matrix.mult(Matrix(a).transpose()).transpose().getRow(0)
//            }
//        }
//    }
//
//    class Constants(
//        val inf: Double,
//        val matrix: Matrix,
//        val graph: Graph,
//        val step: Double,
//        var cplex: IloCplex
//    )
//
//    internal class ICACallback(val v: Variable, val constants: Constants) : IloCplex.HeuristicCallback() {
//
//
//        private fun calcObjective(sol: RawSolution): Double {
//            var sum = 0.0
//            val d = sol.a.size
//            val n = sol.f.size
//            for (i in 0 until d) {
//                sum += sol.a[i] * sol.a[i] * n * 10
//            }
//            for (i in 0 until n) {
//                val `val` = sol.f[i] - sol.q[i]
//                sum -= `val` * `val`
//            }
//            for (i in 0 until n) {
//                val `val` = sol.g[i] - sol.t[i]
//                sum -= `val` * `val`
//            }
//            return sum
//        }
//
//        override fun main() {
//            println("LOL")
//            val sol: RawSolution = RawSolution(
//                this.getValues(v.a),
//                this.getValues(v.f),
//                this.getValues(v.g),
//                this.getValues(v.alpha),
////                this.getValues(v.beta),
//                this.getValues(v.r),
//                this.getValues(v.q),
//                this.getValues(v.x),
//                this.getValues(v.s),
//                this.getValues(v.t),
//                this.getValues(v.y)
//            )
//            val oldStr = sol.toString()
//            if (sol.adapt(constants.matrix, constants.graph, constants.step)) {
//                val newStr = sol.toString()
//                val calcObj: Double = calcObjective(sol)
////                cnt_ans++
////                log.println(cnt_ans)
////                log.println("before: $oldStr")
////                log.println("after: $newStr")
////                log.println()
//                try {
//                    PrintWriter("./answers/q.txt").use { out_q ->
//                        for (i in sol.q.indices) {
//                            out_q.println(sol.q[i])
//                        }
//                    }
//                    PrintWriter("./answers/x.txt").use { out_x ->
//                        for (i in sol.x.indices) {
//                            out_x.println(sol.x[i])
//                        }
//                    }
//                    PrintWriter("./answers/t.txt").use { out_t ->
//                        for (i in sol.t.indices) {
//                            out_t.println(sol.t[i])
//                        }
//                    }
//                    PrintWriter("./answers/y.txt").use { out_y ->
//                        for (i in sol.y.indices) {
//                            out_y.println(sol.y[i])
//                        }
//                    }
//                    //DrawUtils.newDraw("./answers/", "tmp_ans" + cnt_ans++, legacy.graph);
//                } catch (e: Exception) {
//                    throw RuntimeException(e)
//                }
//                val vals = DoubleArray(v.allVars.size)
//                var ind_var = 0
//                for (z in sol.a) vals[ind_var++] = z
//                for (z in sol.f) vals[ind_var++] = z
//                for (z in sol.g) vals[ind_var++] = z
//                for (z in sol.alpha) vals[ind_var++] = z
////                for (z in sol.beta) vals[ind_var++] = z
//                for (z in sol.r) vals[ind_var++] = z
//                for (z in sol.q) vals[ind_var++] = z
//                for (z in sol.x) vals[ind_var++] = z
//                for (z in sol.s) vals[ind_var++] = z
//                for (z in sol.t) vals[ind_var++] = z
//                for (z in sol.y) vals[ind_var++] = z
//                println("IncOV: " + incumbentObjValue)
//
//                if (calcObj > incumbentObjValue) {
//                    System.out.println("found new solution: " + calcObj);
//                    setSolution(v.allVars, vals)
//                }
//            }
//        }
//    }
//
//}
