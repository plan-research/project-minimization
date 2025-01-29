fun f(x: Int, y: String, lambda: () -> Unit) {
    lambda()
    println(x)
    println(y)
}
fun h(a: Int, b: String, c: () -> Unit) {
    lambda()
    println(x)
    println(y)
}

fun g() {
    f(2, "3", { Unit })
    h(2, "3") { Unit }
}