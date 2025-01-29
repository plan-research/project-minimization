package org.plan.research.minimization.plugin.model.monad

import org.plan.research.minimization.core.algorithm.dd.impl.graph.GraphDD
import org.plan.research.minimization.core.model.*

import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.util.progress.reportSequentialProgress

const val DEFAULT_MAX_STEPS = 100

typealias SnapshotWithProgressMonad<C> = WithProgressMonadT<SnapshotMonad<C>>
typealias SnapshotWithProgressMonadFAsync<C, T> = suspend context(SnapshotWithProgressMonad<C>) () -> T

class WithProgressReporterMonadProvider : GraphDD.GraphLayerMonadTProvider {
    context(M)
    override suspend fun <M : Monad> runUnderProgress(block: WithProgressMonadFAsync<M, Unit>) {
        withProgress(block)
    }
}

class WithProgressReporterMonadT<M : Monad>(
    private val maxSteps: Int,
    private val reporter: SequentialProgressReporter,
    monad: M,
) : WithProgressMonadT<M>(monad) {
    override fun nextStep(endFractionP: Int, endFractionQ: Int) {
        if (endFractionQ == maxSteps) {
            reporter.nextStep(endFractionP)
        } else {
            val p = (maxSteps * endFractionP).toLong()
            val frac = p / endFractionQ
            reporter.nextStep(frac.toInt())
        }
    }
}

context(M)
suspend fun <T, M : Monad> withProgress(block: WithProgressMonadFAsync<M, T>): T =
    reportSequentialProgress(DEFAULT_MAX_STEPS) {
        WithProgressReporterMonadT(DEFAULT_MAX_STEPS, it, this@M).run {
            block(this@run)
        }
    }
