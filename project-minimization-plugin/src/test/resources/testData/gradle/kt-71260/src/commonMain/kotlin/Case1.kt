interface Base1 {
    fun method()
}
expect interface Foo : Base1 {
    override fun method()
}

fun unused2() {
    println("This function is unused too, but stored in the used file. So, it will be deleted by stage 2")
}