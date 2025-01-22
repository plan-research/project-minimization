abstract class A {
    abstract fun foo()
}

class B : A() {
    override fun foo() = println("Hello, World!")
}