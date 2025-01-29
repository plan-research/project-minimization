package org.plan.research.minimization.core.model

typealias WithProgressMonadFAsync<M, T> = suspend context(WithProgressMonadT<M>) () -> T

abstract class WithProgressMonadT<M : Monad>(monad: M) : MonadT<M>(monad) {
    /**
     * Starts a new step.
     * Ends a previous step, if any.
     *
     * Clarify: [endFractionP] out of [endFractionQ]
     *
     * @param endFractionP
     * @param endFractionQ
     */
    abstract fun nextStep(endFractionP: Int, endFractionQ: Int)
}

class WithEmptyProgressMonadT<M : Monad>(monad: M) : WithProgressMonadT<M>(monad) {
    override fun nextStep(endFractionP: Int, endFractionQ: Int) {}
}

context(M)
suspend fun <T, M : Monad> withEmptyProgress(block: WithProgressMonadFAsync<M, T>): T {
    val monad = WithEmptyProgressMonadT(this@M)
    return block(monad)
}
