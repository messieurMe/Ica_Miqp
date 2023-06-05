package ext

import org.apache.commons.math3.linear.RealMatrix
import legacy.utils.Matrix
import org.apache.commons.math3.linear.MatrixUtils
import java.text.DecimalFormat

fun List<DoubleArray>.toRealMatrix(): RealMatrix = MatrixUtils.createRealMatrix(this.toTypedArray())
fun Array<DoubleArray>.toRealMatrix(): RealMatrix = MatrixUtils.createRealMatrix(this)
fun RealMatrix.println(title: String? = null, format: Boolean = true) {
    title?.let { kotlin.io.println(title) }
    val df = DecimalFormat("+0.000;-0.000")
    val formatter: (Double) -> String = if (format) {
        { x -> df.format(x) }
    } else {
        { x -> x.toString() }
    }
    for (i in 0 until rowDimension) {
        for (j in 0 until columnDimension) {
            print("${formatter(getEntry(i, j))} ")
        }
        kotlin.io.println()
    }
}

operator fun RealMatrix.times(that: RealMatrix): RealMatrix = this.multiply(that)


fun Matrix.row(i: Int) = this.entry.getRow(i)!!
fun Matrix.column(i: Int) = this.entry.getColumn(i)!!
fun Matrix.println(title: String? = null) = entry.println(title)
