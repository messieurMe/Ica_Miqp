package new_solver.solvers

import ilog.concert.*
import ilog.cplex.*
import ilog.cplex.IloCplex.*
import legacy.utils.Matrix
import new_solver.helpers.model.Constants
import new_solver.solvers.model.Logs


@Deprecated("First iteration of solver. Right now is useless")
class SimpleSolver : Solver{

    var matrix: Matrix
    var d: Int
    var n: Int
    var inf: Double
    val cplex: IloCplex
    var v: Variable


    class Variable {
        var a: Array<IloNumVar>
        var p: Array<IloNumVar>

        constructor(a: Array<IloNumVar>, p: Array<IloNumVar>) {
            this.a = a
            this.p = p
        }
    }

    constructor(matrix: Matrix, tl: Int, inf: Double, mipStartA: DoubleArray?, mipStartP: DoubleArray?) {
        this.matrix = matrix
        this.n = matrix.numRows()
        this.d = matrix.numCols()
        this.inf = inf

        this.cplex = IloCplex()
        this.cplex.setParam(Param.OptimalityTarget, OptimalityTarget.OptimalGlobal)
        this.cplex.setParam(Param.TimeLimit, tl.toDouble())

        this.v = addVariable()

        simpleOptimisation()
        mipStartA?.let { cplex.addMIPStart(v.a, it) }
        mipStartP?.let { cplex.addMIPStart(v.p, it) }

        cplex.use(
            MyIncumbentCallback(v)
        )
    }

    private fun addVariable(): Variable {
        return Variable(
            a = Array(d) { i -> cplex.numVar(-inf, inf, IloNumVarType.Float, "a$i") },
            p = Array(n) { i -> cplex.numVar(-inf, inf, IloNumVarType.Float, "p$i") }
        )
    }

    private fun simpleOptimisation() {
        fun addObjective() {
            val squares = Array<IloNumExpr>(d) { i -> cplex.prod(v.a[i], v.a[i]) }
            cplex.addMaximize(cplex.sum(squares))
        }

        fun addConstraint() {
            for (i in 0 until n) {
                cplex.addEq(cplex.scalProd(matrix.getRow(i), v.a), v.p[i])
            }
            val l1NormP = Array<IloNumExpr>(n) { i -> cplex.abs(v.p[i]) }
            cplex.addEq(cplex.sum(l1NormP), n.toDouble())
        }

        addObjective()
        addConstraint()
    }

    override fun solve(): Boolean {
        return cplex.solve()
    }

    override fun icaDirection() = DoubleArray(d) { i -> cplex.getValue(v.a[i]) }

    override fun writeResult() {
        println("RESULT")
        println(cplex.objValue)
        for (i in 0 until d) {
            println("a$i = ${cplex.getValue(v.a[i])}")
        }
        for (i in 0 until n) {
            print("${cplex.getValue(v.p[i])} ")
        }
        println()
    }


    class MyIncumbentCallback(val vars: Variable) : UserCutCallback() {

        override fun main() {
//            println("=".repeat(5) + "<INCUMBENT CALLBACK>" + "=".repeat(5))
//            println("A: ${vars.a.map { "[${it.lb}, ${it.ub}], " }.toList()}")
//            println("=".repeat(5) + "</INCUMBENT CALLBACK>" + "=".repeat(5))
        }

        override fun getObjValue(): Double {
//            println("=".repeat(5) + "CALLBACK" + "=".repeat(5))
            return super.getObjValue()
        }

        override fun getIncumbentValue(p0: IloNumExpr?): Double {
//            println("=".repeat(5) + "CALLBACK" + "=".repeat(5))
            return super.getIncumbentValue(p0)
        }
    }
}