interface I {
    fun foo()
}

class X<T: I>(val x: T) {
    init {
        x.foo()
    }
}