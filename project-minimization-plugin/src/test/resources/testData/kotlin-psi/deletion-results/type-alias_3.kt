class A<T: Comparable<T>, U: Comparable<U>>(val x: T, val y: List<T>, val z: U)

typealias B = A<Int, String>
typealias D = (Int, String) -> Unit
typealias F<T> = (T) -> T
typealias G2<V, U> = Pair<V, U>
