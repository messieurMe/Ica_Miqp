package new_solver.generator

import new_solver.common.FilePicker.*
import java.io.PrintWriter
import java.text.DecimalFormat
import java.util.concurrent.ArrayBlockingQueue
import kotlin.math.*
import kotlin.random.Random


class DataGenerator(
    val dataGeneratorStep: Double,
    val dataGeneratorSmallNoiseBorder: Double,
    val dataGeneratorMediumNoiseBorder: Double,
    val dataGeneratorBigNoise: Double,
) {
    private val df = DecimalFormat("+0.00000;-0.00000")

    val noise = Random

    private fun smallNoise(): Double {
        return noise.nextDouble(-dataGeneratorSmallNoiseBorder, dataGeneratorSmallNoiseBorder)
    }

    private fun mediumNoise(): Double {
        return noise.nextDouble(-dataGeneratorMediumNoiseBorder, dataGeneratorMediumNoiseBorder)
    }

    private fun bigNoise(): Double {
        return noise.nextDouble(-dataGeneratorBigNoise, dataGeneratorBigNoise)
    }

    private fun getGlobalStart(blockSize: Int) = -((dataGeneratorStep * blockSize) / 2)

    fun simpleGenerator(n: Int, d: Int, components: Int) {
        val blockSize = n / components
        val rest = n % components
        var ind = 0

        val globalStart = getGlobalStart(blockSize)
        PrintWriter(SIMPLE_SANDBOX_MATRIX.matrix).use { out ->
            for (component in 0 until components) {
                var start = globalStart
                for (block in 0 until blockSize) {
                    out.print("UN($component+$block) ")

                    for (i in 0 until component) {
                        out.print(df.format(smallNoise()) + " ")
                    }
                    out.print(df.format(start + mediumNoise()) + " ")
                    for (i in component + 1 until d) {
                        out.print(df.format(smallNoise()) + " ")
                    }
                    out.println()
                    start += dataGeneratorStep
                    ind++
                }
            }
            for (i in 0 until rest) {
                out.print("UN(EX+$i) ")
                for (j in 0 until d) {
                    out.print(df.format(bigNoise()) + " ")
                }
                out.println()
            }
        }
    }

    fun normalGenerator(n: Int, d: Int, components: Int) {
        val blockSize = n / components
        val globalStart = getGlobalStart(blockSize)
        val inz = 1.0 / (1.0 / ((blockSize / 5.0) * sqrt(2.0 * Math.PI)))

        PrintWriter(SIMPLE_SANDBOX_MATRIX()).use { out ->
            for (component in 0 until components) {
                var start = globalStart
                for (block in 0 until blockSize) {
                    out.print("UN($component+$block) ")
                    val noise = (1.0 / ((blockSize / 5.0) * sqrt(2.0 * Math.PI))) * Math.E.pow(
                        -(start).pow(2) / (2.0 * (blockSize / 5.0).pow(2))
                    ) * inz
                    for (i in 0 until component) {
                        out.print(df.format(smallNoise() * noise) + " ")
                    }
                    out.print(df.format(start + mediumNoise()) + " ")
                    for (i in component + 1 until d) {
                        out.print(df.format(smallNoise() * noise) + " ")
                    }
                    out.println()
                    start += dataGeneratorStep
                }
            }
        }
    }

    fun noisyGenerator(n: Int, d: Int, components: Int) {
        val blockSize = n / components

        val globalStart = getGlobalStart(blockSize)
        PrintWriter(SIMPLE_SANDBOX_MATRIX()).use { out ->
            for (component in 0 until components) {
                var start = globalStart
                for (block in 0 until blockSize) {
                    out.print("UN($component+$block) ")
                    for (i in 0 until component) {
                        out.print(df.format(smallNoise() * (i + 1)) + " ")
                    }
                    out.print(df.format(start + mediumNoise() * (globalStart + component + 1)) + " ")
                    for (i in component + 1 until d) {
                        out.print(df.format(smallNoise() * (i + 1)) + " ")
                    }
                    out.println()
                    start += dataGeneratorStep
                }
            }
        }
    }

    private fun write(block: (PrintWriter) -> Unit) {
        PrintWriter(SIMPLE_SANDBOX_MATRIX()).use { block(it) }
    }
}