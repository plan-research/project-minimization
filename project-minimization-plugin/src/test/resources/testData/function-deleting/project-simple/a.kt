fun f() {
    println("Hello World!")
}

fun g() {
    fun f() {
        println("x")
    }
    f()
    f()
    f()
}

class C {
    fun f() = Unit
}

val x: String = "Yay, a global variable!"