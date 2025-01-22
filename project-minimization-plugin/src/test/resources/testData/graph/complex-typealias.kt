interface I
class A<T: I, U>
typealias B<T> = A<T, T>
typealias C<T> = A<I, T>
typealias D<F, G> = (B<F>) -> C<G>