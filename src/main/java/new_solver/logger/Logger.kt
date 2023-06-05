package new_solver.logger

import config.Consts.SKIP_LOGS
import java.util.LinkedList

class Logger private constructor() {
    private val placeSize = 15
    private var currentTabs = 0
    private val spacedTabs = "    "


    val messages = LinkedList<Pair<String, String>>()

    fun d(message: String) = d("unknown", message)

    fun d(logPlace: String, message: Any) {
        messages.add(spacedTabs.repeat(currentTabs) + logPlace to message.toString())
    }

    fun <T> nest(title: String, body: Logger.() -> T): T {
        messages.add(("" to "\r\t" + spacedTabs.repeat(currentTabs) + title))
        currentTabs++
        return this.body().also { currentTabs-- }
    }

    private fun printTabs() {
        print("\t")
    }

    private fun printMessages() {
        val maxLogPlaceSize = messages.maxOf { it.first.length }
        messages.forEachIndexed { i, (logPlace, message) ->
            if (i != 0) {
                println()
            }
            val spaces = " ".repeat(maxLogPlaceSize - logPlace.length)
            printTabs()
            print("$logPlace$spaces : $message")

        }
        println()
    }

    companion object {

        private fun before(blockName: String) {
            val decorator = ">".repeat(5)
            println()
            println(
                """
                ${decorator}START${decorator}
                ${decorator}$blockName
            """.trimIndent()
            )
        }

        private fun after(blockName: String) {
            val decorator = "<".repeat(5)
            println(
                """
                ${decorator}$blockName
                ${decorator}END${decorator}
            """.trimIndent()
            )
            println()
        }

        fun <T> withLogger(blockName: String, skipIfEmpty: Boolean = false, block: Logger.() -> T): T {
            val logger = Logger()
            val r = block(logger)
            if (logger.messages.isNotEmpty() && !SKIP_LOGS) {
                before(blockName)
                logger.printMessages()
                after(blockName)
            }
            return r
        }

        fun <T> withLogger(vararg blockName: String, block: Logger.() -> T): T {
            val logger = Logger()
            val r = block(logger)
            if (logger.messages.isNotEmpty() && !SKIP_LOGS) {
                before(blockName.joinToString(separator = "#"))
                logger.printMessages()
                after(blockName.joinToString(separator = "#"))
            }
            return r
        }

    }
}
