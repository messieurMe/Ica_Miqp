package ext

import ilog.concert.IloNumExpr
import ilog.concert.IloNumVar
import ilog.cplex.IloCplex

class CplexKtExt(val cplex: IloCplex) {

    operator fun IloNumExpr.plus(that: IloNumExpr): IloNumExpr = cplex.sum(this, that)!!
    operator fun IloNumExpr.minus(that: IloNumExpr): IloNumExpr = cplex.diff(this, that)
    operator fun IloNumExpr.times(that: IloNumExpr): IloNumExpr = cplex.prod(this, that)
    infix fun IloNumExpr.aLe(that: IloNumExpr): IloNumExpr = cplex.addLe(this, that)
    infix fun IloNumExpr.aGe(that: IloNumExpr): IloNumExpr = cplex.addGe(this, that)
    infix fun IloNumExpr.aEq(that: IloNumExpr): IloNumExpr = cplex.addEq(this, that)


    operator fun IloNumExpr.plus(that: Double): IloNumExpr = cplex.sum(this, that)!!
    operator fun IloNumExpr.minus(that: Double): IloNumExpr = cplex.diff(this, that)
    operator fun IloNumExpr.times(that: Double): IloNumExpr = cplex.prod(this, that)
    infix fun IloNumExpr.aLe(that: Double): IloNumExpr = cplex.addLe(this, that)
    infix fun IloNumExpr.aGe(that: Double): IloNumExpr = cplex.addGe(this, that)
    infix fun IloNumExpr.aEq(that: Double): IloNumExpr = cplex.addEq(this, that)

    operator fun Double.plus(that: IloNumExpr): IloNumExpr = cplex.sum(this, that)!!
    operator fun Double.minus(that: IloNumExpr): IloNumExpr = cplex.diff(this, that)
    operator fun Double.times(that: IloNumExpr): IloNumExpr = cplex.prod(this, that)
    infix fun Double.aLe(that: IloNumExpr): IloNumExpr = cplex.addLe(this, that)
    infix fun Double.aGe(that: IloNumExpr): IloNumExpr = cplex.addGe(this, that)
    infix fun Double.aEq(that: IloNumExpr): IloNumExpr = cplex.addEq(this, that)
}
