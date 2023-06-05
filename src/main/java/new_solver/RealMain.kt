package new_solver

import config.Consts
import config.Flags
import di.MainModule
import ext.column
import legacy.analysis.DataAnalysis.whitening
import legacy.graph.Graph
import legacy.io.GraphIO
import legacy.io.NewMatrixIO
import new_solver.common.FilePicker
import new_solver.common.computeIf
import new_solver.solvers.*
import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.linear.QRDecomposition
import java.io.PrintWriter
import java.util.LinkedList
import kotlin.io.path.Path
import kotlin.io.path.exists

fun main() {
    val flags = Flags()

    val module = MainModule(flags)

    val data = FilePicker.A2_TEST_05

    val namingMap: Map<String, Int> = HashMap()
    val revNamingMap: Map<Int, String> = HashMap()

    if (flags.dummyData) {
        module.dataGenerator.simpleGenerator(Consts.DUMMY_DATA_N, Consts.INITIAL_D, Consts.COMPONENTS)
    }

    var matrix = NewMatrixIO.read(data.matrix, true, namingMap, revNamingMap)

    module.plotlyPlots.scatter(
        matrix.column(0).toList(), matrix.column(1).toList(), name = "Initial data"
    )

    // preparing
    println(matrix.entry.getEntry(0, 0))

    computeIf(Consts.WHITENING) {
        matrix = whitening(matrix)

        module.plotlyPlots.scatter(
            matrix.column(0).toList(), matrix.column(1).toList(), name = "Whitening"
        )
    }

    // solver
    println("Matrix dim\n\td=${matrix.numCols()}\n\tn=${matrix.numRows()}")

    // graph

    val graph = if (Path(data.graph).exists()) {
        GraphIO.read(data.graph, namingMap, revNamingMap)
    } else {
        Graph(emptyMap(), emptyList(), emptyList())
    }


    val pqs = LinkedList<Pair<DoubleArray, DoubleArray>>()

    val initialD = matrix.numCols()

    while (matrix.numCols() >= 1) {
        val solver: NDimClusterConnectedSolver2 = module.nDimClusterConnectedSolver2.apply { addInput(matrix, graph) }

        val isSolved = solver.solve()
        println("Solved: $isSolved")

        if (isSolved) {
            solver.writeResult()
            solver.printResults(
                PrintWriter("./statistics/components/c${initialD - matrix.numCols()}")
            )
            pqs.add(
                Pair(
                    solver.cplex.getValues(solver.v.f),
                    solver.cplex.getValues(solver.v.g),
                )
            )

        } else {
            module.plotlyPlots.scatter(
                matrix.column(0).toList(), matrix.column(1).toList(), name = "No solutions"
            )
        }


        val icaDirection = solver.icaDirection()


        val transformMatrix = MatrixUtils.createRealMatrix(icaDirection.size, icaDirection.size + 1)
        for (i in 0 until transformMatrix.rowDimension) {
            transformMatrix.setEntry(i, i + 1, 1.0)
            transformMatrix.setEntry(i, 0, icaDirection[i])
        }

        val qrDecomposition = QRDecomposition(transformMatrix, 1e-10)
        val Q = qrDecomposition.q

        val newMatrix = Q.multiply(matrix.entry.transpose()).transpose()
        val newMatrixDecreased = MatrixUtils.createRealMatrix(newMatrix.rowDimension, newMatrix.columnDimension - 1)

        for (i in 0 until newMatrixDecreased.rowDimension) {
            for (j in 0 until newMatrixDecreased.columnDimension) {
                newMatrixDecreased.setEntry(i, j, newMatrix.getEntry(i, j + 1))
            }
        }

        matrix.entry = newMatrixDecreased
    }

    val qs = pqs.map { it.first }
    val ps = pqs.map { it.second }

    println("qs = [")
    qs.forEach { println("\t[${it.toList()}],") }
    println("]")
    println("ps = [")
    ps.forEach { println("\t[${it.toList()}],") }
    println("]")

}