package new_solver.solvers.model

import ilog.concert.IloNumVar

class Variable(
    val a: Array<IloNumVar>,
    val f: Array<IloNumVar>,
    val g: Array<IloNumVar>,
    val alpha: Array<IloNumVar>,
    val beta: Array<IloNumVar>
)
