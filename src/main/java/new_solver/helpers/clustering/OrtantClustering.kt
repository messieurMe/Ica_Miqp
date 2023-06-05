package new_solver.helpers.clustering

import config.Flags
import legacy.utils.Matrix
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sign

class OrtantClustering {

    fun provide(matrix: Matrix): Map<Int, Pair<List<Double>, MutableSet<Int>>> {
        val d = matrix.numCols()
        val d2 = matrix.numCols() * 2
        val dp = 2.toDouble().pow(d).toInt()

        val mapa = mutableMapOf<Int, Pair<List<Double>, MutableSet<Int>>>()

        val endPoints = Array(dp) {
            val x = it.toString(2)
            val xf = "0".repeat((dp - 1).toString(2).length - x.length) + x
            List(d) { i -> if (xf[i] == '0') -1.0 else 1.0 }
        }
        endPoints.forEachIndexed { index, ints ->
            mapa[index] = Pair(ints, mutableSetOf())
        }


        val n = matrix.numRows()
        for (i in 0 until n) {
            val r = matrix.getRow(i)
            val ra = r.map { it.sign }
            mapa.forEach { (_, u) ->
                if (u.first.zip(ra).all { (a, b) -> a == b.sign }) {
                    u.second.add(i)
                    return@forEach
                }
            }
        }
        return mapa.filter { it.value.second.size > 0 }
    }
}

fun main() {
    OrtantClustering().provide(
        Matrix(doubleArrayOf(-0.0001, 1.0))
    ).forEach{
        println(it.value.first)
        println(it.value.second.toList())
    }
}