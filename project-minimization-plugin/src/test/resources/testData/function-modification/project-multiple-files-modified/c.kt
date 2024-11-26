class C {
    fun method() {
        TODO("Removed by DD") as Unit
    }

    fun method2() = TODO("Removed by DD") as Unit

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

}