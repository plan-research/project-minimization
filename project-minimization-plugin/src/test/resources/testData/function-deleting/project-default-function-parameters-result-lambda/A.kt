fun f(x: Int = 1, y: Double = 1.0) {}

fun g() {
    f(1, 1.0)
    f()
    f()
    f(123)
    f(1, 1.23)
}