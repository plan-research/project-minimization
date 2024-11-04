class C {
    fun method() {
        TODO("Removed by DD")
    }

    fun method2() = TODO("Removed by DD")
    var lambda = { x: Int -> TODO("Removed by DD") }
    val x: Int

    init {
        x = 30
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
        zzzz = "Aboba"
    }
}