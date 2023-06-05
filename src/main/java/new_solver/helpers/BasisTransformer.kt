package new_solver.helpers

import ext.println
import ext.times
import ext.toRealMatrix
import new_solver.logger.Logger.Companion.withLogger
import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.linear.QRDecomposition
import org.apache.commons.math3.linear.RealMatrix
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

interface BasisTransformer {

    fun findTransformMatrix(direction: DoubleArray): RealMatrix

    fun transformToNewBasis(transformMatrix: RealMatrix, matrix: RealMatrix): RealMatrix {
        return transformMatrix * matrix
    }

    fun transformToNewBasis(direction: DoubleArray, matrix: RealMatrix): RealMatrix {
        return transformToNewBasis(findTransformMatrix(direction), matrix)
    }
}

class BasisTransformerImpl : BasisTransformer {

    override fun findTransformMatrix(direction: DoubleArray): RealMatrix = withLogger("BTI#findNewBasis") {
        val n = direction.size

        val transformMatrix = MatrixUtils.createRealMatrix(n, n)
        for (i in 0 until n) {
            transformMatrix.setEntry(i, i, 1.0)
            transformMatrix.setEntry(i, 0, direction[i])
        }
//        transformMatrix.println("transformMatrix", format = true)
        val qrDecomposition = QRDecomposition(transformMatrix, 1e-10)
        val printLogs = true
        var r = qrDecomposition.q
//        r.println()
        if (r.getColumn(0).toList() angleDeg direction.toList() > 1e-4) {
//            r = r.transpose()
            val lol = (qrDecomposition.q.getColumn(0).toList() * -1.0).toDoubleArray()
            r.setColumn(0, lol)
        }
        return@withLogger r
    }
}

fun main() {
    val bt: BasisTransformer = BasisTransformerImpl()

    val direction = arrayOf(1.0, 1.0).toDoubleArray()
    val newBasis = bt.findTransformMatrix(direction)

    // region: ploting
    val circle = Array<RealMatrix>(4) { i ->
        List(100) { j ->
            val scale = (i + 1) / 4.0
            val rad = Math.PI / 2 * i + Math.PI / 2 * (j / 100.0)
            doubleArrayOf(sin(rad) * scale, cos(rad) * scale)
        }.toRealMatrix().transpose()
    }
    val direc = List(1) { direction }.toRealMatrix().transpose()
    val dots = listOf(
//        doubleArrayOf(0.0, 1.0),
        direction
    ).toRealMatrix().transpose()
    val dots2 = listOf(
        doubleArrayOf(1.0, 0.0),
    ).toRealMatrix().transpose()
    val nb = newBasis
    bt.transformToNewBasis(nb, dots).data.forEachIndexed { i, v ->
        println("y$i = ${v.toList()}")
    }
    bt.transformToNewBasis(nb, dots2).data.forEachIndexed { i, v ->
        println("x$i = ${v.toList()}")
    }
    circle.forEachIndexed { i, ci ->
        println("c$i = []")
        bt.transformToNewBasis(nb, ci).data.forEachIndexed { j, v ->
            println("c$i.append(${v.toList()})")
        }
    }
    bt.transformToNewBasis(nb, direc).transpose().data.forEachIndexed { i, v ->
        println("drN = ${v.toList()}")
    }
    println("drO = " + direction.toList())
    // endregion

    for (i in 2 until 3) {
        val d = i
        val dPow = 2.0.pow(d).toInt()
        val endPoints = Array(dPow) { j ->
            val x = j.toString(2)
            val xf = "0".repeat(d - x.length) + x
            println(xf)
            List(d) { i -> if (xf[i] == '0') -1.0 else 1.0 }
        }

        endPoints.forEachIndexed { ii, v ->
            println("Progress: $i + $ii")
            val newBasis = bt.findTransformMatrix(v.toDoubleArray())

            val b = bt.transformToNewBasis(
                newBasis,
                List(1) { v.toDoubleArray() }.toRealMatrix()
                    .transpose()
            ).transpose().data[0]

            if (newBasis.getColumn(0).toList() angleDeg v.toList() > 1e-2) {
                println("ERROR 1.1 : ${v angleDeg b.toList()}")
            }

            if (b.toList() angleDeg List(v.size) { if (it == 0) 1.0 else 0.0 } > 1e-2) {
                println("ERROR 1 : ${v angleDeg b.toList()}")
                newBasis.println()
                println("B " + b.toList())
                println("v " + v.toList())
            }

//            newBasis.println("NB")
//            println("V: ${v.toList()}")
//            println("B: ${b.toList()}")

//            return
            for (j in 0 until 500) {
                val newV = List(v.size) { v[it] * Random.nextDouble(0.1, 100.0) }
                val newB = bt.transformToNewBasis(
                    newBasis,
                    List(1) { newV.toDoubleArray() }.toRealMatrix().transpose()
                ).transpose().data[0]
//                println(newB.toList())
                val ones = DoubleArray(d) { if (it == 0) 1.0 else 0.0 }

                if ((ones.toList() angleDeg newB.toList()) > 45.0) {
//                    println()
//                    println("ERROR: ${ones.toList() angleDeg newB.toList()}")
                }

            }

        }

    }

}


fun lol(){
//    org.apache.commons.math3.geometry.spherical
}