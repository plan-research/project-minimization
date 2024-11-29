val x: (Int) -> Unit = {a: Int -> TODO("Removed by DD") as Unit }
val y: (Int) -> Unit = {a: Int ->
    TODO("Removed by DD") as Unit
}

fun letsReturnAFun(): (Int) -> Unit {
    return fun(x: Int) {
        println("Wow, such a good place for fun keyword")
    }
}

