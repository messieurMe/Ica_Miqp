package new_solver

import config.Consts
import config.Flags
import di.MainModule
import legacy.analysis.DataAnalysis.whitening
import legacy.graph.Graph
import legacy.io.NewMatrixIO
import new_solver.common.FilePicker
import new_solver.common.computeIf
import java.io.PrintWriter
import java.util.LinkedList

fun main() {
    val namingMap: Map<String, Int> = HashMap()
    val revNamingMap: Map<Int, String> = HashMap()



    val data = FilePicker.SIMPLE_SANDBOX_MATRIX

    var matrix = NewMatrixIO.read(data.matrix, true, namingMap, revNamingMap)
    computeIf(Consts.WHITENING) { matrix = whitening(matrix) }
    val graph = Graph(emptyMap(), emptyList(), emptyList())
    val mainModule = MainModule(Flags())

    val lastQTs = LinkedList<Pair<List<Double>, List<Double>>>()

    for (i in 0 until 25) {
        val solver = mainModule.nDimClusterConnectedSolver2.apply {
            addInput(matrix, graph)
        }
        solver.solve()
        solver.runCatching {
            PrintWriter("./statistics/r_$i").use { pw ->
                with(pw) {
                    println(
                        """
                        Obj:
                        ${cplex.objValue}
                        Dir:
                        ${cplex.getValues(v.a).toList()}
                    """.trimIndent()
                    )

                    val variables = listOf(
                        v.a to "A",
                        v.f to "F",
                        v.g to "G",
//                        v.q to "Q",
//                        v.t to "T",
//                        v.r to "R",
//                        v.s to "S",
//                        v.x to "X",
//                        v.y to "Y"
                    ).map { (cplex.getValues(it.first) to it.second) }

                    variables.forEach { (data, title) ->
                        pw.println(title)
                        pw.println(data.toList())
                    }
                    lastQTs.addLast(Pair(variables[1].first.toList(), variables[2].first.toList()))
//                    lastQTs.addLast(Pair(variables[3].first.toList(), variables[4].first.toList()))
                }
            }
            lastQTs.forEach {
                println("=======")
                println(it.first)
                println(it.second)
            }
        }
    }
}
