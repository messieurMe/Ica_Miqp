package new_solver.helpers.model

import ilog.concert.IloNumVar
import ilog.concert.IloNumVarType
import ilog.cplex.IloCplex
import config.Consts
import new_solver.common.toVarName

interface BasicVariables<T> : Variables<T> {
    val a: Array<T>
    val f: Array<T>
    val g: Array<T>
    val alpha: Array<T>

    override fun allVariables(): List<T> {
        return listOf(*a, *f, *g, *alpha)
    }
}

class ActualBasicVariables(
    override val a: Array<Double>,
    override val f: Array<Double>,
    override val g: Array<Double>,
    override val alpha: Array<Double>
) : BasicVariables<Double>

fun IloBasicVariables(n: Int, d: Int, cplex: IloCplex) = object : BasicVariables<IloNumVar> {
    override val a = Array(d) { cplex.numVar(-Consts.INF, Consts.INF, IloNumVarType.Float, "a".toVarName(it)) }
    override val f = Array(n) { cplex.numVar(0.0, Consts.INF, IloNumVarType.Float, "f".toVarName(it)) }
    override val g = Array(n) { cplex.numVar(0.0, Consts.INF, IloNumVarType.Float, "g".toVarName(it)) }
    override val alpha = Array(n) { cplex.numVar(0.0, 1.0, IloNumVarType.Int, "alpha".toVarName(it)) }
}
