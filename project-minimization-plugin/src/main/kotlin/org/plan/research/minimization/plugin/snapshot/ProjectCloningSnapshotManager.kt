package org.plan.research.minimization.plugin.snapshot

import org.plan.research.minimization.plugin.errors.SnapshotError
import org.plan.research.minimization.plugin.errors.SnapshotError.*
import org.plan.research.minimization.plugin.logging.statLogger
import org.plan.research.minimization.plugin.model.context.HeavyIJDDContext
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.context.IJDDContextMonad
import org.plan.research.minimization.plugin.model.snapshot.SnapshotManager
import org.plan.research.minimization.plugin.model.snapshot.TransactionAction
import org.plan.research.minimization.plugin.model.snapshot.TransactionResult
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

    /**
     * Executes a transaction within the provided context,
     * typically involving project cloning and rollback upon failures.
     *
     * Transaction guarantees that:
     * - Cloned project is closed if a transaction fails.
     * - If a transaction is successful, the project of the context is closed.
     *
     * @param T The type parameter indicating the type of any raised error during the transaction.
     * @param action A suspendable lambda function that takes a new transactional context and returns the updated context.
     * @return Either a `SnapshotError` encapsulating the error in case of failure, or the updated `IJDDContext` in case of success.
     */
    context(IJDDContextMonad<C>)
    override suspend fun <T, C : IJDDContext> transaction(
        action: TransactionAction<T, C>,
    ): TransactionResult<T> = either {
        statLogger.info { "Snapshot manager start's transaction" }
        generalLogger.info { "Snapshot manager start's transaction" }

        @Suppress("UNCHECKED_CAST")
        val clonedContext = projectCloning.clone(context) as? C
            ?: raise(TransactionCreationFailed("Failed to create project"))
        val subMonad = createSubMonad(clonedContext)

        try {
            recover<T, _>(
                block = { action(subMonad, this) },
                recover = { raise(Aborted(it)) },
                catch = { raise(TransactionFailed(it)) },
            )
        } catch (e: Throwable) {
            closeProject(subMonad.context)
            throw e
        }
        generalLogger.info { "Transaction completed successfully" }
        statLogger.info { "Transaction result: success" }
        closeProject(context)
        context = subMonad.context
    }.onLeft { it.log() }

    // TODO: JBRes-2103 Resource Management
    private suspend fun closeProject(context: IJDDContext) {
        withContext<Unit>(NonCancellable) {
            if (context is HeavyIJDDContext) {
                ProjectManagerEx.getInstanceEx().forceCloseProjectAsync(context.project)
            } else {
                // avoid unnecessary project deletion, bad design (poebat' for now)
                if (context.projectDir != context.indexProjectDir) {
                    writeAction {
                        context.projectDir.run { delete(fileSystem) }
                    }
                }
            }
        }
    }

    private fun <T> SnapshotError<T>.log() = when (this) {
        is Aborted<*> -> {
            generalLogger.info { "Transaction aborted. Reason: $reason" }
            statLogger.info { "Transaction aborted" }
        }

        is TransactionFailed -> {
            generalLogger.error(error) { "Transaction failed with error" }
            statLogger.info { "Transaction failed with error" }
        }

        is TransactionCreationFailed -> {
            generalLogger.error { "Failed to create project transaction. Reason: $reason" }
            statLogger.info { "Failed to create project transaction" }
        }
    }
}
