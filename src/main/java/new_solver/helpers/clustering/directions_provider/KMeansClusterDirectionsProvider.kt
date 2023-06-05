package new_solver.helpers.clustering.directions_provider

import config.Flags
import new_solver.helpers.angleDeg
import new_solver.helpers.div
import new_solver.helpers.l2
import new_solver.logger.Logger.Companion.withLogger
import smile.clustering.KMeans
import java.util.LinkedList
import kotlin.system.exitProcess

class KMeansClusterDirectionsProvider(
    private val k: Int,
    private val maxIter: Int,
    private val tol: Double,
    private val minAngleBetweenDirections: Double,
    private val clusteringFilterCloseClusters: Boolean,
) : ClusterDirectionsProvider {

    override fun provide(points: Array<DoubleArray>): List<DoubleArray> = withLogger("KMeansDirectionProvider") {
        val p = points.map { it.toList() }.map { (it / l2(it)) }.map { it.toDoubleArray() }.toTypedArray()

        d("Expected number of clusters", k)

        var unwantedZeros = 0
        val r = KMeans.fit(p, k, maxIter, tol).centroids
            .filter { !it.any(Double::isNaN) }
            .map { it / l2(it) }
            .toTypedArray()
        r.forEach { d ->
            for (i in d.indices) {
                if (d[i] == 0.0) {
                    unwantedZeros++
                    d[i] += 1e-5
                }
            }
        }
        d("Direction slightly changed to avoid zeros in vectors", unwantedZeros)

        val dGoodDirections = mutableSetOf<Int>()

        for (i in r.indices) {
            var goodDirection = true
            for (j in i until r.size) if (i != j) {
                val angle = r[i] angleDeg r[j]
                if ((angle.isNaN() || angle < minAngleBetweenDirections) && clusteringFilterCloseClusters) {
                    goodDirection = false
                    break
                }
            }
            if (goodDirection) {
                dGoodDirections.add(i)
            }
        }
        d("Good direction percent", (dGoodDirections.size.toDouble() / r.size * 100).toString() + "%")
        d("Clusters left", dGoodDirections.size)

        val result = r.filterIndexed { i, _ -> i in dGoodDirections }

        val dAllAngles = mutableListOf<Double>()
        for (i in 0 until result.size) {
            for (j in i until result.size) if (i != j) {
                dAllAngles.add(result[i] angleDeg result[j])
            }
        }

        d("Average distance between directions", dAllAngles.average())

        return@withLogger r.filterIndexed { i, _ -> i in dGoodDirections }
    }
}