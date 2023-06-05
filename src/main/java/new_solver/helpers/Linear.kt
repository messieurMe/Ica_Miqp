package new_solver.helpers

import new_solver.helpers.clustering.direction_to_cluster_mapper.DirectionToClusterMapperWithCircle
import kotlin.math.*

fun <A, B> Iterable<A>.mapThis(block: A.() -> B): Iterable<B> = this.map { block(it) }

typealias Vector = Iterable<Double>

operator fun Vector.minus(that: Vector) = zip(that).mapThis { first - second }

operator fun DoubleArray.minus(that: DoubleArray) = zip(that).mapThis { first - second }

operator fun Vector.plus(that: Vector) = zip(that).mapThis { first + second }
operator fun DoubleArray.plus(that: DoubleArray) = zip(that).mapThis { first + second }


operator fun Vector.times(that: Vector) = zip(that).sumOf { (a, b) -> a * b }
operator fun DoubleArray.times(that: DoubleArray) = zip(that).sumOf { (a, b) -> a * b }

operator fun Vector.times(d: Double) = this.map { it * d }

operator fun Vector.div(d: Double) = this.map { it / d }
operator fun DoubleArray.div(d: Double) = this.map { it / d }.toDoubleArray() // TODO: Create new or change existed

fun l1(v: Vector) = v.sumOf(Double::absoluteValue)
fun l2(v: Vector) = sqrt(v * v)
fun l2(v: DoubleArray) = sqrt(v * v)

infix fun Vector.angle(that: Vector): Double = acos((this * that) / (l2(this) * l2(that))).let {
    if (it.isNaN()) {
        if ((this / l2(this)).zip((that / l2(that))).all { (a, b) -> (((a - b).absoluteValue)) < 1e-1 })
            0.0
        else PI
    } else {
        it
    }
}

infix fun DoubleArray.angle(that: DoubleArray): Double = acos((this * that) / (l2(this) * l2(that))).let {
    if (it.isNaN()) {
        if ((this / l2(this)).zip((that / l2(that))).all { (a, b) -> (((a - b).absoluteValue)) < 1e-1 })
            0.0
        else PI
    } else {
        it
    }

}

infix fun Vector.angleDeg(that: Vector): Double = Math.toDegrees(this angle that)
infix fun DoubleArray.angleDeg(that: DoubleArray): Double = Math.toDegrees(this angle that)

operator fun Vector.unaryMinus() = map { -it }

fun main() {

    val a = listOf(1.0, 1.0, 1.0, 2.34, -123.1, 33.33)


//    val cs = DirectionToClusterMapperWithCircle.findClusterEndpoints(arrayOf(a.toDoubleArray()), 1.0)[0]
//    cs.forEach { cA ->
//        val c = cA.toList()
//        println("===DELIMITER===")
//        val b = (a + (-c)) * -1.0
//        println("len a = " + l2(a))
//        println("len b = " + l2(b))
//        println("len c = " + l2(c))
//        println("check sum = " + (l2(a).pow(2) + l2(b).pow(2)) + "=" + l2(c).pow(2))
//        println("a angle b = " + Math.toDegrees(a angle b))
//        println("a angle c = " + Math.toDegrees(a angle c))
//        println("b angle c = " + Math.toDegrees(b angle c))
//        println("check sum = " + (Math.toDegrees(a angle b) + Math.toDegrees(a angle c) + Math.toDegrees(b angle c)))
//    }
}
