class A(
    val a: Int = 5,
    val b: () -> Unit,
) {
    fun method(x: Int, y: Double) = Unit
    fun method2(z: Int) {
        method(z, 5.0)
    }
}

fun main() {
    A() { println() }
    A(5) { Unit }
}