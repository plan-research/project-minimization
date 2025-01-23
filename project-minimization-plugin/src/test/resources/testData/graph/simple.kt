open class A
class B : A() {
    fun f() {}
    val x = 5
    fun g() {
        f()
        println(x)
    }
}
