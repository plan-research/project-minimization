interface I {
    fun overridden(): Unit
}
class A: I {
    fun overridden() = Unit
}
class C(): I {
    fun simpleMethod() {
        TODO("Removed by DD") as Unit
    }

    fun simpleMethod2(x: Int, y: Int): Int {
        TODO("Removed by DD")
    }

    fun simpleMethod3(a: Int, b: Int): Int = a + b

    override fun overridden() {
        TODO("Removed by DD") as Unit
    }

    fun complexMethod() {
        TODO("Removed by DD") as Unit
    }
}