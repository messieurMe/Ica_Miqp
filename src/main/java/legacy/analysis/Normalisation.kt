package legacy.analysis

import legacy.utils.Matrix

object Normalisation {

    operator fun invoke(matrix: Matrix): Matrix {
        val minMax = Array(matrix.numCols()) {
            with(matrix.entry.getColumn(it)) {
                min() to max()
            }
        }
        for (i in 0 until matrix.numCols()) {
            val diff = (minMax[i].second - minMax[i].first) / 2.0
            val min = minMax[i].first
            for (j in 0 until matrix.numRows()) {
                val entry = matrix.entry.getEntry(j, i)
                matrix.entry.setEntry(j, i, ((entry - min) / diff) - 1.0)
            }
        }
        return matrix
    }
}