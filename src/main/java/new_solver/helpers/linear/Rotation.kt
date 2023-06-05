package new_solver.helpers.linear

import ext.times
import ext.toRealMatrix
import new_solver.helpers.angleDeg
import new_solver.helpers.div
import new_solver.helpers.l2
import new_solver.helpers.times
import new_solver.logger.Logger
import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.linear.RealMatrix
import kotlin.math.*
import kotlin.random.Random

class GivensRotations() {
    fun rotate(from: DoubleArray, to: DoubleArray, vs: Array<DoubleArray>): Array<DoubleArray> =
        Logger.withLogger("GivensRotations#rotate") {
            val n = from.size

            var fromR = from.copyOf() / l2(from)
            val toR = to.copyOf() / l2(to)
            val fromIJ = DoubleArray(n)
            val toIJ = DoubleArray(n)

            repeat(3) {
                for (dimensionI in 0 until n) {
//                for (dimensionJ in dimensionI + 1 until n) {
                    for (dimensionJ in 0 until n) {
                        if (dimensionI == dimensionJ) continue
                        for (i in 0 until n) {
                            if (i == dimensionI || i == dimensionJ) {
                                fromIJ[i] = fromR[i]
                                toIJ[i] = toR[i]
                            } else {
                                fromIJ[i] = 0.0
                                toIJ[i] = 0.0
                            }
                        }

                        val angle = atan2(
                            (fromIJ[dimensionI] * toIJ[dimensionJ] - fromIJ[dimensionJ] * toIJ[dimensionI]),
                            (fromIJ[dimensionI] * toIJ[dimensionI] + fromIJ[dimensionJ] * toIJ[dimensionJ]),
                        )


                        val givensRotationMatrix = Array(n) { i ->
                            DoubleArray(n) { j ->
                                when {
                                    i == j && i != dimensionI && j != dimensionJ -> 1.0
                                    i == j -> cos(angle)
                                    i == dimensionI && j == dimensionJ -> -sin(angle)
                                    i == dimensionJ && j == dimensionI -> sin(angle)
                                    else -> 0.0
                                }
                            }
                        }.toRealMatrix()

                        fromR = (givensRotationMatrix * MatrixUtils.createRealMatrix(Array(1) { fromR })
                            .transpose()).getColumn(0)

                        for (i in vs.indices) {
                            val viRealMatrix = MatrixUtils.createRealMatrix(Array(1) { vs[i] }).transpose()
                            vs[i] = (givensRotationMatrix * viRealMatrix).getColumn(0)

                        }
                    }
                }
            }
            return@withLogger vs
        }
}

fun main() {

    val v1 = DoubleArray(13) { (if (Random.nextBoolean()) 1.0 else -1.0) * Random.nextDouble(0.0001, 100.0) }
    val v2 = DoubleArray(13) { (if (Random.nextBoolean()) 1.0 else -1.0) * Random.nextDouble(0.0001, 100.0) }

//    val v1 = doubleArrayOf(1.0, -0.5, -1.0)
//    val v2 = doubleArrayOf(1.0, 1.0, 0.5)
    println(v1 angleDeg v2)
//   \ |
//    \|__
//     |
//

    val vs = arrayOf(
        v1.copyOf(), //
//        v2,
//        doubleArrayOf(-1.0, 0.0, 0.0, 0.0),
//        doubleArrayOf(-1.0, -1.0, 0.5, 0.5)
    )

    println(v1 angleDeg v2)
    val r = GivensRotations().rotate(v1.copyOf(), v2.copyOf(), vs)
    r.forEach {
        println(/*it.toList() +*/ " | " + (it angleDeg v1) + " | " + (it angleDeg v2) + " | " + "${l2(it)} vs ${l2(v1)}")
    }

//    val circle = List(16) { j ->
//        val scale = (j + 1) / 4.0
//        val rad = Math.PI * 2.0 * (j / 16.0)
//        doubleArrayOf(sin(rad), cos(rad))
//    }

//    circle.forEach { fromIJ ->
//        println("FROM: ${fromIJ.toList()}")?
//        circle.forEach { toIJ ->
//            val dimensionI = 0
//            val dimensionJ = 1
//            val angle = atan2(
//                (fromIJ[dimensionI] * toIJ[dimensionJ] - fromIJ[dimensionJ] * toIJ[dimensionI]),
//                (fromIJ[dimensionI] * toIJ[dimensionI] + fromIJ[dimensionJ] * toIJ[dimensionJ])
//            )
//            println("TO: ${toIJ.toList()}")
//            println(Math.toDegrees(angle))
//        }
//        println("ROUND")
//    }
}