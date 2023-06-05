package new_solver.optimisations.mip_start

import ext.column
import legacy.utils.Matrix
import config.Consts.COMPONENTS
import config.Consts.DUMMY_DATA_N
import kotlin.math.absoluteValue

class MipStart {

    fun getA(): Nothing = error("Not Implemented")

    fun getP(): Nothing = error("Not Implemented")

    fun getAP(matrix: Matrix, icaDirection: Int): Pair<DoubleArray, Double> {
        val a = DoubleArray(matrix.numCols())
        val p = DoubleArray(matrix.numRows())
        val icaDirectionSize = (DUMMY_DATA_N / COMPONENTS)
        val start = icaDirection * icaDirectionSize
        val end = start + icaDirectionSize

        val icaElems = matrix.column(icaDirection)
        var icaSum = 0.0
        for (i in 0 until icaElems.size){
            icaSum += icaElems[i].absoluteValue
        }
        val diff = matrix.numRows().toDouble() / icaSum

        for (i in p.indices) {
            p[i] = matrix.getElem(i, icaDirection) * diff
        }


        a[icaDirection] = diff

        println("AP found")
        var sum = 0.0
        for (i in p.indices) {
            sum += p[i].absoluteValue
            if((p[i].absoluteValue - (matrix.getElem(i, icaDirection) * diff).absoluteValue).absoluteValue > 1e-4){
                error("Sum doesnt match")
            }
        }

        println(
            "" +
                    "\tSum for p is $sum\n" +
                    "\tDiff is $diff\n" +
                    "\tP is ${p.toList()}"
        )
        return (p to diff)

    }
}