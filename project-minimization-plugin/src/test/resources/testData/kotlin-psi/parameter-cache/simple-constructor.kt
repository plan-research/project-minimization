class A(val x: Int, val y: Int)
class B(val y: Int, z: Double, f: () -> Unit)
val a = A(1, 2)
val b = B(2, 3.0) {}
val c = B(2, 3.0, {})