package new_solver.helpers.clustering.direction_to_cluster_mapper

import ext.times
import legacy.utils.Matrix
import new_solver.helpers.*
import new_solver.helpers.linear.GivensRotations
import new_solver.logger.Logger
import new_solver.logger.Logger.Companion.withLogger
import org.apache.commons.math3.linear.MatrixUtils
import java.util.LinkedList
import kotlin.math.*

class DirectionToClusterMapperWithGivensRotations(
    val angleDeg: Double
) : DirectionsToClustersMapper {
    val angle = Math.toRadians(angleDeg)

    override fun invoke(directions: List<DoubleArray>, matrix: Matrix): List<Cluster> =
        withLogger("Directions2Cluster#GivensRotations") {
            val n = matrix.numRows()
            val d = directions[0].size


            val primalDirection = DoubleArray(d) { 0.0 }.also { it[0] = 1.0 }

            val endpointsBase = Array(2 * (d - 1)) { i ->
                DoubleArray(d) { j ->
                    when {
                        j == 0 -> cos(angle)//1.0
                        (j - 1) == (i) / 2 && i % 2 == 0 -> sin(angle)//tan(angle)
                        (j - 1) == (i) / 2 && i % 2 == 1 -> -sin(angle)//-tan(angle)
                        else -> 0.0
                    }
                }
            }

            val angleEndpointDir = endpointsBase.map { it angleDeg primalDirection }
            d("Angle between endpoints and direction before rotations", angleEndpointDir.toList())
            d(
                "All angles correct \$before\$ rotations (allowed error ~1deg)",
                angleEndpointDir.all { (it - angleDeg).absoluteValue < 1.0 }
            )

            val endpointsRotated = directions.mapIndexed { index, direction ->
                val normDirection = direction / l2(direction)
                GivensRotations().rotate(
                    primalDirection.copyOf(),
                    normDirection,
                    endpointsBase.copyOf()
                ).toList()
            }

            val angleEndpointsDir2 = endpointsRotated.map { it }

            angleEndpointsDir2.run {
                var invalidAngles = 0

                val badAngles = LinkedList<Double>()
                for (i in indices) {
                    get(i).forEach {
                        val angleBetween = it angleDeg directions[i]

                        if ((angleBetween - angleDeg).absoluteValue > 1.0) {
                            invalidAngles++
                            badAngles.add(angleBetween)
                        }
                    }
                }
                d("All angles correct \$after \$ rotations (allowed error ~1deg)", invalidAngles == 0)
                if (invalidAngles > 0) {
                    nest("INVALID ANGLES") {
                        d("Failed to rotate \$n", invalidAngles)
                        d("Bad angles (should be ~90)", badAngles.toList())
                    }
                }

            }

            val result = nest("Finding clusters") { findClusters(directions, endpointsRotated, matrix) }
            d("Average cluster contains elements", result.map { it.points.size }.average())

            return@withLogger result
        }

    private fun Logger.findClusters(
        directions: List<DoubleArray>, clusterEndpoints: List<List<DoubleArray>>, matrix: Matrix
    ): List<Cluster> {

        val result = List(clusterEndpoints.size) { Cluster(directions[it], emptyList(), IntArray(0)) }

        clusterEndpoints.forEachIndexed { index, endpoints ->
            val d = endpoints.size / 2
            val dp = 2.0.pow(endpoints.size / 2).toInt()
            val endPointsAll = Array(dp) {
                val x = it.toString(2)
                val xf = "0".repeat((dp - 1).toString(2).length - x.length) + x
                List(d) { i ->
                    if (xf[i] == '0') endpoints[i * 2] else endpoints[i * 2 + 1]
                }
            }
            result[index].endpoints = endpoints

            val points = mutableSetOf<Int>()
            for (v in 0 until dp) {

                val newEP = List(1) { directions[index] } + endPointsAll[v]

                val basis = MatrixUtils.createRealMatrix(newEP.toTypedArray()).transpose()
                val transformBasis = MatrixUtils.inverse(basis)
                val dotsInBasis = (transformBasis * matrix.entry.transpose()).transpose()

                points.addAll(
                    dotsInBasis.data
                        .mapIndexedNotNull { i, vv -> if (vv.all { it.sign > 0.0 }) i else null }
//                        .also { xs -> d("Dots in basis size", xs.size) }
                )
            }
            result[index].points = points.toIntArray()

            for (i in directions.indices) {
                for (j in result[i].points) {
                    val angleBetween = directions[i] angleDeg matrix.getRow(j)
                    if (angleBetween > angleDeg) {
                        nest("ANGLE IS TOO BIG: ") {
                            d("expected", angleDeg)
                            d("actual", angleBetween)
                        }
                    } else {
//                        print("K${j * i + j}#")
                    }
                }
            }
        }

        d("Max points in cluster", result.maxOf { it.points.size })
        d("Min points in cluster", result.minOf { it.points.size })

        nest("All clusters") {
            result.forEachIndexed { i, v -> d("Dots in cluster $i", v.points.size) }
        }

        return result
    }
}
