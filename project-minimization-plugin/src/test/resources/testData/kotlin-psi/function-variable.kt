fun fn() {
    data class DataClass(val x: Int, var y: Double)

    var z = "zzz"
    var x = DataClass(1, 2.0)
    println("$z $x")
}