package new_solver.helpers.solution_adapter

import new_solver.helpers.model.Constants
import new_solver.helpers.model.Variables

interface Adapter<V : Variables<Double>, C : Constants> {

    fun adapt(
        rawSolution: V,
        constants: C
    ): Boolean
}
