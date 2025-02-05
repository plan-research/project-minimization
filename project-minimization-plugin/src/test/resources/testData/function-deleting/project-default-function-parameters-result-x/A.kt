fun f(y: Double = 1.0, lambda: () -> Unit) {}

fun g() {
    f(1.0) {}
    f()
    f(lambda = {})
    f()
    f(1.23)
}