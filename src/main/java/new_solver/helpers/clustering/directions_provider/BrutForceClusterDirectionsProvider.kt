package new_solver.helpers.clustering.directions_provider

import new_solver.logger.Logger
import new_solver.helpers.angleDeg
import kotlin.math.max

class BrutForceClusterDirectionsProvider : ClusterDirectionsProvider {

    var availableAngle = 45
    var minClusterSize = 25

    override fun provide(points: Array<DoubleArray>): List<DoubleArray> = Logger.withLogger("BrutForcing clusters") {
        val setOfClusters = mutableSetOf<Int>()
        val allCl = mutableSetOf<Int>()
        val listPoints = points.map { it.toList() }
        var maxN = -1

        listPoints.forEachIndexed { index, point ->
            var clusterSize = 0
            for (i in index until points.size) {
                if (point angleDeg listPoints[i] < availableAngle) {
                    clusterSize++
                }
            }
            if (clusterSize > minClusterSize) {
                setOfClusters.add(index)
                maxN = max(maxN, clusterSize)
            }
            allCl.add(clusterSize)
        }

        d("maxN", maxN.toString())
        d("avg all", allCl.average().toString())
        d("avg best", setOfClusters.average().toString())
        return@withLogger setOfClusters.map { points[it] }
    }
}