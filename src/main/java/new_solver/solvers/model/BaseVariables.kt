package new_solver.solvers.model

import ilog.concert.IloNumVar

open class BaseVariables(
    val a: Array<IloNumVar>,
    val f: Array<IloNumVar>,
    val g: Array<IloNumVar>,
    val alpha: Array<IloNumVar>,
    val beta: Array<IloNumVar>
)
