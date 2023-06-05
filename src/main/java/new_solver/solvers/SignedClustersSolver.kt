package new_solver.solvers

import ilog.concert.*
import ilog.cplex.*
import ilog.cplex.IloCplex.*
import legacy.utils.Matrix
import ext.CplexKtExt
import new_solver.common.toVarName
import new_solver.solvers.model.Logs
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt


class SignedClustersSolver : Solver{

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
    )

    constructor(matrix: Matrix, tl: Int, inf: Double, clusters: List<IntArray>) {
        this.matrix = matrix
        this.n = matrix.numRows()
        this.d = matrix.numCols()
        this.inf = inf
        this.clusters = clusters.toTypedArray()

        this.cplex = IloCplex()
        this.cplex.setParam(Param.OptimalityTarget, OptimalityTarget.OptimalGlobal)
        this.cplex.setParam(Param.TimeLimit, tl.toDouble())
        this.cplexKtExt = CplexKtExt(cplex)

        this.v = addVariable()

//        cplex.setParam(IloCplex.Param.MIP.Strategy.VariableSelect, 3)
        findClusterEdgePointsV2(clusters)
        simpleOptimisation()
    }

    // cluster edge points
    lateinit var clusterEP: Array<Pair<Int, Int>>

    // Находим такие две точки, что у первой угол до оси ОХ минимален, а у второй максимален
    private fun findClusterEdgePoints(clusters: Array<IntArray>) {
        clusterEP = Array<Pair<Int, Int>>(clusters.size) { i ->
            val cluster = clusters[i]
            val angles = cluster.map { matrix.getRow(it) }.map { (it[0] to it[1]) }.map { (x, y) ->
                val len = (x.pow(2) + y.pow(2))
                (x / len to y / len)
            }.map { (x, y) -> Math.atan2(y, x) }.map { angle ->
                if (angle < 0.0) {
                    2 * Math.PI + angle
                } else angle
            }

            val min = cluster[angles.indexOf(angles.min())]
            val max = cluster[angles.indexOf(angles.max())]

            (min to max)
        }
    }

    // Находим такие две точки, что угол между ними максимален
    private fun findClusterEdgePointsV2(clusters: List<IntArray>) {
        clusterEP = Array(clusters.size) {
            val cluster = clusters[it]
            val allAngles = Array(cluster.size) { i ->
                var biggestAngle = -0.1
                for (j in cluster.indices) if (j != i) {
                    val a = matrix.getRow(cluster[i])
                    val b = matrix.getRow(cluster[j])
                    val p = a[0] * b[0] + a[1] * b[1]
                    fun length(x: DoubleArray) = sqrt(x[0].pow(2) + x[1].pow(2))
                    val r = acos(p / (length(a) * length(b)))
                    if (r > biggestAngle) {
                        biggestAngle = r
                    }
                }
                biggestAngle
            }

            val maxAngles = allAngles
                .mapIndexed { i, v -> i to v }
                .sortedBy { (i, v) -> v }

            println((maxAngles[maxAngles.size - 1].first to maxAngles[maxAngles.size - 2].first))
            (maxAngles[maxAngles.size - 1].first to maxAngles[maxAngles.size - 2].first)
        }
    }

    private fun addVariable(): Variable {
        return Variable(
            a = Array(d) { cplex.numVar(-inf, inf, IloNumVarType.Float, "a".toVarName(it)) },
            f = Array(n) { cplex.numVar(0.0, inf, IloNumVarType.Float, "f".toVarName(it)) },
            g = Array(n) { cplex.numVar(0.0, inf, IloNumVarType.Float, "g".toVarName(it)) },
            alpha = Array(n) { cplex.numVar(0.0, 1.0, IloNumVarType.Int, "alpha".toVarName(it)) },
            beta = Array(n) { cplex.numVar(0.0, 1.0, IloNumVarType.Int, "alpha".toVarName(it)) },
            fixedSign = Array(clusters.size) { i ->
                cplex.numVar(0.0, 1.0, IloNumVarType.Int, "fixedSign".toVarName(i))
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

            // constraints on elements in cluster
            clusters.forEachIndexed { cI, cluster ->
                ((v.alpha[clusterEP[cI].first] + v.alpha[clusterEP[cI].second]) - 1.0) aLe v.fixedSign[cI]
//                ((1.0 - v.alpha[clusterEP[cI].first] - v.alpha[clusterEP[cI].second]) ) aLe v.fixedSign[cI]
                ((v.beta[clusterEP[cI].first] + v.beta[clusterEP[cI].second]) - 1.0) aLe v.fixedSign[cI]

                val alphaOfGroup = v.alpha[clusterEP[cI].first]

                val oneExtrFixSign = 1.0 - v.fixedSign[cI]
                val fixSignExtrOne = v.fixedSign[cI] - 1.0

                val betaOfGroup = 1.0 - v.beta[clusterEP[cI].first]
                cluster.forEach { i ->
                    cplex.addLe(v.alpha[i] - alphaOfGroup, oneExtrFixSign)
                    cplex.addGe(v.alpha[i] - alphaOfGroup, fixSignExtrOne)

                    // actually useless
//                    cplex.addLe(v.beta[i] - betaOfGroup, 1.0 - v.fixedSign[cI])
//                    cplex.addGe(v.beta[i] - betaOfGroup, v.fixedSign[cI] - 1.0)
                }


            }

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
//        cplex.getValues(v.beta).printIt("Beta ")
        cplex.getValues(v.fixedSign).printIt("FS   ")

        println(cplex.status)
        println(cplex.status == Status.Optimal)
    }


    class MyLocalCallback(val vars: Variable, val logs: Logs) : Callback.Function {

        var counter = 0

        override fun invoke(context: Callback.Context) {
            counter++
            logs.callbackId.addLast(context.id)

            context.getGlobalUB(vars.f)
            context.getLocalUB(vars.f)
            context.getLocalLB(vars.f)
            context.getGlobalLB(vars.f)

            context.getGlobalUB(vars.g)
            context.getLocalUB(vars.g)
            context.getLocalLB(vars.g)
            context.getGlobalLB(vars.g)

            try {
                if (counter in 200..700 && counter % 5 == 0) {
                    logs.alpha.add(context.getRelaxationPoint(vars.f))
                }
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

