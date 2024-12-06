interface A {
    val x: Int
    fun method(a: Int, b: String) = Unit
    fun methodWithoutBody(): String
}

abstract class B : A {
    override val x: Int
        get() = 10
    override fun method(a: Int, b: String) = Unit
}

class C : B() {
    override fun methodWithoutBody() = "Overriden"
}

class D(override val x) : A {
    override fun methodWithoutBody() = "$x"
}