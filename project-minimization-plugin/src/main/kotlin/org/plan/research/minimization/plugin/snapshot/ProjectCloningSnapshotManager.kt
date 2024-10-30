package org.plan.research.minimization.plugin.snapshot

import org.plan.research.minimization.plugin.errors.SnapshotError.*
import org.plan.research.minimization.plugin.logging.statLogger
import org.plan.research.minimization.plugin.model.HeavyIJDDContext
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.snapshot.SnapshotManager
import org.plan.research.minimization.plugin.model.snapshot.TransactionBody
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
     * - If a transaction is successful, the project of the [context] is closed.
     *
     * @param T The type parameter indicating the type of any raised error during the transaction.
     * @param context The `IJDDContext` containing the project and associated data needed to perform the transaction.
     * @param action A suspendable lambda function that takes a new transactional context and returns the updated context.
     * @return Either a `SnapshotError` encapsulating the error in case of failure, or the updated `IJDDContext` in case of success.
     */
    override suspend fun <T, C : IJDDContext> transaction(
        context: C,
        action: suspend TransactionBody<T>.(newContext: C) -> C,
    ): TransactionResult<T, C> = either {
        statLogger.info { "Snapshot manager start's transaction" }
        generalLogger.info { "Snapshot manager start's transaction" }
        val clonedContext = projectCloning.clone(context)
            ?: raise(TransactionCreationFailed("Failed to create project"))

        try {
            recover<T, _>(
                block = {
                    val transaction = TransactionBody(this@recover)
                    @Suppress("UNCHECKED_CAST")
                    transaction.action(clonedContext as C)
                },
                recover = { raise(Aborted(it)) },
                catch = { raise(TransactionFailed(it)) },
            )
        } catch (e: Throwable) {
            closeProject(clonedContext)
            throw e
        }
    }.onRight {
        generalLogger.info { "Transaction completed successfully" }
        statLogger.info { "Transaction result: success" }
        closeProject(context)
    }.onLeft { error ->
        generalLogger.error { "Transaction failed with error: $error" }
        statLogger.info { "Transaction result: error" }
    }

    private suspend fun closeProject(context: IJDDContext) {
        withContext(NonCancellable) {
            if (context is HeavyIJDDContext) {
                ProjectManagerEx.getInstanceEx().forceCloseProjectAsync(context.project)
            }
            writeAction {
                context.projectDir.run { delete(fileSystem) }
            }
        }
    }
}
