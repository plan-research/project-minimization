class C {
    fun method() {
        while (true);
    }

    fun method2() = println("Aboba")
    var lambda = { x: Int -> println(x) }
    val x: Int

    init {
        TODO("Removed by DD")
    }

    val field: Int
        get() {
            val string = "Wow cool getter"
            return string.toList().map { it.code }.sum()
        }
    var mutableField: Int
        get() = 5
        set(value) {
            mutableField = 13 + value
        }

    var zzzz: String
    init {
        TODO("Removed by DD")
    }
}