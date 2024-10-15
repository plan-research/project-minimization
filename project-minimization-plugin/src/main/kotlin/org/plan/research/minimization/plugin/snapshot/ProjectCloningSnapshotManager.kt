package org.plan.research.minimization.plugin.snapshot

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.recover
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.plan.research.minimization.plugin.errors.SnapshotError
import org.plan.research.minimization.plugin.errors.SnapshotError.*
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.snapshot.SnapshotManager
import org.plan.research.minimization.plugin.model.snapshot.TransactionBody
import org.plan.research.minimization.plugin.services.ProjectCloningService

/**
 * Manages the creation and handling of project cloning snapshots for transactions.
 *
 * @param rootProject The root project used to access services.
 */
class ProjectCloningSnapshotManager(rootProject: Project) : SnapshotManager {
    private val projectCloning = rootProject.service<ProjectCloningService>()

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
    override suspend fun <T> transaction(
        context: IJDDContext,
        action: suspend TransactionBody<T>.(newContext: IJDDContext) -> IJDDContext
    ): Either<SnapshotError<T>, IJDDContext> = either {
        val clonedProject = projectCloning.clone(context.project)
            ?: raise(TransactionCreationFailed("Failed to create project"))
        val clonedContext = context.copy(project = clonedProject)

        try {
            recover<T, _>(
                block = {
                    val transaction = TransactionBody(this@recover)
                    transaction.action(clonedContext)
                },
                recover = { raise(Aborted(it)) },
                catch = { raise(TransactionFailed(it)) },
            )
        } catch (e: Throwable) {
            closeProject(clonedContext)
            throw e
        }
    }.onRight {
        closeProject(context)
    }

    private suspend fun closeProject(context: IJDDContext) {
        // TODO: think about deleting the project
        withContext(NonCancellable + Dispatchers.EDT) {
            ProjectManager.getInstance().closeAndDispose(context.project)
        }
    }
}
