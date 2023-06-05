package legacy.helpers

import org.apache.commons.math3.linear.EigenDecomposition
import legacy.utils.Matrix
import java.io.PrintWriter

object MatrixWriter {

    fun write(file: String, matrix: Matrix, namingMap: Map<String, Int>) {
        PrintWriter(file).use { out ->
            val sb = StringBuilder()
            for (i in matrix.namingList) {
                sb.append(i + "\t")
                val j: Int = namingMap[i]!!
                for (k in 0 until matrix.numCols()) {
                    sb.append(matrix.getElem(j, k))
                    sb.append("\t")
                }
                sb.dropLast(1)
                sb.append("\n")
            }
            sb.delete(sb.length - 1, sb.length)
            out.println(sb)

            val eigenDecomposition = EigenDecomposition(matrix.entry)
            eigenDecomposition

        }
    }
}