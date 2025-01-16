interface A {
    fun f()
}
interface B
interface C {
    fun f()
}

class D : A, B, C {
    override fun f() {
        println("f")
    }
}

class E : D() {
    override fun f() {
        data class GG(val x: A)
        fun g(): D {
            val y = "Aboba"
            return D()
        }
        val x: Int = 5
        println("f!!!!${g()}")
    }
}