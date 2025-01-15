fun f(ff: (Int) -> () -> Unit): () -> Unit = ff(239)
fun gg(): () -> Unit = { println("Hello, world!") }
val x = f(::gg)