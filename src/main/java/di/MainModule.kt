package di

import config.Flags
import config.Flags.DirectionProviderType.*
import config.Flags.DirectionToClusterMapperType.*
import new_solver.generator.DataGenerator
import new_solver.helpers.clustering.directions_provider.BrutForceClusterDirectionsProvider
import new_solver.helpers.clustering.directions_provider.ClusterDirectionsProvider
import new_solver.helpers.clustering.directions_provider.KMeansClusterDirectionsProvider
import new_solver.helpers.BasisTransformer
import new_solver.helpers.BasisTransformerImpl
import new_solver.helpers.clustering.direction_to_cluster_mapper.DirectionToClusterMapperWithCircle
import new_solver.helpers.clustering.direction_to_cluster_mapper.DirectionToClusterMapperWithGivensRotations
import new_solver.helpers.clustering.direction_to_cluster_mapper.DirectionsToClustersMapper
import new_solver.helpers.clustering.directions_provider.SinglePointClusterDirectionProvider
import new_solver.helpers.solution_adapter.NDimClusterConnectedSolutionAdapter
import new_solver.helpers.solution_adapter.NDimClusterConnectedSolutionAdapterImpl
import new_solver.plot.plotly.PlotlyPlots
import new_solver.solvers.NDimClusterConnectedSolver2

interface MainModule {

    val nDimClusterConnectedSolver2: NDimClusterConnectedSolver2
    val dataGenerator: DataGenerator
    val plotlyPlots: PlotlyPlots
}

fun MainModule(flags: Flags): MainModule {


    return object : MainModule {

        val clusterDirectionsProvider: ClusterDirectionsProvider
            get() = when (flags.directionProviderType) {
                KMEANS -> KMeansClusterDirectionsProvider(
                    flags.KMeansK,
                    flags.KMeansMaxIter,
                    flags.KMeansTolerance,
                    flags.minAngleBetweenDirections,
                    flags.clusteringFilterCloseClusters
                )

                BRUT_FORCE -> BrutForceClusterDirectionsProvider()
                SINGLE_POINT -> SinglePointClusterDirectionProvider()
            }

        val basisTransformer: BasisTransformer
            get() = BasisTransformerImpl()

        val directionToClusterMapper: DirectionsToClustersMapper
            get() = when (flags.directionToClusterMapperType) {
                CIRCLE -> DirectionToClusterMapperWithCircle(flags)
                GIVENS_ROTATIONS -> DirectionToClusterMapperWithGivensRotations(flags.clusteringAngle)
            }

        val nDimClusterConnectedSolutionAdapter: NDimClusterConnectedSolutionAdapter
            get() = NDimClusterConnectedSolutionAdapterImpl()


        override val nDimClusterConnectedSolver2: NDimClusterConnectedSolver2
            get() = NDimClusterConnectedSolver2(
                flags.maxClusters,
                flags.useMipStart,
                flags.clusteringMinClusterSize,
                flags.clusteringAddPriorities,
                nDimClusterConnectedSolutionAdapter,
                clusterDirectionsProvider,
                directionToClusterMapper,
            )

        override val dataGenerator: DataGenerator
            get() = DataGenerator(
                flags.dataGeneratorStep,
                flags.dataGeneratorSmallNoiseBorder,
                flags.dataGeneratorMediumNoiseBorder,
                flags.dataGeneratorBigNoise,
            )

        override val plotlyPlots: PlotlyPlots = PlotlyPlots(flags.skipPlots)
    }
}