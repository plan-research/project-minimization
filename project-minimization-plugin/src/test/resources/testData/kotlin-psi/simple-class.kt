interface I {
    fun overridden(): Unit
}
class A: I {
    fun overridden() = Unit
}
class C(): I {
    fun simpleMethod() {
        println("566")
    }
    fun simpleMethod2(x: Int, y: Int): Int {
        return x + y
    }
    fun simpleMethod3(x: Int, y: Int): Int = x + y
    override fun overridden() {
        println("hi from overridden")
    }
    fun complexMethod() {
        fun doNotParseThis() {
            fun andThisAsWell() {
                TODO()
            }
        }
    }
}