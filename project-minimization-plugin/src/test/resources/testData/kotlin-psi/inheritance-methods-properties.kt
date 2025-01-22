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

class D(y: Int) : A {
    override val x = y // TODO: Fix constuctor parameters
    override fun methodWithoutBody() = "$x"
}