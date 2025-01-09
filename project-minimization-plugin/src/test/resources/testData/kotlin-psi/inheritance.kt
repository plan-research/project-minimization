interface A {
    fun f(): Unit
}
open class B : A {
    override fun f() = Unit
}
open class C : B
class D : C