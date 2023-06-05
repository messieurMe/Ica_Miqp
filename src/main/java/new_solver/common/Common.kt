package new_solver.common

fun computeIf(predicate: Boolean, block: () -> Unit) {
    if (predicate) {
        block()
    }
}

fun String.toVarName(i: Int) = "$this$i"
fun String.toVarName(vararg args: Int) = args.fold(this) { acc, i -> "$acc+$i" }

fun Array<*>.printArray(title: String? = null) {
    title?.let { print("$it:") }
    forEach { print("$it ") }
    println()
}