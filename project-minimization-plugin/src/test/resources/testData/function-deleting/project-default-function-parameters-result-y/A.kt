fun f(x: Int = 1, lambda: () -> Unit) {}

fun g() {
    f(1) {}
    f()
    f(lambda = {})
    f(123)
    f(1)
}