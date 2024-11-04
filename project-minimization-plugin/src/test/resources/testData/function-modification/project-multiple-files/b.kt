val lambda = { x: Int, b: Int, c: String, d: Double -> println("This should be replaced") }
val lambda2 = fun(x: Int) {
    println(x); println("This should be replaced");
    val y = x + 5
}