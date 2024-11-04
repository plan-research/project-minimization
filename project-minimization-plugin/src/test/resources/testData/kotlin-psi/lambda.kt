val x: (Int) -> Unit = {a: Int -> println("Wow, how queer a lambda in my lambda.kt file?")}
val y: (Int) -> Unit = {a: Int ->
    fun lol(x: Int) {
        println("Excuse me. What is the function do in my top level lambda?")
    }
    println("Hmm I think we are making lambdas now")
}

fun letsReturnAFun(): (Int) -> Unit {
    return fun(x: Int) {
        println("Wow, such a good place for fun keyword")
    }
}

