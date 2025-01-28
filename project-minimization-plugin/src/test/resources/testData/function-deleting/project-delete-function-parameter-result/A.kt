class A(
    val a: Int = 5,
) {
    fun method(y: Double) = Unit
    fun method2(z: Int) {
        method(5.0)
    }
}

fun main() {
    A()
    A(5)
}