package config

import kotlin.math.tan

class Flags(
    // region: Common

    val skipPlots: Boolean = true,
    val skipLogs: Boolean = false,
    // endregionww

    // region: clustering

    val maxClusters: Int = 15,
    val clusteringAngle: Double = 60.0,
    val clusteringRadiusRange: Double = 0.1,
    val clusteringMinClusterSize: Int = 200,
    val minAngleBetweenDirections: Double = 45.0,
    val clusteringAddPriorities: Boolean = false,
    val clusteringFilterCloseClusters: Boolean = false,
    @Deprecated("Consider changing to method with givens rotations")
    val clusteringRadius: Double = tan(Math.toRadians(87.0)),

    val directionProviderType: DirectionProviderType = DirectionProviderType.KMEANS,
    // endregion

    // region: KMeans

    val KMeansK: Int = 50,
    val KMeansTolerance: Double = 1e-4,
    val KMeansMaxIter: Int = Int.MAX_VALUE / 2,
    // endregion

    // region: Callback

    val callbackMinAngle: Double = 45.0,
    // endregion

    // region: solver

    val useMipStart: Boolean = true,
    val solverType: SolverType = SolverType.NDIM_CLUSTER_CONNECTED,
    val directionToClusterMapperType: DirectionToClusterMapperType = DirectionToClusterMapperType.GIVENS_ROTATIONS,
    // endregion

    // region: DataGenerator

    val dummyData: Boolean = true,
    val dataGeneratorStep: Double = 0.5,
    val dataGeneratorSmallNoiseBorder: Double = 0.05,
    val dataGeneratorMediumNoiseBorder: Double = 0.5,
    val dataGeneratorBigNoise: Double = 0.95,
    // endregion
) {

    enum class DirectionProviderType {
        BRUT_FORCE,
        SINGLE_POINT,
        KMEANS
    }

    enum class DirectionToClusterMapperType {
        CIRCLE,
        GIVENS_ROTATIONS
    }

    enum class SolverType {
        NDIM_CLUSTER_CONNECTED
    }
}