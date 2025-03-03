package org.plan.research.minimization.plugin.algorithm.stages

import org.plan.research.minimization.plugin.algorithm.MinimizationError
import org.plan.research.minimization.plugin.context.HeavyIJDDContext
import org.plan.research.minimization.plugin.context.IJDDContextBase
import org.plan.research.minimization.plugin.context.snapshot.SnapshotMonad
import org.plan.research.minimization.plugin.context.snapshot.SnapshotMonadF
import org.plan.research.minimization.plugin.logging.statLogger
import org.plan.research.minimization.plugin.services.SnapshotManagerService

import arrow.core.raise.Raise
import arrow.core.raise.either
import com.intellij.openapi.components.service
import mu.KLogger

sealed class MinimizationStageBase<C : IJDDContextBase<C>> : MinimizationStage {
    abstract val logger: KLogger

    context(SnapshotMonad<C>, Raise<MinimizationError>)
    protected abstract suspend fun execute()

    context(Raise<MinimizationError>)
    protected abstract suspend fun createContext(context: HeavyIJDDContext<*>): C

    final override suspend fun executeStage(context: HeavyIJDDContext<*>): MinimizationResult = either {
        logger.info { "Start $stageName stage" }
        statLogger.info { "Start $stageName stage" }

        val innerContext = createContext(context)
        innerContext.runMonad {
            execute()
        }
    }.onRight {
        logger.info { "End $stageName level stage" }
        statLogger.info { "End $stageName level stage" }
        statLogger.info { "$stageName level stage result: success" }
    }.onLeft { error ->
        logger.info { "End $stageName level stage" }
        statLogger.info { "End $stageName level stage" }
        statLogger.info { "$stageName level stage result: $error" }
        logger.error { "$stageName level stage failed with error: $error" }
    }

    private suspend inline fun C.runMonad(
        action: SnapshotMonadF<C, Unit>,
    ): C {
        val snapshotManager = originalProject.service<SnapshotManagerService>()
        val monad = snapshotManager.createMonad(this)
        action(monad)
        return monad.context
    }
}
