interface I {
    val a: String
    fun kek(): Unit
}

abstract class A : I

class B : A() {
    override val a: String = "Hello, World!"
    override fun kek(): Unit = println("Hello, World!")
}