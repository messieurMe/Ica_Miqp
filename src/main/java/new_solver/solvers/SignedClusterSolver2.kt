package new_solver.solvers

import ilog.concert.*
import ilog.cplex.*
import ilog.cplex.IloCplex.*
import legacy.utils.Matrix
import ext.CplexKtExt
import new_solver.common.toVarName
import new_solver.solvers.model.Logs

@Deprecated("Used to test callbacks on simple data. Outdated")
class SignedClustersSolver2 : Solver{

    var matrix: Matrix
    var d: Int
    var n: Int
    var inf: Double
    val cplex: IloCplex
    val cplexKtExt: CplexKtExt
    var v: Variable
    val clusters: Array<IntArray>

    class Variable(
        val a: Array<IloNumVar>,
        val f: Array<IloNumVar>,
        val g: Array<IloNumVar>,
        val alpha: Array<IloNumVar>,
        val beta: Array<IloNumVar>,
        val fixedSign: Array<IloNumVar>,
//        val aSign: Array<IloNumVar>,
//        val aVars: Array<IloNumVar?>
    )

    constructor(matrix: Matrix, tl: Int, inf: Double, clusters: Array<IntArray>) {
        this.matrix = matrix
        this.n = matrix.numRows()
        this.d = matrix.numCols()
        this.inf = inf
        this.clusters = clusters

        this.cplex = IloCplex()
        this.cplex.setParam(Param.OptimalityTarget, OptimalityTarget.OptimalGlobal)
        this.cplex.setParam(Param.TimeLimit, tl.toDouble())
        this.cplexKtExt = CplexKtExt(cplex)

        this.v = addVariable()

        simpleOptimisation()
        cplex.addMIPStart(v.a, doubleArrayOf(0.0, 10.0))

        cplex.use(MyLocalCallback(v), 64L)
    }

//    val goodSigns = setOf(3, 27, 51, 54, 60, 87, 108, 120, 123, 129, 144)
//    val goodSignsVars = mutableListOf<Int>().also { x ->
//        goodSigns.forEach { i ->
//            for (j in i until i + ONE_SIGN_CLUSTER_SIZE) {
//                x.add(j)
//            }
//        }
//    }

    private fun addVariable(): Variable {
        return Variable(
            a = Array(d) { cplex.numVar(-inf, inf, IloNumVarType.Float, "a".toVarName(it)) },
            f = Array(n) { cplex.numVar(0.0, inf, IloNumVarType.Float, "f".toVarName(it)) },
            g = Array(n) { cplex.numVar(0.0, inf, IloNumVarType.Float, "g".toVarName(it)) },
            alpha = Array(n) { cplex.numVar(0.0, 1.0, IloNumVarType.Int, "alpha".toVarName(it)) },
            beta = Array(n) { cplex.numVar(0.0, 1.0, IloNumVarType.Int, "alpha".toVarName(it)) },
            fixedSign = Array(clusters.size) { i ->
                cplex.numVar(0.0, 1.0, IloNumVarType.Int, "fixedSign".toVarName(i))
            }.also {
                cplex.addEq(it[0], 0.0)
                cplex.addEq(it[1], 0.0)
                cplex.addEq(it[2], 0.0)
                cplex.addEq(it[3], 0.0)
//                cplex.addEq(it[4], 1.0)
//                cplex.addEq(it[5], 1.0)
            },
        )
    }

    private fun simpleOptimisation() {
        fun addObjective() {
            val squares = Array<IloNumExpr>(d) { i ->
                cplex.prod(v.a[i], v.a[i])
            }
            cplex.addMaximize(cplex.sum(squares))
        }

        fun addConstraint() = with(cplexKtExt) {
//            cplex.linearNumExpr().addTerm()

            for (i in 0 until n) {
                cplex.addEq(cplex.scalProd(matrix.getRow(i), v.a), v.f[i] - v.g[i])
            }
            val l1NormP = Array(n) { i -> (v.f[i] + v.g[i]) }
            cplex.addEq(cplex.sum(l1NormP), n.toDouble())

            for (i in 0 until n) {
                cplex.addLe(v.f[i], v.alpha[i] * inf)
                cplex.addLe(v.g[i], v.beta[i] * inf)
                cplex.addEq(v.alpha[i] + v.beta[i], 1.0)
            }

            val clusterSign = listOf<IloNumVar>()
            val clusterCenters = listOf<DoubleArray>()
            val centerF = listOf<IloNumVar>()
            val centerG = listOf<IloNumVar>()
            val centerAlpha = listOf<IloNumVar>()
            val centerBeta = listOf<IloNumVar>()
            clusterCenters.forEachIndexed { i: Int, clusterCenter ->

                // У нас есть центр кластера и мы считаем для него знак.
                // Я предполагаю, что знак центра кластера должен быть равен знаку кластера?
                cplex.addEq(cplex.scalProd(clusterCenter, v.a), centerF[i] - centerG[i])
                val l1Norm = centerF[i] + centerG[i]
                cplex.addEq(l1Norm, 1.0)

                centerF[i] aLe (centerAlpha[i] * inf)
                centerG[i] aLe (centerBeta[i] * inf)
                (centerAlpha[i] + centerBeta[i]) aEq 1.0

                // TODO Дописать проверку
            }

            clusters.forEachIndexed { clusterI, cluster ->
                val alphaOfGroup = v.alpha[cluster[0]]
                val betaOfGroup = v.beta[cluster[0]]
                cluster.forEach { i ->
                    cplex.addLe(v.alpha[i] - alphaOfGroup, 1.0 - v.fixedSign[clusterI])
                    cplex.addGe(v.alpha[i] - alphaOfGroup, v.fixedSign[clusterI] - 1.0)

                    // actually useless
                    cplex.addLe(v.beta[i] - betaOfGroup, 1.0 - v.fixedSign[clusterI])
                    cplex.addGe(v.beta[i] - betaOfGroup, v.fixedSign[clusterI] - 1.0)
                }
            }

//            val clusterCenter = listOf<List<Double>>()
//            clusterCenter.forEach {
//            }


        }

        addObjective()
        addConstraint()
    }

    override fun solve(): Boolean {
        return cplex.solve()
    }

    override fun icaDirection() = DoubleArray(d) { i -> cplex.getValue(v.a[i]) }

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
        cplex.getValues(v.beta).printIt("Beta ")
        cplex.getValues(v.fixedSign).printIt("FS   ")
        println(cplex.status)
    }


    class MyLocalCallback(val vars: Variable) : Callback.Function {

        var counter = 0

        override fun invoke(context: Callback.Context) {
            counter++
            context.getGlobalUB(vars.f)
            context.getLocalUB(vars.f)
            context.getLocalLB(vars.f)
            context.getGlobalLB(vars.f)

            context.getGlobalUB(vars.g)
            context.getLocalUB(vars.g)
            context.getLocalLB(vars.g)
            context.getGlobalLB(vars.g)

            try {
//                runCatching {
//                    val point = context.getIncumbent(vars.a)
//                    logs.a.add(point[0] to point[1])
//                }.onFailure { logs.a.add(null to null) }
//                println(context.getIncumbent(vars.a).toList())
            } catch (e: Exception) {
//                println("\tFailed")
            }
        }

//        override fun new_solver.helpers.linear.main() {
//            val `as` = this.getIncumbentValues(vars.a)
//            logs.a.add((`as`[0] to `as`[1]))
//            logs.a.add((vars.a[0] to vars.a[1]))
//        }

//        override fun new_solver.helpers.linear.main() {
//
//        }
    }
}

