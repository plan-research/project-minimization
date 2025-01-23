class A<T: Comparable<T>, U: Comparable<U>>(val x: T, val y: List<T>, val z: U)

typealias B = A<Int, String>
typealias C = (Int) -> String
typealias D = (Int, String) -> Unit
typealias E<T> = A<T, T>
typealias F<T> = (T) -> T
typealias G<V, U> = ((V) -> A<V, U>) -> A<U, *>
typealias G2<V, U> = Pair<V, U>
