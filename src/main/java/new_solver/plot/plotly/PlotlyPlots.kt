package new_solver.plot.plotly

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.Value
import space.kscience.dataforge.meta.set
import space.kscience.dataforge.names.Name
import space.kscience.plotly.*
import space.kscience.plotly.models.Bar
import space.kscience.plotly.models.ScatterMode
import space.kscience.plotly.models.TraceType
import space.kscience.plotly.models.color
import java.util.*

@OptIn(UnsupportedPlotlyAPI::class)
class PlotlyPlots(val skipPlots: Boolean = true) {

    fun scatter(xs: List<Number>, ys: List<Number>, arrow: Pair<Double, Double>? = null, name: String = "") {
        if (skipPlots) return
        val plot = Plotly.plot {
            scatter {
                x.set(xs)
                y.set(ys)
                mode = ScatterMode.markers
            }

            if (arrow != null) {
                trace {
                    x.set(listOf(0.0, arrow.first))
                    y.set(listOf(0.0, arrow.second))
                }
            }
            defaultLayout(name)
        }.makeFile()
    }

    fun plot3d(
        xs: List<Number?>, ys: List<Number?>,
        arrows: Iterable<Triple<Double, Double, Double>>? = null,
        name: String
    ) = Plotly.plot {
        if (skipPlots) return

        trace {
            type = TraceType.scatter3d
            x.set(xs)
            y.set(ys)
            z.set(List(xs.size) { it.toDouble() })
            line {
                width = 0.1
            }
        }
        arrows?.forEach { arrow ->
            trace {
                type = TraceType.scatter3d
                x.set(listOf(0.0, arrow.first))
                y.set(listOf(0.0, arrow.second))
                z.set(listOf(0.0, arrow.third))
                line {
                    width = 0.5
                }
            }
        }

        defaultLayout(name)
    }.makeFile()

    fun plot3d(
        xs: List<Number?>, ys: List<Number?>, zs: List<Number>?,
        arrows: List<Array<DoubleArray>>? = null,
        name: String
    ) = Plotly.plot {
        if (skipPlots) return

        trace {
            type = TraceType.scatter3d
            x.set(xs)
            y.set(ys)
            z.set(zs)
            line {
                width = 0.01
            }
        }
        if (arrows != null) {
            traces(
                arrows.map { arrow ->
                    trace {
                        type = TraceType.scatter3d
                        x.set(listOf(0.0, arrow[0][0], 0.0, arrow[1][0], 0.0, arrow[2][0]))
                        y.set(listOf(0.0, arrow[0][1], 0.0, arrow[1][1], 0.0, arrow[2][1]))
                        z.set(listOf(0.0, arrow[0][2], 0.0, arrow[1][2], 0.0, arrow[2][2]))

            //                    y.set(listOf(0.0, arrow.second))
            //                    z.set(listOf(0.0, arrow.third))
                        line {
                            width = 1
                        }
                    }
                }
            )
        }

        defaultLayout(name)
    }.makeFile()

    private fun Plot.defaultLayout(name: String) {
        layout {
            height = 750
            width = 750
            title = name
        }
    }

    fun plotIt(alpha: LinkedList<DoubleArray>, name: String) = Plotly.plot {
        if (skipPlots) return

        val res = Array<Triple<Double, Double, Double>?>(alpha.size * alpha.first.size) { null }

        var c = 0
        var cLine = 0
        for (i in 0 until alpha.size * alpha.first.size) {
            if (c == alpha.first.size) {
                cLine++
                c = 0
            }
            res[i] = Triple(cLine.toDouble(), c.toDouble(), alpha[cLine][c])
            c++
        }

        trace {
            type = TraceType.scatter3d
            x.set(res.map { it!!.first })
            y.set(res.map { it!!.second })
            z.set(res.map { it!!.third })
            line { width = 0.0 }
            marker { size = 2 }
        }
        defaultLayout(name)
    }.makeFile()

}