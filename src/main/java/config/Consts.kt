package config

object Consts {
    const val EPS = 1e-6
    const val TL = 30

    const val INF = 500_000.0
    const val STEP = 0.001
    var DUMMY_DATA_N = 50
    const val INITIAL_D = 4
    const val ONE_SIGN_CLUSTER_SIZE = 80
    const val COMPONENTS = INITIAL_D
    const val MIP_START_LENGTH = 5.0
    const val SKIP_LOGS = false

    const val GENERATE_DUMMY_DATA = true
    const val MIP_START_ALL = false
    const val MIP_START_FOR_A = MIP_START_ALL
    const val MIP_START_FOR_P = MIP_START_ALL
    const val NORMALIZATION = false
    const val PLOT_LOGS = false
    const val PLOT_CLUSTERS = false
    const val WHITENING = true
    const val RANDOM_MIP_START_DIRECTION = false

}