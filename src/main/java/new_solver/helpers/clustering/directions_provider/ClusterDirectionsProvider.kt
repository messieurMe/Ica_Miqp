package new_solver.helpers.clustering.directions_provider

interface ClusterDirectionsProvider {
    fun provide(points: Array<DoubleArray>): List<DoubleArray>
}