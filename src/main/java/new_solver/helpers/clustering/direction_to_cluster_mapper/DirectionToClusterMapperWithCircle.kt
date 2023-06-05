package new_solver.helpers.clustering.direction_to_cluster_mapper

import config.Flags
import ext.times
import legacy.utils.Matrix
import new_solver.logger.Logger
import new_solver.helpers.angle
import new_solver.helpers.angleDeg
import new_solver.helpers.l2
import new_solver.logger.Logger.Companion.withLogger
import org.apache.commons.math3.linear.MatrixUtils
import kotlin.math.*
import kotlin.random.Random
import kotlin.system.exitProcess

@Deprecated("Used to test algorithm with connections constraints. Outdated")
class DirectionToClusterMapperWithCircle(
    val flags: Flags,
) : DirectionsToClustersMapper {

    val className = "DirectionToClusterMapperWithCircle"

    var clusterLength = Random.nextDouble(
        flags.clusteringRadius - flags.clusteringRadiusRange, flags.clusteringRadius + flags.clusteringRadiusRange
    )

    override operator fun invoke(
        directions: List<DoubleArray>,
        matrix: Matrix,
    ): List<Cluster> = withLogger(className) {
        val endpoints = nest("finding endpoints") { findClusterEndpoints(directions, clusterLength) }
        return@withLogger nest("finding clusters") { findClusters(directions, endpoints, matrix) }
    }


    fun Logger.findClusterEndpoints(
        directions: List<DoubleArray>, clusterLength: Double
    ): List<List<DoubleArray>> {
        val d = directions[0].size
        return directions.map { direction ->
            val directionL2 = l2(direction)

            direction.mapIndexed { index, _ ->
                val basisVector = MutableList(d) { 0.0 }.apply { set(index, direction[index].sign) }.toDoubleArray()

                val basisLength = directionL2 / cos(basisVector angle direction)

                val v = DoubleArray(d) { i -> basisVector[i] * basisLength - direction[i] }
                val vNorm = sqrt(v.sumOf { it.pow(2) })
                val u = DoubleArray(d) { i -> v[i] / vNorm }

                DoubleArray(d) { i -> direction[i] + clusterLength * u[i] }
                    .also { result ->
                        if (result.any { it == 0.0 }) {
                            d("STRANGE BEHAVIOUR", "D is 0")
                        }
                    }
            }.also { endpoint ->
                val rv = endpoint.map { radVec -> radVec + (direction.map { -it }) }
                for (i in rv.indices) {
                    val res = rv.map { rv[i] angleDeg it }
                    nest("Direction $i") {
                        d("Direction angles", res)
                        d("Summary", res.sumOf { x -> x })
                    }
                }
            }
            //    val cs = DirectionToClusterMapperWithCircle.findClusterEndpoints(arrayOf(a.toDoubleArray()), 1.0)[0]
//    cs.forEach { cA ->
//        val c = cA.toList()
//        println("===DELIMITER===")
//        val b = (a + (-c)) * -1.0
//        println("len a = " + l2(a))
//        println("len b = " + l2(b))
//        println("len c = " + l2(c))
//        println("check sum = " + (l2(a).pow(2) + l2(b).pow(2)) + "=" + l2(c).pow(2))
//        println("a angle b = " + Math.toDegrees(a angle b))
//        println("a angle c = " + Math.toDegrees(a angle c))
//        println("b angle c = " + Math.toDegrees(b angle c))
//        println("check sum = " + (Math.toDegrees(a angle b) + Math.toDegrees(a angle c) + Math.toDegrees(b angle c)))
//    }

        }
    }

    private fun Logger.findClusters(
        directions: List<DoubleArray>, clusterEndpoints: List<List<DoubleArray>>, matrix: Matrix
    ): List<Cluster> {
        val result = List(clusterEndpoints.size) { Cluster(directions[it], emptyList(), IntArray(0)) }

        clusterEndpoints.forEachIndexed { index, endpoints ->
            val newEP = endpoints
            val basis = MatrixUtils.createRealMatrix(newEP.toTypedArray()).transpose()
            val transformBasis = MatrixUtils.inverse(basis)
            val dotsInBasis = (transformBasis * matrix.entry.transpose()).transpose()

            result[index].endpoints = endpoints
            result[index].points =
                dotsInBasis.data.mapIndexedNotNull { i, v -> if (v.all { it.sign > 0 }) i else null }.toIntArray()
        }
        d("Cluster avg size", "${result.map { it.points.size }.average()}")
        d("Cluster max size", "${result.maxOfOrNull { it.points.size }}")

        return result
    }
}
