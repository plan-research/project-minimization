class A<T: Comparable<T>, U: Comparable<U>>(val x: T, val y: List<T>, val z: U)

typealias C = (Int) -> String
typealias E<T> = A<T, T>
typealias G<V, U> = ((V) -> A<V, U>) -> A<U, *>
