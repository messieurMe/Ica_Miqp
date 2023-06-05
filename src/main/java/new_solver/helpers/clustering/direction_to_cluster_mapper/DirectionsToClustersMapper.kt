package new_solver.helpers.clustering.direction_to_cluster_mapper

import legacy.utils.Matrix

interface DirectionsToClustersMapper {
    fun invoke(directions: List<DoubleArray>, matrix: Matrix): List<Cluster>

}
