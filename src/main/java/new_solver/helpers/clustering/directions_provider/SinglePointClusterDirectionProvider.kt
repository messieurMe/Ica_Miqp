package new_solver.helpers.clustering.directions_provider

class SinglePointClusterDirectionProvider : ClusterDirectionsProvider {
    override fun provide(points: Array<DoubleArray>): List<DoubleArray> {
        return points.toList()
    }
}