interface Base1 {
    fun method()
}
expect interface Foo : Base1 {
    override fun method()
}