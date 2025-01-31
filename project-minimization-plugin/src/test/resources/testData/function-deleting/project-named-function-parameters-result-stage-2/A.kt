fun f(lambda: () -> Unit) = y

fun g(a: Int, b: Int) {
    f() {}
    f(lambda = {})
    f() {}
}