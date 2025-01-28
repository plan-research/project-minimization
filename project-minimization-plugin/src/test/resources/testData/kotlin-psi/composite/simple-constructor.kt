class A(val x: Int)

val x = A(1)

fun f(): A {
    return A(1)
}
fun g() = A(1)

fun h() {
    fun g() {
        fun h() {
            println(A(1))
        }
        h()
    }
    g()
}