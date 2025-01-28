interface I {
    fun foo()
}

interface II {
    fun bar()
}

abstract class A(): I, II {
    override fun foo() = Unit
}

abstract class B(): A(), I, II {
    override fun bar() = Unit
}

class C(): B(), I, II {
    override fun foo() = Unit
    override fun bar() = Unit
}