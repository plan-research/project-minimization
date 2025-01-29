class A(val x: Int) {
    constructor(y: Double): this(y.toInt())
    constructor(z: String, a: Int): this(z.toInt() + a) {
        println("Body!")
    }
}