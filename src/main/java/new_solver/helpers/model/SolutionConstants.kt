package new_solver.helpers.model

import legacy.graph.Graph
import legacy.utils.Matrix
import new_solver.helpers.clustering.direction_to_cluster_mapper.Cluster

interface Constants

interface BaseConstants : Constants {
    val matrix: Matrix
    val n: Int get() = matrix.numRows()
    val d: Int get() = matrix.numCols()
}

interface ConnectedConstants : Constants {
    val graph: Graph
}

interface ClusterConstants : Constants {
    val clusters: Array<Cluster>
}