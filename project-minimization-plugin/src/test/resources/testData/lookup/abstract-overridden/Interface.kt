interface I {
    val x: Int
    fun foo()
}

class A : I {
    override val x: Int = 1
    override fun foo() {}
}