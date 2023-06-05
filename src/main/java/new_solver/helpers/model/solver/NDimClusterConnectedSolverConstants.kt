package new_solver.helpers.model.solver

import legacy.graph.Graph
import legacy.utils.Matrix
import new_solver.helpers.clustering.direction_to_cluster_mapper.Cluster
import new_solver.helpers.model.BaseConstants
import new_solver.helpers.model.ClusterConstants
import new_solver.helpers.model.ConnectedConstants

class NDimClusterConnectedSolverConstants(
    override val matrix: Matrix,
    override val n: Int,
    override val d: Int,
    override val graph: Graph,
    override val clusters: Array<Cluster>
) : BaseConstants, ConnectedConstants, ClusterConstants
