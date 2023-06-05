package new_solver.solvers.callbacks.generic

import ilog.concert.IloNumVar
import ilog.cplex.IloCplex
import kotlinx.coroutines.*
import new_solver.helpers.model.NDimClustersVariables
import new_solver.helpers.model.solver.NDimClusterConnectedActualVariables
import new_solver.helpers.model.solver.NDimClusterConnectedIloVariables
import new_solver.helpers.model.solver.NDimClusterConnectedSolverConstants
import new_solver.helpers.solution_adapter.NDimClusterConnectedSolutionAdapter
import java.io.PrintWriter
import kotlin.math.pow

class GenCallback(
    val v: NDimClusterConnectedIloVariables,
    val consts: NDimClusterConnectedSolverConstants,
    val adapter: NDimClusterConnectedSolutionAdapter
) : IloCplex.Callback.Function {


    var counter = 0

    override fun invoke(context: IloCplex.Callback.Context) {
        try {
            when {
                context.inGlobalProgress() -> {
//                    updateBestSolution(context)
//                    postHeuristic(context)
                }

                context.inRelaxation() -> {
                    postHeuristic(context)
                }

                context.inBranching() -> {
                    val boundsFS = v.clusters
                        .map { context.getLocalLB(it.fixedSign) to context.getLocalUB(it.fixedSign) }
                    for(i in boundsFS.indices){
                        if(boundsFS[i].first == 0.0 && boundsFS[i].second == 1.0){
                            context.makeBranch(v.clusters[i].fixedSign, 1.0, IloCplex.BranchDirection.Up, context.relaxationObjective)
                            context.makeBranch(v.clusters[i].fixedSign, 0.0, IloCplex.BranchDirection.Down, context.relaxationObjective)
                            return
                        }
                    }

                    v.clusters.map { context.getLocalLB(it.fixedSign) }
                        for (i in 0 until consts.clusters.size) {
                            if (context.getLocalLB(v.clusters[i].fixedSign) == 1.0) {
                                val alphaSignsLB = context.getLocalLB(v.clusters[i].alphaSign)
                                val alphaSignsUB = context.getLocalUB(v.clusters[i].alphaSign)

                                if (alphaSignsLB.all { it == 1.0 }) {
                                    consts.clusters[i].points.forEach {
                                        if (context.getLocalUB(v.alpha[it]) == 0.0) {
                                            context.pruneCurrentNode()
                                        }else if (context.getLocalLB(v.alpha[i]) == 0.0
                                        ){
                                            context.makeBranch(v.alpha[it], 1.0, IloCplex.BranchDirection.Up, context.relaxationObjective)
                                            context.makeBranch(v.alpha[it], 0.0, IloCplex.BranchDirection.Down, context.relaxationObjective)
                                        }
                                    }
                                }

                                if (alphaSignsUB.all { it == 0.0 }) {
                                    consts.clusters[i].points.forEach {
                                        if (context.getLocalLB(v.alpha[it]) == 1.0) {
                                            context.pruneCurrentNode()
                                        }
                                    }
                                }
                            }
                        }
                }
            }
        } catch (e: Exception) {
            println("Exception in GenCallback ${e.message}")
        }
    }

    @Volatile
    private var solutionWriterJob: Job? = null

    private fun updateBestSolution(context: IloCplex.Callback.Context) = with(context) {

        val variables = listOf(
            v.a to "A",
            v.f to "F",
            v.g to "G",
            v.q to "Q",
            v.r to "R",
            v.s to "S",
            v.t to "T",
            v.x to "X",
            v.y to "Y"
        ).map { (getIncumbent(it.first) to it.second) }

        solutionWriterJob?.cancel()
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.IO) {
                PrintWriter("./answers/rtd.r").use { pw ->
                    variables.forEach { (data, title) ->
                        pw.println(title)
                        pw.println(data.toList())
                    }
                }
            }
        }
    }

    @Volatile
    var lastBestInteger = Double.MIN_VALUE

    private fun postHeuristic(context: IloCplex.Callback.Context) = with(context) {
        val sol = NDimClusterConnectedActualVariables(
            a = getArrayValues((v.a)),
            f = getArrayValues((v.f)),
            g = getArrayValues((v.g)),
            alpha = getArrayValues((v.alpha)),
            r =  getArrayValues((v.r)),
            q =  getArrayValues((v.q)),
            s =  getArrayValues((v.s)),
            t =  getArrayValues((v.t)),
            x =  getArrayValues((v.x)),
            y =  getArrayValues((v.y)),
            clusters = Array(v.clusters.size) { i ->
                val currentCluster = v.clusters[i]
                NDimClustersVariables.ClusterVars(
                    fixedSign = context.getRelaxationPoint(currentCluster.fixedSign),
                    alphaSign = getArrayValues(currentCluster.alphaSign)
                )
            },
        )
        if (adapter.adapt(sol, consts)) {
            val obj = calcObjective(sol)

            if (context.incumbentObjective < obj && lastBestInteger < obj) {
                println("===New solution===\n\tCurrent: ${context.incumbentObjective}, $lastBestInteger\n\tFound: $obj")
                lastBestInteger = obj

                val vars = v.allVariables().toTypedArray()
                val vals = sol.allVariables().toDoubleArray()

                context.postHeuristicSolution(
                    vars,
                    vals,
                    0,
                    vars.size,
                    obj,
                    IloCplex.Callback.Context.SolutionStrategy.CheckFeasible
                )
            }
        }
    }

    private fun calcObjective(sol: NDimClusterConnectedActualVariables): Double {
        var sum = 0.0
        val n10 = 10.0
        sum += sol.a.sumOf { it.pow(2) * n10 }
        sum -= sol.f.zip(sol.q).sumOf { (it.first - it.second).pow(2) }
        sum -= sol.g.zip(sol.t).sumOf { (it.first - it.second).pow(2) }
        return sum
    }

    private fun IloCplex.Callback.Context.getArrayValues(values: Array<IloNumVar>): Array<Double> {
        return getRelaxationPoint(values).toTypedArray()
    }
}
