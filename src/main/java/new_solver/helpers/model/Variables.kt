package new_solver.helpers.model

import ilog.concert.IloNumVar

//sealed interface VariableType {}

sealed interface Variables<T> {

    fun allVariables(): List<T>
}

//interface AllVariables<T : VariableType> {
//    fun allVariables(): List<T>
//}


//interface ConnectedVariables<T> : Variables<T> {
//    val b: Array<T>
//    val c: Array<T>
//    override fun allVariables(): List<T> {
//        return buildList {
//            addAll(b)
//            addAll(c)
//        }
//    }
//}

//fun ActualConnectedVariables() = object : ConnectedVariables<Double> {
//    override val b: Array<Double>
//        get() = TODO("Not yet implemented")
//    override val c: Array<Double>
//        get() = TODO("Not yet implemented")
//}
//fun BasicConnectedVariables(): BasicVariables<Double> {
//    return object :
//        BasicVariables<Double> by ActualBasicVariables(0, 0),
//        ConnectedVariables<Double> by ActualConnectedVariables() {
//        override fun allVariables(): List<Double> {
//            return super<BasicVariables>.allVariables() +
//                    super<ConnectedVariables>.allVariables()
//        }
//    }
//}
//fun lol() {
//    val x = BasicConnectedVariables()
//}