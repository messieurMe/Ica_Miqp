package new_solver.solvers

import ilog.concert.*
import ilog.cplex.*
import ilog.cplex.IloCplex.*
import legacy.utils.Matrix
import ext.CplexKtExt
import new_solver.common.toVarName
import new_solver.solvers.model.Logs
import java.lang.IndexOutOfBoundsException
import java.util.LinkedList
import kotlin.math.pow
import kotlin.math.sign

@Deprecated("Used to explore clusters constraints. Now is outdated")
class CustomClusterSolver : Solver{

    var matrix: Matrix
    var d: Int
    var n: Int
    var inf: Double
    val cplex: IloCplex
    val cplexKtExt: CplexKtExt
    var v: Variable

    class Variable(
        val a: Array<IloNumVar>,
        val f: Array<IloNumVar>,
        val g: Array<IloNumVar>,
        val alpha: Array<IloNumVar>,
//        val beta: Array<IloNumVar>,
        val fixedSign: Array<IloNumVar>,

        val fSign: Array<IloNumVar>,
        val gSign: Array<IloNumVar>,
        val alphaSign: Array<IloNumVar>,
//        val betaSign: Array<IloNumVar>,
    )

    constructor(matrix: Matrix, tl: Int, inf: Double) {
        this.matrix = matrix
        this.n = matrix.numRows()
        this.d = matrix.numCols()
        this.inf = inf

        this.cplex = IloCplex()
        this.cplex.setParam(Param.OptimalityTarget, OptimalityTarget.OptimalGlobal)
        this.cplex.setParam(Param.TimeLimit, tl.toDouble())
        this.cplexKtExt = CplexKtExt(cplex)

        this.v = addVariable()


//        findClusterEdgePointsV2(matrix)
        simpleOptimisation()
    }


    // cluster edge points
    var clusterEPSigns: Array<IntArray> = emptyArray()
    var clusters: Array<LinkedList<Int>> = emptyArray()
    var allStraightVectors: Array<IntArray> = emptyArray()


    // Находим такие две точки, что угол между ними максимален
    private fun findClusterEdgePointsV2(matrix: Matrix) {
        if (matrix.numCols() >= 63) {
            throw Exception("Dimension is too big")
        }

        allStraightVectors = Array(2 * d) { i ->
            IntArray(d) { j -> if (j == (i / 2)) (i % 2) * 2 - 1 else 0 }
        }


        // Всевозможные битовые вектора, т.е все блоки пространства
        clusterEPSigns = Array(2.0.pow(d).toInt()) { i ->
            var x = i
            val array = IntArray(d)
            for (j in d - 1 downTo 0) {
                array[j] = ((x and 1) * 2 - 1)
                x = x shr 1
            }
            array.also { println(it.toList()) }
        }

        clusters = Array(2.0.pow(d).toInt()) { LinkedList<Int>() }

        val check = Array(4) { HashSet<DoubleArray>() }

        for (i in 0 until n) {
            val row = this.matrix.getRow(i)

            var mask = 0
            for (j in 0 until d) {
                mask = mask.shl(1)
                mask = mask or (row[j].sign.toInt() + 1) / 2
            }
            clusters[mask].addLast(i)
//            check[mask].add(matrix.getRow(i))
        }
//        check

    }


    private fun addVariable(): Variable {
        return Variable(
            a = Array(d) { cplex.numVar(-inf, inf, IloNumVarType.Float, "a".toVarName(it)) },
            f = Array(n) { cplex.numVar(0.0, inf, IloNumVarType.Float, "f".toVarName(it)) },
            g = Array(n) { cplex.numVar(0.0, inf, IloNumVarType.Float, "g".toVarName(it)) },
            alpha = Array(n) { cplex.numVar(0.0, 1.0, IloNumVarType.Int, "alpha".toVarName(it)) },
//            beta = Array(n) { cplex.numVar(0.0, 1.0, IloNumVarType.Int, "alpha".toVarName(it)) },
            fixedSign = Array(2.0.pow(d).toInt()) { i ->
                cplex.numVar(
                    0.0, 1.0, IloNumVarType.Int, "fixedSign".toVarName(i)
                )
            },
            fSign = Array(2 * d) { cplex.numVar(0.0, inf, IloNumVarType.Float, "fSign".toVarName(it)) },
            gSign = Array(2 * d) { cplex.numVar(0.0, inf, IloNumVarType.Float, "gSign".toVarName(it)) },
            alphaSign = Array(2 * d) {
                cplex.numVar(
                    0.0,
                    1.0,
                    IloNumVarType.Int,
                    "alphaSign".toVarName(it)
                )
            },
//            betaSign = Array(2 * d) { cplex.numVar(0.0, 1.0, IloNumVarType.Int, "betaSign".toVarName(it)) }
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
                cplex.addLe(v.g[i], (1.0 - v.alpha[i]) * inf)
//                cplex.addEq(v.alpha[i] + v.beta[i], 1.0)
            }
        }

        fun addConstraintOnClusterSigns() = with(cplexKtExt) {


            for (i in allStraightVectors.indices) {
                cplex.addEq(cplex.scalProd(allStraightVectors[i], v.a), v.fSign[i] - v.gSign[i])
            }

            for (i in allStraightVectors.indices) {
                v.fSign[i] aLe (v.alphaSign[i] * inf)
                v.gSign[i] aLe ((1.0 - v.alphaSign[i]) * inf)
//                cplex.addEq(v.alphaSign[i] + v.betaSign[i], 1.0)
            }


            // constraints on elements in cluster
            clusters.forEachIndexed { cI, cluster ->
                try {
                    val alphaOfGroup = v.alpha[cluster[0]]
                    val betaOfGroup = (1.0 - v.alpha[cluster[0]])
                    cluster.forEach { i ->
                        cplex.addLe(v.alpha[i] - alphaOfGroup, 1.0 - v.fixedSign[cI])
                        cplex.addGe(v.alpha[i] - alphaOfGroup, v.fixedSign[cI] - 1.0)

                        // actually useless
                        cplex.addLe((1.0 - v.alpha[i]) - betaOfGroup, 1.0 - v.fixedSign[cI])
                        cplex.addGe((1.0 - v.alpha[i]) - betaOfGroup, v.fixedSign[cI] - 1.0)
                    }

                    val xs = clusterEPSigns[cI].mapIndexed { index, v -> (index * 2) + ((v + 1) / 2) }
                    // считаем сумму знаков для всех опорных векторов
                    val alphaEndPointsSignsSum = xs.map { v.alphaSign[it] }.toTypedArray()
                    (cplex.sum(alphaEndPointsSignsSum) - (d - 1).toDouble()) aLe v.fixedSign[cI]

                    val betaEndPointsSignSum = xs.map { (1.0 - v.alphaSign[it]) }.toTypedArray()
                    (cplex.sum(betaEndPointsSignSum) - (d - 1).toDouble()) aLe v.fixedSign[cI]
                } catch (e: IndexOutOfBoundsException) {
                    // empty
                }
            }
        }

        addObjective()
        addConstraint()
        addConstraintOnClusterSigns()
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

