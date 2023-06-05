package new_solver

import config.Consts
import ext.column
import legacy.analysis.Normalisation
import legacy.analysis.Whitening
import legacy.io.NewMatrixIO
import config.Consts.COMPONENTS
import config.Consts.DUMMY_DATA_N
import config.Consts.TL
import config.Consts.INF
import config.Consts.INITIAL_D
import config.Consts.MIP_START_LENGTH
import config.Consts.ONE_SIGN_CLUSTER_SIZE
import config.Flags
import new_solver.common.FilePicker.*
import new_solver.common.computeIf
import new_solver.generator.DataGenerator
import new_solver.optimisations.mip_start.MipStart
import new_solver.plot.plotly.PlotlyPlots
import new_solver.solvers.*
import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.linear.QRDecomposition
import kotlin.random.Random

// flags

const val IN = "./input/"

@Deprecated("Use RealMain, this one is outdated")
fun main(args: Array<String>) {
    // init
    val namingMap: Map<String, Int> = HashMap()
    val revNamingMap: Map<Int, String> = HashMap()
    val listList = ArrayList<String>()

    val flags = Flags()

    if (Consts.GENERATE_DUMMY_DATA) {
        DataGenerator(
            flags.dataGeneratorStep,
            flags.dataGeneratorSmallNoiseBorder,
            flags.dataGeneratorMediumNoiseBorder,
            flags.dataGeneratorBigNoise
        ).simpleGenerator(DUMMY_DATA_N, INITIAL_D, COMPONENTS)
    }

    // read matrix
    var matrix = NewMatrixIO.read(SIMPLE_SANDBOX_MATRIX(), true, namingMap, revNamingMap)


    var counter = 0
    val oneSigned = ArrayList<Int>()

    println(counter)
    println("One sign: ${oneSigned.toList()}")

    val plotlyPlots = PlotlyPlots(true)

    plotlyPlots.scatter(
        matrix.column(0).toList(), matrix.column(1).toList(), name = "Initial data"
    )
    println(
        """
        Matrix dim
           d=${matrix.numCols()}
           n=${matrix.numRows()}
        """.trimMargin()
    )

    // preparing
    println(matrix.entry.getEntry(0, 0))

    computeIf(Consts.NORMALIZATION) {
        matrix = Normalisation(matrix)
        plotlyPlots.scatter(
            matrix.column(0).toList(), matrix.column(1).toList(), name = "Normalisation"
        )
    }

    computeIf(Consts.WHITENING) {
        matrix = Whitening(matrix)
        plotlyPlots.scatter(
            matrix.column(0).toList(), matrix.column(1).toList(), name = "Whitening"
        )
    }

    // Another normalization?
    // Maybe it increases speed cause each element is between -1 & 1
    // First normalisation is to improve whitening

    while (matrix.numRows() > 1) {
        println("=".repeat(10))

        // solver
        println("Matrix dim\n\td=${matrix.numCols()}\n\tn=${matrix.numRows()}")

        val directionIndex = if (Consts.RANDOM_MIP_START_DIRECTION) Random.nextInt(0, matrix.numCols()) else 1

        val mipStartAValue = DoubleArray(matrix.numCols()).apply {
            set(directionIndex, MIP_START_LENGTH)
            println("MipStart direction: $directionIndex")
        }
        val (pStart, aStart) = MipStart().getAP(matrix, directionIndex)
        mipStartAValue[directionIndex] = aStart
//        val mipStartPValue = pStart
        val mipStartPValue = DoubleArray(matrix.numRows()).apply {
            val icaDirectionSize = (DUMMY_DATA_N / COMPONENTS)
            if (icaDirectionSize >= size) {
                println("Expected size $size, but given $icaDirectionSize")
            }
            val start = directionIndex * icaDirectionSize
            val end = start + icaDirectionSize
            println("MipStart elements is from $start until $end")
            val pStart = matrix.numRows().toDouble() / icaDirectionSize
        }

        val clusters = 8
        val part = DUMMY_DATA_N / clusters
        val clusterSize = part
        val clusters2d = listOf(
            0,
//            DUMMY_DATA_N / 2 - ONE_SIGN_CLUSTER_SIZE,
            DUMMY_DATA_N / 2,
//            DUMMY_DATA_N - ONE_SIGN_CLUSTER_SIZE
        )
            .map { i -> IntArray(ONE_SIGN_CLUSTER_SIZE) { j -> i + j } }

        if (Consts.PLOT_CLUSTERS) {
            clusters2d.forEachIndexed { ind, cluster ->
                plotlyPlots.scatter(
                    cluster.map { i -> matrix.getElem(i, 0) },
                    cluster.map { i -> matrix.getElem(i, 1) },
                    name = "Cluster $ind"
                )
            }
        }

        val nDimClustersAL = ArrayList<DoubleArray>()
        for (i in 0 until INITIAL_D * 2) {
            val dirI = Random.nextInt(INITIAL_D)
            val dir = DoubleArray(INITIAL_D) { Random.nextDouble(-0.5, 0.5) }
            dir[dirI] = Random.nextDouble(2.0, 5.0) * if (Random.nextBoolean()) 1.0 else -1.0
            nDimClustersAL.add(dir)
        }
        val nDimClusters = nDimClustersAL.toTypedArray()

        @Suppress("SimplifyWhenWithBooleanConstantCondition")
        val solver: Solver = when {
            false -> SimpleSolver(matrix,
                TL,
                INF,
                mipStartAValue.takeIf { Consts.MIP_START_FOR_A },
                mipStartPValue.takeIf { Consts.MIP_START_FOR_P })

            false -> OpenedModuleSolver(matrix,
                TL,
                INF,
                mipStartAValue.takeIf { Consts.MIP_START_FOR_A },
                mipStartPValue.takeIf { Consts.MIP_START_FOR_P })

            false -> SignedClustersSolver(
                matrix,
                TL,
                INF,
                clusters2d
            )

            false -> CustomClusterSolver(matrix, TL, INF)
            else -> error("Solver not selected")
        }

        val isSolved = solver.solve()
        println("Solved: $isSolved")
        if (isSolved) {
            solver.writeResult()

            plotlyPlots.scatter(
                matrix.column(0).toList(),
                matrix.column(1).toList(),
                solver.icaDirection().let { Pair(it[0], it[1]) },
                name = "Result"
            )
            if (Consts.PLOT_LOGS) {
//                with(solver) {
//                    runCatching {
//                        plotlyPlots.plot3d(a.map { it.first }, a.map { it.second }, name = "Path: A")
//                    }
//                    plotlyPlots.plot3d(
//                        callbackId.toList(), callbackId.toList(), name = "CallbackIds"
//                    )
//                    plotlyPlots.plotIt(alpha, name = "Alpha")
//                }
            }
        } else {
            plotlyPlots.scatter(
                matrix.column(0).toList(), matrix.column(1).toList(), name = "No solutions"
            )
        }

        // decrease dim
        println("Press F to decrease dimension").also { readLine() }

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
}
