package org.plan.research.minimization.plugin.snapshot

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.recover
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import org.plan.research.minimization.plugin.errors.SnapshotError
import org.plan.research.minimization.plugin.errors.SnapshotError.*
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.snapshot.SnapshotManager
import org.plan.research.minimization.plugin.model.snapshot.TransactionBody
import org.plan.research.minimization.plugin.services.ProjectCloningService

class ProjectCloningSnapshotManager(rootProject: Project) : SnapshotManager {
    private val projectCloning = rootProject.service<ProjectCloningService>()

    override suspend fun <T> transaction(
        context: IJDDContext,
        action: suspend TransactionBody<T>.(newContext: IJDDContext) -> IJDDContext
    ): Either<SnapshotError<T>, IJDDContext> = either {
        val clonedProject = projectCloning.clone(context.project)
            ?: raise(TransactionCreationFailed("Failed to create project"))

        try {
            recover<T, _>(
                block = {
                    val transaction = TransactionBody(this@recover)
                    transaction.action(context.copy(project = clonedProject))
                },
                recover = { raise(Aborted(it)) },
                catch = { raise(TransactionFailed(it)) },
            )
        } catch (e: Throwable) {
            invokeLater {
                ProjectManager.getInstance().closeAndDispose(clonedProject)
            }
            throw e
        }
    }.onRight {
        invokeLater {
            ProjectManager.getInstance().closeAndDispose(context.project)
        }
    }
}
