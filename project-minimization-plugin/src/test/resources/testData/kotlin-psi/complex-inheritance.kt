interface A {
    fun f(): Unit
}
interface B {
    fun g(): Unit
}
interface C : A, B {
    override fun f() = Unit
    override fun g() = Unit
}
class D : C {
    override fun g() = Unit
    override fun f() = Unit
}