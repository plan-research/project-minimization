class A(val x: Int) {
    constructor(z: String, a: Int): this(z.toInt() + a) {
        println("Body!")
    }
}