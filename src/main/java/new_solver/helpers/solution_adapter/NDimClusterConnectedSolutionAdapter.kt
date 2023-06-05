package new_solver.helpers.solution_adapter

import legacy.algo.MST
import legacy.utils.Matrix
import config.Consts
import ext.println
import new_solver.helpers.l1
import new_solver.helpers.model.*
import new_solver.helpers.model.solver.NDimClusterConnectedActualVariables
import new_solver.helpers.model.solver.NDimClusterConnectedSolverConstants
import org.apache.commons.math3.linear.MatrixUtils
import kotlin.math.abs

interface NDimClusterConnectedSolutionAdapter :
    Adapter<NDimClusterConnectedActualVariables, NDimClusterConnectedSolverConstants>

class NDimClusterConnectedSolutionAdapterImpl() : NDimClusterConnectedSolutionAdapter {

    fun mul(matrix: Matrix, a: DoubleArray): DoubleArray {
        return matrix.mult(Matrix(a).transpose()).transpose().getRow(0)
    }

    override fun adapt(
        rawSolution: NDimClusterConnectedActualVariables,
        constants: NDimClusterConnectedSolverConstants
    ): Boolean {
        return adaptBasic(rawSolution, constants)
                && adaptConnected(rawSolution, constants)
                && adaptGivensClusters(rawSolution, constants)
//                && adaptClusters(rawSolution, constants)
    }


    private fun adaptBasic(
        rawSolution: BasicVariables<Double>, constants: NDimClusterConnectedSolverConstants
    ): Boolean {
        val p = mul(constants.matrix, rawSolution.a.toDoubleArray())

        val l1norm = l1(p.toList())

        if (l1norm < 0.1) {
            return false
        }

        var cff = 1.0
        if (abs(l1norm - constants.matrix.numRows()) > Consts.EPS) {
            cff = constants.matrix.numRows() / l1norm
        }

        for (i in rawSolution.a.indices) {
            rawSolution.a[i] *= cff
        }
        val newP = mul(constants.matrix, rawSolution.a.toDoubleArray())

        for (i in newP.indices) {
            if (newP[i] > 0) {
                rawSolution.f[i] = abs(newP[i])
                rawSolution.g[i] = 0.0
                rawSolution.alpha[i] = 1.0
            } else {
                rawSolution.f[i] = 0.0
                rawSolution.g[i] = abs(newP[i])
                rawSolution.alpha[i] = 0.0
            }
        }
        if (abs(l1(newP.toList()) - constants.matrix.numRows()) > Consts.EPS) {
//            throw RuntimeException("unexpected l1norm after adapt")
        }

        return true
    }

    private fun <T> adaptConnected(
        rawSolution: T, constants: NDimClusterConnectedSolverConstants
    ): Boolean where T : BasicVariables<Double>, T : ConnectionVariables<Double> = with(rawSolution) {
        if(rawSolution.q.isEmpty()){
            return@with true
        }

        System.arraycopy(f, 0, q, 0, f.size)
        System.arraycopy(g, 0, t, 0, g.size)

        val xx = x.toDoubleArray()
        val qq = q.toDoubleArray()
        val rr = r.toDoubleArray()
        val yy = y.toDoubleArray()
        val tt = t.toDoubleArray()
        val ss = s.toDoubleArray()

        MST.solve(constants.graph, xx, qq, rr, Consts.STEP)
        MST.solve(constants.graph, yy, tt, ss, Consts.STEP)

        xx.forEachIndexed { index, d -> x[index] = d }
        qq.forEachIndexed { index, d -> q[index] = d }
        rr.forEachIndexed { index, d -> r[index] = d }
        yy.forEachIndexed { index, d -> y[index] = d }
        tt.forEachIndexed { index, d -> t[index] = d }
        ss.forEachIndexed { index, d -> s[index] = d }

        return true
    }

    private fun <T> adaptClusters(
        rawSolution: T, constants: NDimClusterConnectedSolverConstants
    ): Boolean where T : BasicVariables<Double>, T : NDimClustersVariables<Double> {
        constants.clusters.zip(rawSolution.clusters).forEach { (constClust, givClust) ->
            val prod = mul(
                Matrix(MatrixUtils.createRealMatrix(constClust.endpoints.toTypedArray())),
                rawSolution.a.toDoubleArray()
            )

            for (i in prod.indices) with(givClust) {
                alphaSign[i] = if (prod[i] >= 0.0) 1.0 else 0.0
            }
            if (givClust.alphaSign.all { it == 1.0 } || givClust.alphaSign.all { it == 0.0 }) {
                givClust.fixedSign = 1.0
            }
        }
        return true
    }

    private fun <T> adaptGivensClusters(
        rawSolution: T, constants: NDimClusterConnectedSolverConstants
    ): Boolean where T : BasicVariables<Double>, T : NDimClustersVariables<Double> {
        constants.clusters.zip(rawSolution.clusters).forEach { (constClust, givClust) ->
            val prod = mul(
                Matrix(MatrixUtils.createRealMatrix(constClust.endpoints.toTypedArray())),
                rawSolution.a.toDoubleArray()
            )

            for (i in prod.indices) with(givClust) {
                alphaSign[i] = if (prod[i] >= 0.0) 1.0 else 0.0
            }
            if (givClust.alphaSign.all { it == 1.0 } || givClust.alphaSign.all { it == 0.0 }) {
                givClust.fixedSign = 1.0
            }
        }
        return true
    }

}

fun main() {
    val x = NDimClusterConnectedSolutionAdapterImpl()
    val matrix = Matrix(
        arrayOf(
            doubleArrayOf(0.0, 1.0, 2.0),
            doubleArrayOf(3.0, 4.0, 5.0)
        )
    )
    val a = doubleArrayOf(5.0, 6.0, 7.0)
    println(
        x.mul(matrix, a).toList()
    )
}