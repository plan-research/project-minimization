val x: (Int) -> Unit = {a: Int -> TODO("Removed by DD") }
val y: (Int) -> Unit = {a: Int ->
    TODO("Removed by DD")
}

fun letsReturnAFun(): (Int) -> Unit {
    return fun(x: Int) {
        println("Wow, such a good place for fun keyword")
    }
}

