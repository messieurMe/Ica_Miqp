package new_solver.helpers.clustering.direction_to_cluster_mapper

class Cluster(
    val direction: DoubleArray,
    var endpoints: List<DoubleArray>,
    var points: IntArray
)
