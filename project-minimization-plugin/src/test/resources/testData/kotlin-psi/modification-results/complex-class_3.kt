class C {
    fun method() {
        while (true);
    }

    fun method2() = println("Aboba")
    var lambda = { x: Int -> println(x) }
    val x: Int

    init {
        x = 30
    }

    val field: Int
        get() {
            TODO("Removed by DD")
        }
    var mutableField: Int
        get() = TODO("Removed by DD")
        set(value) {
            TODO("Removed by DD")
        }

    var zzzz: String
    init {
        zzzz = "Aboba"
    }
}