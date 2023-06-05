package new_solver.solvers.model

import java.util.*
import kotlin.collections.ArrayList

class Logs(
    val callbackId: LinkedList<Long> = LinkedList(),
    val a: ArrayList<Pair<Double?, Double?>> = ArrayList(),
    val f: LinkedList<Double> = LinkedList(),
    val g: LinkedList<Double> = LinkedList(),
    val alpha: LinkedList<DoubleArray> = LinkedList(),
    val beta: LinkedList<Double> = LinkedList()
)
