fun f(x: Int) = 566
fun g(a: String): String {
    f(566)
    g(a.drop(1))
    println("HEEEEEY")
}