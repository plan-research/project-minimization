package org.plan.research.minimization.core.model

typealias MonadF<M, T> = context(M) () -> T

interface Monad

object EmptyMonad : Monad

abstract class MonadT<M : Monad>(val monad: M) : Monad

context(MonadT<M>)
inline fun <T, M : Monad> lift(block: MonadF<M, T>): T = block(monad)
