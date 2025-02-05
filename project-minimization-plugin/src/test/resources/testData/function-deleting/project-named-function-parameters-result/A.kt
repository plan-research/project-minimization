fun f(x: Int, lambda: () -> Unit) = y

fun g(a: Int, b: Int) {
    f(x = a) {}
    f(lambda = {}, x = b)
    f(x = a) {}
}