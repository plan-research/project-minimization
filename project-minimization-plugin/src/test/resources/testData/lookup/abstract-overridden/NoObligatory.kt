open class A {
    fun foo() = println("Hello, World!")
    open fun bar() = println ("Hey! I'm still not overridden")
}
class B : A() {
    val x: Int
}