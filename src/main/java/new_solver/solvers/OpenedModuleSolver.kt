package new_solver.solvers

import ilog.concert.*
import ilog.cplex.*
import ilog.cplex.IloCplex.*
import legacy.utils.Matrix
import new_solver.common.toVarName
import new_solver.helpers.model.Constants
import new_solver.solvers.model.Logs

@Deprecated("Used to test module optimization. Right now is outdated")
class OpenedModuleSolver : Solver{

    var matrix: Matrix
    var d: Int
    var n: Int
    var inf: Double
    val cplex: IloCplex
    var v: Variable

    class Variable(
        val a: Array<IloNumVar>,
        val f: Array<IloNumVar>,
        val g: Array<IloNumVar>,
        val alpha: Array<IloNumVar>,
        val beta: Array<IloNumVar>
    )


    // Can we precompute how big may be inf?
    constructor(matrix: Matrix, tl: Int, inf: Double, mipStartA: DoubleArray?, mipStartP: DoubleArray?) {
        this.matrix = matrix
        this.n = matrix.numRows()
        this.d = matrix.numCols()
        this.inf = inf

        this.cplex = IloCplex()
        this.cplex.setParam(Param.OptimalityTarget, OptimalityTarget.OptimalGlobal)
        this.cplex.setParam(Param.TimeLimit, tl.toDouble())

        this.v = addVariable()

//        cplex.parameterSet.setParam(Param.Advance, 1)
//        cplex.parameterSet.setParam(IntParam.Reduce, 1)
//        mipStartA?.let { cplex.addMIPStart(v.a, it) }
//        mipStartP?.let { cplex.addMIPStart(v.p, it) }

        simpleOptimisation()
        val mipStart = Array<Pair<IloNumVar, Double>?>(
            v.a.size + v.f.size + v.g.size + v.alpha.size + v.beta.size
        ) { null }

        val ass = doubleArrayOf(-1.5541288234611779, -0.011868344420449262)


        var countr = 0
        for (i in 0 until v.a.size) {
            mipStart[countr] = (v.a[i] to ass[i])
            countr++
        }
        for (i in 0 until v.f.size) {
//            mipStart[countr] = (v.f[i] to fs[i])
            countr++
        }
        for (i in 0 until v.g.size) {
//            mipStart[countr] = (v.g[i] to gs[i])
            countr++
        }
        for (i in 0 until v.alpha.size) {
//            mipStart[countr] = (v.alpha[i] to alphas[i])
            countr++
        }
        for (i in 0 until v.beta.size) {
//            mipStart[countr] = (v.beta[i] to betas[i])
            countr++
        }

//        cplex.addMIPStart(mipStart.map { it!!.first }.toTypedArray(), mipStart.map { it!!.second }.toDoubleArray())
//        cplex.use(MyLocalCallback(v, logs), 64L)
//        cplex.use(MyLocalCallback(v, logs))
    }

    private fun addVariable(): Variable {
        return Variable(
            a = Array(d) { cplex.numVar(-inf, inf, IloNumVarType.Float, "a".toVarName(it)) },
            f = Array(n) { cplex.numVar(0.0, inf, IloNumVarType.Float, "f".toVarName(it)) },
            g = Array(n) { cplex.numVar(0.0, inf, IloNumVarType.Float, "g".toVarName(it)) },
            alpha = Array(n) { cplex.numVar(0.0, 1.0, IloNumVarType.Int, "alpha".toVarName(it)) },
            beta = Array(n) { cplex.numVar(0.0, 1.0, IloNumVarType.Int, "alpha".toVarName(it)) },
        )
    }

    private fun simpleOptimisation() {
        fun addObjective() {
            val squares = Array<IloNumExpr>(d) { i ->
                cplex.prod(v.a[i], v.a[i])
            }
            cplex.addMaximize(cplex.sum(squares))
        }

        fun addConstraint() {
            for (i in 0 until n) {
                cplex.addEq(
                    cplex.scalProd(matrix.getRow(i), v.a),
                    cplex.diff(v.f[i], v.g[i])
                )
            }
            val l1NormP = Array<IloNumExpr>(n) { i -> cplex.sum(v.f[i], v.g[i]) }
            cplex.addEq(cplex.sum(l1NormP), n.toDouble())

            for (i in 0 until n) {
                cplex.addLe(v.f[i], cplex.prod(v.alpha[i], inf))
                cplex.addLe(v.g[i], cplex.prod(v.beta[i], inf))
                cplex.addEq(cplex.sum(v.alpha[i], v.beta[i]), 1.0)
            }
        }

        addObjective()
        addConstraint()
    }

    override fun solve(): Boolean {
        return cplex.solve()
    }

    override fun icaDirection(): DoubleArray = DoubleArray(d) { i -> cplex.getValue(v.a[i]) }

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
        cplex.getValues(v.beta).printIt("Beta")
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