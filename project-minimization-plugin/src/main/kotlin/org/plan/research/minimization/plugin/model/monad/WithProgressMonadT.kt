package org.plan.research.minimization.plugin.model.monad

import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.util.progress.reportSequentialProgress
import org.plan.research.minimization.core.model.Monad
import org.plan.research.minimization.core.model.MonadT

sealed class WithProgressMonadT<M : Monad>(monad: M, ) : MonadT<M>(monad) {
    abstract fun nextStep(endFraction: Int)
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

suspend inline fun <T, M : Monad> M.withProgress(block: context(WithProgressMonadT<M>) () -> T): T =
    reportSequentialProgress {
        WithProgressReporterMonadT(it, this@withProgress).run {
            block(this@run)
        }
    }

inline fun <T, M : Monad> M.withEmptyProgress(block: context(WithProgressMonadT<M>) () -> T): T =
    WithEmptyProgressMonadT(this@withEmptyProgress).run(block)
