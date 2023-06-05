package legacy.analysis

import ext.println
import org.apache.commons.math3.linear.DiagonalMatrix
import org.apache.commons.math3.linear.EigenDecomposition
import org.apache.commons.math3.stat.correlation.Covariance
import legacy.utils.Matrix
import kotlin.math.sqrt

object Whitening {

    val EPS = 1e-6

    operator fun invoke(matrix: Matrix): Matrix {
        println("DEBUG")
        val sigma = (
                matrix
                    .transpose()
                    .mult(matrix)
                    .div(matrix.numRows().toDouble())
                ).also { it.println("sigma") } // NxN
        val eigenDecomposition = EigenDecomposition(sigma.entry)
        val eigenValues = eigenDecomposition.d.also { it.println("eigenValues") } // NxN
        val eigenVectors = eigenDecomposition.v.also { it.println("eigenVectors") } // NxN
        val lambda = DiagonalMatrix(eigenValues.rowDimension)
        println(lambda.rowDimension)
        for (i in 0 until lambda.rowDimension) {
            val newValue = 1.0 / (sqrt(eigenValues.getEntry(i, i)) + EPS)
            if (newValue.isFinite()) {
                lambda.setEntry(i, i, newValue)
            }
        }
        lambda.also { it.println("lambda") }
        val eigenVectorsT = eigenVectors.transpose()
        val w = (eigenVectors.multiply(lambda)).multiply(eigenVectorsT).also { it.println("w") }
//        val w2 = (lambda).multiply(eigenVectorsT).also { it.println("w") }

        val dataWhitened = (matrix.mult(Matrix(w)))
//        val dataW2 = (Matrix(w2).mult(matrix.transpose())).transpose()
//        checkCov(dataW2)
        checkCov(dataWhitened)
//        System.exit(0)
//        matrix.entry = dataWhitened.entry
        return dataWhitened
    }

    private fun checkCov(matrix: Matrix) {
        println("COV")
//        matrix.println()
        val cov = Covariance(matrix.entry).covarianceMatrix
        cov.println(format = true)
    }
}