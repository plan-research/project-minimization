fun f(x: Int, y: Int, lambda: () -> Unit) = x + y

fun g(x: Int, y: Int) {
    f(x = x, y = y) {}
    f(lambda = {}, y = x, x = y)
    f(y = y, x = x) {}
}