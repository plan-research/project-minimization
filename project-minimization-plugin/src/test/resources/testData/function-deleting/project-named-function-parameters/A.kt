fun f(x: Int, y: Int, lambda: () -> Unit) = y

fun g(a: Int, b: Int) {
    f(x = a, y = b) {}
    f(lambda = {}, y = a, x = b)
    f(y = b, x = a) {}
}