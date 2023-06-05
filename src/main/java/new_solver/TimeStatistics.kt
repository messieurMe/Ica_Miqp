package new_solver

import config.Consts
import config.Flags
import di.MainModule
import legacy.analysis.DataAnalysis.whitening
import legacy.graph.Graph
import legacy.io.NewMatrixIO
import new_solver.common.FilePicker
import new_solver.common.computeIf
import new_solver.solvers.*
import java.lang.Integer.max
import java.util.LinkedList

fun main() {
    val module = MainModule(Flags())

    val data = FilePicker.SIMPLE_SANDBOX_MATRIX


    val timeResults = Array<LinkedList<Long>>(10) { LinkedList() }

    for (d in 2 until 8) {
        for (n in max(5, d) until 25 step 1) {
            val namingMap: Map<String, Int> = HashMap()
            val revNamingMap: Map<Int, String> = HashMap()

            if (Consts.GENERATE_DUMMY_DATA) {
                module.dataGenerator.simpleGenerator(n, d, d)
            }

            var matrix = NewMatrixIO.read(data.matrix, true, namingMap, revNamingMap)
            println(matrix.numRows().toString() + "x" + matrix.numCols())

            computeIf(Consts.WHITENING) { matrix = whitening(matrix) }
            val graph = Graph(emptyMap(), emptyList(), emptyList())

            val solver: NDimClusterConnectedSolver2 =
                module.nDimClusterConnectedSolver2.apply { addInput(matrix, graph) }

            val start = System.currentTimeMillis()
            solver.solve()
            val end = System.currentTimeMillis()

            timeResults[d].addLast(end - start)


            println("timeResults = ")
            timeResults.forEach {
                print("[")
                it.forEach { print("$it, ") }
                println("],")
            }
        }
    }

    timeResults.forEach {
        it.forEach { print("$it, ") }
        println()
    }

}