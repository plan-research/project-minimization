package org.plan.research.minimization.core.model

interface Monad

object EmptyMonad : Monad

abstract class MonadT<M : Monad>(val monad: M) : Monad

context(MonadT<M>)
inline fun <T, M: Monad> lift(block: context(M) () -> T): T = block(monad)
