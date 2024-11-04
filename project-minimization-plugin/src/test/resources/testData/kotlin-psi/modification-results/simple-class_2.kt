interface I {
    fun overridden(): Unit
}
class A: I {
    fun overridden() = TODO("Removed by DD")
}
class C(): I {
    fun simpleMethod() {
        println("566")
    }

    fun simpleMethod2(x: Int, y: Int): Int {
        return x + y
    }

    fun simpleMethod3(a: Int, b: Int): Int = TODO("Removed by DD")

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