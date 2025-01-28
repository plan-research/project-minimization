interface I {
    val a: String
    fun kek(): Unit
}

abstract class A : I {
    override val a: String = "Hello, World!"
}

class B : A() {
    override val a: String = "Hello, World!2"
    override fun kek(): Unit = println("Hello, World!")
}