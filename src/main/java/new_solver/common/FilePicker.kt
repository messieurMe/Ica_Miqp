package new_solver.common

import java.util.*

enum class FilePicker(val path: String, val title: String? = null) {
    A2_TEST_05(path = "input"),
    SIMPLE_SANDBOX_MATRIX(path = "input"),
    REAL_TEST_05(path = "real_data"),
    REAL_TEST_NEW(path = "real_data"),
    TEST_SMALL_1(path = "in_data", title = "1_test_small_025");


    @Deprecated("Use method which represents extension of file")
    operator fun invoke(): String {
        return "./$path/${this.name.lowercase()}.mtx"
    }

    private val realTitle: String
        get() = title ?: name.lowercase()

    val matrix: String
        get() = "./$path/$realTitle.mtx"

    val graph: String
        get() = "./$path/$realTitle.graph"

    val answers: String
        get() = "./$path/$realTitle.ans"
}