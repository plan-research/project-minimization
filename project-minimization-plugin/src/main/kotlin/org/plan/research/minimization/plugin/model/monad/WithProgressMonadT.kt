package org.plan.research.minimization.plugin.model.monad

import org.plan.research.minimization.core.model.Monad
import org.plan.research.minimization.core.model.MonadF
import org.plan.research.minimization.core.model.MonadT

import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.util.progress.reportSequentialProgress

typealias WithProgressMonadF<M, T> = MonadF<WithProgressMonadT<M>, T>
typealias SnapshotWithProgressMonad<C> = WithProgressMonadT<SnapshotMonad<C>>
typealias SnapshotWithProgressMonadF<C, T> = MonadF<SnapshotWithProgressMonad<C>, T>

sealed class WithProgressMonadT<M : Monad>(monad: M) : MonadT<M>(monad) {
    /**
     * Starts a new step.
     * Ends a previous step if any.
     * @see [SequentialProgressReporter.nextStep]
     *
     * @param endFraction
     */
    abstract fun nextStep(endFraction: Int)

    companion object {
        const val MAXIMUM_FRACTION = 100
    }
}

class WithEmptyProgressMonadT<M : Monad>(monad: M) : WithProgressMonadT<M>(monad) {
    override fun nextStep(endFraction: Int) {}
}

class WithProgressReporterMonadT<M : Monad>(
    private val reporter: SequentialProgressReporter,
    monad: M,
) : WithProgressMonadT<M>(monad) {
    override fun nextStep(endFraction: Int) {
        reporter.nextStep(endFraction)
    }
}

suspend inline fun <T, M : Monad> M.withProgress(block: WithProgressMonadF<M, T>): T =
    reportSequentialProgress {
        WithProgressReporterMonadT(it, this@withProgress).run {
            block(this@run)
        }
    }

inline fun <T, M : Monad> M.withEmptyProgress(block: WithProgressMonadF<M, T>): T =
    WithEmptyProgressMonadT(this@withEmptyProgress).run(block)
