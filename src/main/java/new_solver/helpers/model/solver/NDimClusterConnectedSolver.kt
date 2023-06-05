package new_solver.helpers.model.solver

import ilog.concert.IloNumVar
import ilog.cplex.IloCplex
import new_solver.helpers.clustering.direction_to_cluster_mapper.Cluster
import new_solver.helpers.model.*
import new_solver.helpers.model.NDimClustersVariables.*

interface NDimClusterConnectedVariables<T> :
    BasicVariables<T>,
    ConnectionVariables<T>,
    NDimClustersVariables<T> {

    override fun allVariables(): List<T> {
        return super<BasicVariables>.allVariables() +
                super<ConnectionVariables>.allVariables() +
                super<NDimClustersVariables>.allVariables()
    }
}

interface NDimClusterConnectedIloVariables : NDimClusterConnectedVariables<IloNumVar>

interface NDimClusterConnectedActualVariables : NDimClusterConnectedVariables<Double>

fun NDimClusterConnectedIloVariables(
    n: Int,
    d: Int,
    e: Int,
    clusterEndpointsNum: Int,
    clusters: Array<Cluster>,
    cplex: IloCplex,
): NDimClusterConnectedIloVariables {
    return object : NDimClusterConnectedIloVariables,
        BasicVariables<IloNumVar> by IloBasicVariables(n = n, d = d, cplex),
        ConnectionVariables<IloNumVar> by IloConnectionVariables(n, e, cplex),
        NDimClustersVariables<IloNumVar> by IloNDimClustersVariables(clusterEndpointsNum, clusters, cplex) {

        override fun allVariables(): List<IloNumVar> {
            return super<NDimClusterConnectedIloVariables>.allVariables()
        }
    }
}

fun NDimClusterConnectedActualVariables(
    a: Array<Double>,
    f: Array<Double>,
    g: Array<Double>,
    alpha: Array<Double>,
    r: Array<Double>?,
    q: Array<Double>?,
    s: Array<Double>?,
    t: Array<Double>?,
    x: Array<Double>?,
    y: Array<Double>?,
    clusters: Array<ClusterVars<Double>>
): NDimClusterConnectedActualVariables {
    return object : NDimClusterConnectedActualVariables,
        BasicVariables<Double> by ActualBasicVariables(
            a = a,
            f = f,
            g = g,
            alpha = alpha
        ),
        ConnectionVariables<Double> by ActualConnectionVariables(
            r = r ?: emptyArray(),
            q = q ?: emptyArray(),
            s = s ?: emptyArray(),
            t = t ?: emptyArray(),
            x = x ?: emptyArray(),
            y = y ?: emptyArray(),
        ),
        NDimClustersVariables<Double> by ActualNDimClustersVariables(clusters) {

        override fun allVariables(): List<Double> {
            return super<NDimClusterConnectedActualVariables>.allVariables()
        }
    }
}