package org.plan.research.minimization.plugin.context.snapshot.impl

import org.plan.research.minimization.plugin.context.HeavyIJDDContext
import org.plan.research.minimization.plugin.context.IJDDContextBase
import org.plan.research.minimization.plugin.context.IJDDContextMonad
import org.plan.research.minimization.plugin.context.IJDDContextTransformer
import org.plan.research.minimization.plugin.context.LightIJDDContext
import org.plan.research.minimization.plugin.context.snapshot.SnapshotError
import org.plan.research.minimization.plugin.context.snapshot.SnapshotManager
import org.plan.research.minimization.plugin.context.snapshot.SnapshotMonad
import org.plan.research.minimization.plugin.context.snapshot.TransactionAction
import org.plan.research.minimization.plugin.context.snapshot.TransactionResult
import org.plan.research.minimization.plugin.logging.statLogger
import org.plan.research.minimization.plugin.services.ProjectCloningService

import arrow.core.raise.either
import arrow.core.raise.recover
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import mu.KotlinLogging

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Manages the creation and handling of project cloning snapshots for transactions.
 *
 * @param rootProject The root project used to access services.
 */
class ProjectCloningSnapshotManager(rootProject: Project) : SnapshotManager {
    private val projectCloning = rootProject.service<ProjectCloningService>()
    private val generalLogger = KotlinLogging.logger {}

    private object ProjectCloseTransformer : IJDDContextTransformer<Unit> {
        override suspend fun transformLight(context: LightIJDDContext<*>) {
            if (context.projectDir != context.indexProjectDir) {
                writeAction {
                    context.projectDir.run { delete(fileSystem) }
                }
            }
        }

        override suspend fun transformHeavy(context: HeavyIJDDContext<*>) {
            ProjectManagerEx.Companion.getInstanceEx().forceCloseProjectAsync(context.project)
        }
    }

    override suspend fun <C : IJDDContextBase<C>> createMonad(context: C): SnapshotMonad<C> =
        ProjectCloningMonad(context)

    // TODO: JBRes-2103 Resource Management
    private suspend fun closeProject(context: IJDDContextBase<*>) {
        withContext(NonCancellable) {
            context.transform(ProjectCloseTransformer)
        }
    }

    private fun <T> SnapshotError<T>.log() = when (this) {
        is SnapshotError.Aborted<*> -> {
            generalLogger.info { "Transaction aborted. Reason: $reason" }
            statLogger.info { "Transaction aborted" }
        }

        is SnapshotError.TransactionFailed -> {
            generalLogger.error(error) { "Transaction failed with error" }
            statLogger.info { "Transaction failed with error" }
        }

        is SnapshotError.TransactionCreationFailed -> {
            generalLogger.error { "Failed to create project transaction. Reason: $reason" }
            statLogger.info { "Failed to create project transaction" }
        }
    }

    private inner class ProjectCloningMonad<C : IJDDContextBase<C>>(context: C) : SnapshotMonad<C> {
        override var context: C = context
            private set

        override suspend fun <T> transaction(action: TransactionAction<T, C>): TransactionResult<T> = either {
            statLogger.info { "Snapshot manager start's transaction" }
            generalLogger.info { "Snapshot manager start's transaction" }

            val clonedContext = context.clone(projectCloning)
                ?: raise(SnapshotError.TransactionCreationFailed("Failed to create project"))

            val monad = IJDDContextMonad(clonedContext)

            try {
                recover<T, _>(
                    block = { action(monad, this) },
                    recover = { raise(SnapshotError.Aborted(it)) },
                    catch = { raise(SnapshotError.TransactionFailed(it)) },
                )
            } catch (e: Throwable) {
                closeProject(monad.context)
                throw e
            }

            generalLogger.info { "Transaction completed successfully" }
            statLogger.info { "Transaction result: success" }

            closeProject(context)
            context = monad.context
        }.onLeft { it.log() }
    }
}
