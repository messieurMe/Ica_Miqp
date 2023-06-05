//package new_solver.solvers
//
//import ilog.concert.IloNumExpr
//import ilog.concert.IloNumVar
//import ilog.concert.IloNumVarType
//import ilog.cplex.IloCplex
//import ilog.cplex.IloCplex.*
//import ilog.cplex.IloCplex.Callback.*
//import ilog.cplex.IloCplex.Callback.Context.Id.*
//import legacy.graph.Graph
//import legacy.utils.Matrix
//import ext.CplexKtExt
//import new_solver.common.toVarName
//import new_solver.helpers.angleDeg
//import new_solver.helpers.clustering.direction_to_cluster_mapper.DirectionToClusterMapperWithCircle
//import new_solver.solvers.model.Logs
//import java.lang.Exception
//import java.util.LinkedList
//
//
//@Suppress(
//    "InconsistentCommentForJavaParameter",
//    "ConvertSecondaryConstructorToPrimary"
//)
//class NDimClusterSolver : Solver {
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
//    lateinit var graph: Graph
//    val E: Int = -1
//    val STEP: Double = 1.0
//
//    override val logs = Logs()
//
//
//    class Constants(
//        val inf: Double,
//        val matrix: Matrix,
//        var cplex: IloCplex
//    )
//
//    class Variable(
//        val a: Array<IloNumVar>, val f: Array<IloNumVar>, val g: Array<IloNumVar>, val alpha: Array<IloNumVar>,
//
//        val clusters: ArrayList<ClusterVars> = ArrayList()
//    ) {
//        class ClusterVars(
//            val fixedSign: IloNumVar,
//            val fSign: Array<IloNumVar>,
//            val gSign: Array<IloNumVar>,
//            val alphaSign: Array<IloNumVar>,
//        )
//    }
//
//    constructor(matrix: Matrix, tl: Int, inf: Double, clusters: Array<DoubleArray>) {
//        this.matrix = matrix
//        this.n = matrix.numRows()
//        this.d = matrix.numCols()
//        this.inf = inf
//        this.clusters = clusters
//        this.cplex = IloCplex()
//
//        this.v = addVariable()
//        this.cplexKtExt = CplexKtExt(cplex)
//
//        this.cplex.setParam(Param.OptimalityTarget, OptimalityTarget.OptimalGlobal)
//        this.cplex.setParam(Param.TimeLimit, tl.toDouble())
////        this.cplex.setParam(Param.NodeAlgorithm,  NodeSelect.BestEst)
//
//        cplex.use(
//            /* callback = */ MyLocalCallback(
//                v,
//                logs,
//                Constants(inf, matrix, cplex)
//            ),
//            /* idMask = */ 63
//        )
//
//        simpleOptimisation()
//    }
//
//    private fun addVariable(): Variable {
//        return Variable(
//            a = Array(d) { cplex.numVar(-inf, inf, IloNumVarType.Float, "a".toVarName(it)) },
//            f = Array(n) { cplex.numVar(0.0, inf, IloNumVarType.Float, "f".toVarName(it)) },
//            g = Array(n) { cplex.numVar(0.0, inf, IloNumVarType.Float, "g".toVarName(it)) },
////            alpha = Array(n) { cplex.numVar(0.0, 1.0, IloNumVarType.Int, "alpha".toVarName(it)) },
//            alpha = Array(n) { cplex.boolVar("alpha".toVarName(it)) },
//
//            )
//    }
//
//    private fun simpleOptimisation() {
//        fun addObjective() {
//            val pow2 = cplex.quadNumExpr()
//            val squares = Array<IloNumExpr>(d) { i ->
//                pow2.addTerm(1.0, v.a[i], v.a[i])
//                cplex.prod(v.a[i], v.a[i])
//            }
//            cplex.addMaximize(pow2)
////            cplex.addMaximize(cplex.sum(squares))
//        }
//
//        fun addConstraint() = with(cplexKtExt) {
////            cplex.linearNumExpr().addTerm()
//
//            for (i in 0 until n) {
//                cplex.addEq(cplex.scalProd(matrix.getRow(i), v.a), v.f[i] - v.g[i])
//            }
//            val l1NormP = Array(n) { i -> (v.f[i] + v.g[i]) }
//            cplex.addEq(cplex.sum(l1NormP), n.toDouble())
////            cplex.addEq(cplex.sum(v.f) + cplex.sum(v.g), n.toDouble())
//
//            for (i in 0 until n) {
//                cplex.addLe(v.f[i], v.alpha[i] * inf)
//                cplex.addLe(v.g[i], (1.0 - v.alpha[i]) * inf)
////                cplex.addEq(v.alpha[i] + v.beta[i], 1.0)
//            }
//        }
//
//        fun initClusters() = with(cplexKtExt) {
//            val clusters = DirectionToClusterMapperWithCircle(
//                clusters,
//                matrix,
//            )
//
////            println("\tdots = np.array([")
////            for (i in 0 until n) {
////                println("\t\t${matrix.getRow(i).toList()},")
////            }
////            println("\t])")
////            println("\tplt.scatter(dots[:, 0], dots[:, 1], color='b')")
//
////            clusters.forEach { cluster ->
////
////                println("\tdots = np.array([")
////                for (i in cluster.points) {
////                    println("\t\t${matrix.getRow(i).toList()},")
////                }
////                println("\t])")
////                println("\tplt.scatter(dots[:, 0], dots[:, 1], color='r')")
////                println("\t# Endpoints")
////                cluster.endpoints.forEach { println("\tplt.arrow(0.0, 0.0, ${it[0]}, ${it[1]})") }
////                println("\t# Points")
////            }
//
//            println("CLUSTERING")
//            println("With average: " + clusters.map { it.points.size }.average())
//            for (i in clusters.indices) {
//                val cluster = clusters[i]
//
//                if (cluster.points.size < clusteringMinClusterSize) {
//                    continue
//                } else {
//                    println("CLUSTER CREATED WITH ${cluster.points.size} POINTS")
//                }
//
//                val cv = Variable.ClusterVars(fixedSign = cplex.numVar(
//                    0.0, 1.0, IloNumVarType.Int, "fixedSign".toVarName(i)
//                ), fSign = Array(cluster.endpoints.size) {
//                    cplex.numVar(0.0, inf, IloNumVarType.Float, "fSign".toVarName(i * 100 + it))
//                }, gSign = Array(cluster.endpoints.size) {
//                    cplex.numVar(0.0, inf, IloNumVarType.Float, "gSign".toVarName(i * 100 + it))
//                }, alphaSign = Array(cluster.endpoints.size) {
//                    cplex.numVar(0.0, 1.0, IloNumVarType.Int, "alphaSign".toVarName(i * 100 + it))
//                }).also(v.clusters::add)
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
//
//
////            for (iIndex in clusters.indices) {
////                val cluster = clusters[iIndex]
////                if (
////                    cluster.points.isEmpty() || cluster.points.size < 50
////                ) {
////                    continue
////                }
////                println("CLUSTER ADDED WITH ${cluster.points.size} points")
////                cluster.endpoints.forEachIndexed { jIndex, ep ->
////                    val varIndex = iIndex * d + jIndex
////                    cplex.addEq(cplex.scalProd(ep, v.a), v.fSign[varIndex] - v.gSign[varIndex])
////
////                    v.fSign[varIndex] aLe (v.alphaSign[varIndex] * inf)
////                    v.gSign[varIndex] aLe ((1.0 - v.alphaSign[varIndex]) * inf)
////                }
////
////                val clusterPoint = cluster.points.first()
////                val alphaOfGroup = v.alpha[clusterPoint]
////                val betaOfGroup = 1.0 - v.alpha[clusterPoint]
////
////                cluster.points.forEach { i ->
////                    val alphaSubtractGroupAlpha = (v.alpha[i] - alphaOfGroup)
////                    (alphaSubtractGroupAlpha) aLe (1.0 - v.fixedSign[iIndex])
////                    (alphaSubtractGroupAlpha) aLe (v.fixedSign[iIndex] - 1.0)
////
////                     actually useless
////                    val betaSubtractGroupBeta = ((1.0 - v.alpha[i]) - betaOfGroup)
////                    betaSubtractGroupBeta aLe (1.0 - v.fixedSign[iIndex])
////                    betaSubtractGroupBeta aLe (v.fixedSign[iIndex] - 1.0)
////                }
//
////                val clusterEndPointsAlpha = Array(d) { i -> v.alphaSign[d * iIndex + i] }
////                val sumOfAlphas = cplex.sum(clusterEndPointsAlpha)
////                (sumOfAlphas - (d - 1.0)) aLe (v.fixedSign[iIndex])
////
////                val clusterEndPointsBeta = clusterEndPointsAlpha.map { 1.0 - it }.toTypedArray()
////                ((cplex.sum(clusterEndPointsBeta)) - (d - 1.0)) aLe (v.fixedSign[iIndex])
////                ((-1.0 * sumOfAlphas) + 1.0) aLe (v.fixedSign[iIndex])
////            }
//        }
//
////        fun addConstraintOnClusterSigns() = with(cplexKtExt) {
////            for (i in allStraightVectors.indices) {
////                v.fSign[i] aLe (v.alphaSign[i] * inf)
////                v.gSign[i] aLe ((1.0 - v.alphaSign[i]) * inf)
//////                cplex.addEq(v.alphaSign[i] + v.betaSign[i], 1.0)
////            }
////
////            // constraints on elements in cluster
////            clusters.forEachIndexed { cI, cluster ->
////                try {
//////                    val alphaOfGroup = v.alpha[cluster[0]]
//////                    val betaOfGroup = (1.0 - v.alpha[cluster[0]])
////                    cluster.forEach { i ->
//////                        cplex.addLe(v.alpha[i] - alphaOfGroup, 1.0 - v.fixedSign[cI])
//////                        cplex.addGe(v.alpha[i] - alphaOfGroup, v.fixedSign[cI] - 1.0)
////
////                        // actually useless
//////                        cplex.addLe((1.0 - v.alpha[i]) - betaOfGroup, 1.0 - v.fixedSign[cI])
//////                        cplex.addGe((1.0 - v.alpha[i]) - betaOfGroup, v.fixedSign[cI] - 1.0)
////                    }
////
////                    val xs = clusterEPSigns[cI].mapIndexed { index, v -> (index * 2) + ((v + 1) / 2) }
////                    // считаем сумму знаков для всех опорных векторов
////                    val alphaEndPointsSignsSum = xs.map { v.alphaSign[it] }.toTypedArray()
////                    (cplex.sum(alphaEndPointsSignsSum) - (d - 1).toDouble()) aLe v.fixedSign[cI]
////
////                    val betaEndPointsSignSum = xs.map { (1.0 - v.alphaSign[it]) }.toTypedArray()
////                    (cplex.sum(betaEndPointsSignSum) - (d - 1).toDouble()) aLe v.fixedSign[cI]
////                } catch (e: IndexOutOfBoundsException) {
////                    // empty
////                }
////            }
////        }
//
//
//        addObjective()
//        addConstraint()
//        initClusters()
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
//        println(cplex.status == Status.Optimal)
//    }
//
//    class MyLocalCallback(
//        val vars: Variable, val logs: Logs, val constants: Constants
//    ) : Callback.Function
////        : IncumbentCallback()
//    {
//
//        val addedDirections = LinkedList<List<Double>>()
//
//        //        override fun new_solver.helpers.linear.main() {
//
//        override fun invoke(context: Context) {
////            logs.callbackId.addLast(context.id)
////            println("LOL")
//            constants.cplex = context.cplex
//            try {
////                addCluster(context)
//                when {
//                    0L != context.id and LocalProgress -> {
////                    addCluster(context)
//                    }
//
//                    0L != context.id and Branching -> {
////                    addCluster(context)
//                    }
//
//                    0L != context.id and Candidate -> {
//                        addCluster(context)
//                    }
//
//                    0L != context.id and Relaxation -> {
////                    addCluster(context)
//                    }
//
//                    0L != context.id and GlobalProgress -> {
////                        addCluster(context)
//                    }
//                }
//            } catch (e: Exception) {
//
//            }
//        }
//
//
//        private fun addCluster(
//            context: Context
//        ) {
////            Logger.withLogger("Callback.addCluster", skipIfEmpty = true) {
////            try {
//            val direction: List<Double> = context.getIncumbent(vars.a).toList()
//                .also {
////                    d("we have solution", it.toString())
//                }
//
//            println("LIL")
////            d("direction", direction.toString())
//
//            if (addedDirections.all { it angleDeg direction >= callbackMinAngle }) {
//                with(CplexKtExt(constants.cplex)) {
//                    val newClusters = DirectionToClusterMapperWithCircle(
//                        arrayOf(direction.toDoubleArray()), constants.matrix,
//                    )
////                    d("number of points in cluster", newClusters.map { it.points.size }.toString())
//
//                    newClusters.forEachIndexed { i, cluster ->
//
//                        val cv = Variable.ClusterVars(
//                            fixedSign = cplex.numVar(
//                                0.0, 1.0, IloNumVarType.Int, "fixedSign".toVarName(i)
//                            ),
//                            fSign = Array(cluster.endpoints.size) {
//                                cplex.numVar(
//                                    0.0,
//                                    constants.inf,
//                                    IloNumVarType.Float,
//                                    "fSign".toVarName(i * 100 + it)
//                                )
//                            },
//                            gSign = Array(cluster.endpoints.size) {
//                                cplex.numVar(
//                                    0.0,
//                                    constants.inf,
//                                    IloNumVarType.Float,
//                                    "gSign".toVarName(i * 100 + it)
//                                )
//                            },
//                            alphaSign = Array(cluster.endpoints.size) {
//                                cplex.numVar(
//                                    0.0,
//                                    1.0,
//                                    IloNumVarType.Int,
//                                    "alphaSign".toVarName(i * 100 + it)
//                                )
//                            })//.also(v.clusters::add)
//
//                        cluster.endpoints.forEachIndexed { endpointI, endpoint ->
//                            cplex.addEq(
//                                cplex.scalProd(endpoint, vars.a),
//                                cv.fSign[endpointI] - cv.gSign[endpointI]
//                            )
//                            cv.fSign[endpointI] aLe (cv.alphaSign[endpointI] * constants.inf)
//                            cv.gSign[endpointI] aLe ((1.0 - cv.alphaSign[endpointI]) * constants.inf)
//                        }
//
//                        val clusterPoint = cluster.points.first()
//                        val alphaOfGroup = vars.alpha[clusterPoint]
//                        val betaOfGroup = 1.0 - vars.alpha[clusterPoint]
//
//                        cluster.points.forEach { pointIndex ->
//                            val alphaSubtractGroup = (vars.alpha[pointIndex] - alphaOfGroup)
//                            (alphaSubtractGroup) aLe (1.0 - cv.fixedSign)
//                            (alphaSubtractGroup) aLe (cv.fixedSign - 1.0)
//
//                            val betaSubtractGroup = ((1.0 - vars.alpha[pointIndex]) - betaOfGroup)
//                            (betaSubtractGroup) aLe (1.0 - cv.fixedSign)
//                            (betaSubtractGroup) aLe (cv.fixedSign - 1.0)
//                        }
//                        val clusterD = cluster.endpoints.size
//
//                        val alphaSums = cplex.sum(cv.alphaSign)
//                        (alphaSums - (clusterD - 1.0)) aLe (cv.fixedSign)
//
//                        val betaSums = cplex.sum(cv.alphaSign.map { 1.0 - it }.toTypedArray())
//                        (betaSums - (clusterD - 1.0)) aLe cv.fixedSign
//                    }
//
////                }
//                }
//            }
////            } catch (e: Throwable) {
////                d("exception", e.message ?: "OutOfMessage")
////            }
//        }
//
//    }
////        override fun new_solver.helpers.linear.main() {
////            val `as` = this.getIncumbentValues(vars.a)
////            logs.a.add((`as`[0] to `as`[1]))
////            logs.a.add((vars.a[0] to vars.a[1]))
////        }
//
////        override fun new_solver.helpers.linear.main() {
////
////        }
//}
//
