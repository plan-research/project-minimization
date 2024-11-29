val x: (Int) -> Unit = {a: Int -> println("Wow, how queer a lambda in my lambda.kt file?")}

fun letsReturnAFun(): (Int) -> Unit {
    return fun(x: Int) {
        println("Wow, such a good place for fun keyword")
    }
}

