package new_solver.helpers.model

import ilog.concert.IloNumVar
import ilog.concert.IloNumVarType
import ilog.cplex.IloCplex
import config.Consts
import new_solver.common.toVarName

interface ConnectionVariables<T> : Variables<T> {
    val r: Array<T>
    val q: Array<T>
    val s: Array<T>
    val t: Array<T>
    val x: Array<T>
    val y: Array<T>

    override fun allVariables(): List<T> {
        return listOf(*r, *q, *s, *t, *x, *y)
    }
}

class ActualConnectionVariables(
    override val r: Array<Double>,
    override val q: Array<Double>,
    override val s: Array<Double>,
    override val t: Array<Double>,
    override val x: Array<Double>,
    override val y: Array<Double>,
) : ConnectionVariables<Double>

fun IloConnectionVariables(n: Int, e: Int, cplex: IloCplex): ConnectionVariables<IloNumVar> {
    val actualN = if (e != 0) n else 0

    return object : ConnectionVariables<IloNumVar> {
        override val r = Array(actualN) { cplex.numVar(0.0, 1.0, IloNumVarType.Int, "r".toVarName(it)) }
        override val q = Array(actualN) { cplex.numVar(0.0, Consts.INF, IloNumVarType.Float, "q".toVarName(it)) }
        override val s = Array(actualN) { cplex.numVar(0.0, 1.0, IloNumVarType.Int, "s".toVarName(it)) }
        override val t = Array(actualN) { cplex.numVar(0.0, Consts.INF, IloNumVarType.Float, "t".toVarName(it)) }
        override val x = Array(e) { cplex.numVar(0.0, 1.0, IloNumVarType.Int, "x".toVarName(it)) }
        override val y = Array(e) { cplex.numVar(0.0, 1.0, IloNumVarType.Int, "y".toVarName(it)) }

    }
}
