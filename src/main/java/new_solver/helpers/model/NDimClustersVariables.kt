package new_solver.helpers.model

import ilog.concert.IloNumVar
import ilog.concert.IloNumVarType
import ilog.cplex.IloCplex
import new_solver.common.toVarName
import new_solver.helpers.clustering.direction_to_cluster_mapper.Cluster
import new_solver.helpers.model.NDimClustersVariables.*

interface NDimClustersVariables<T> : Variables<T> {
    var clusters: Array<ClusterVars<T>>

    class ClusterVars<T>(
        var fixedSign: T,
        val alphaSign: Array<T>,
    )

    override fun allVariables(): List<T> {
        return clusters.flatMap { i ->
            listOf(
                i.fixedSign,
                *i.alphaSign
            )
        }
    }
}

fun ActualNDimClustersVariables(clusterVars: Array<ClusterVars<Double>>) =
    object : NDimClustersVariables<Double> {
        override var clusters: Array<ClusterVars<Double>> = clusterVars
    }

fun IloNDimClustersVariables(clusterEndpointsNum: Int, clusterVars: Array<Cluster>, cplex: IloCplex) =
    object : NDimClustersVariables<IloNumVar> {
        override var clusters: Array<ClusterVars<IloNumVar>> = Array(clusterVars.size) { i ->
            ClusterVars(
                fixedSign = cplex.numVar(0.0, 1.0, IloNumVarType.Int, "fixedSign".toVarName(i)),
                alphaSign = Array(clusterEndpointsNum) {
                    cplex.numVar(0.0, 1.0, IloNumVarType.Int, "alphaSign".toVarName(i, it))
                }

            )
        }
    }