package new_solver.solvers

import new_solver.helpers.model.Constants
import new_solver.solvers.model.Logs

interface Solver {

    fun solve(): Boolean

    fun writeResult()

    fun icaDirection(): DoubleArray
}