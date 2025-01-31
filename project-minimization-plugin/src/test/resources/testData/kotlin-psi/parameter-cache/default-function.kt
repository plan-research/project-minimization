fun f(x: Int = 1, y: Double = 1.0, lambda: () -> Unit) {}

fun g() {
    f(1, 1.0) {}
    f()
    f(lambda = {})
    f(123)
    f(1, 1.23)
}