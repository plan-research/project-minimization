package org.plan.research.minimization.plugin.snapshot

import arrow.core.Either
import arrow.core.identity
import arrow.core.raise.either
import arrow.core.raise.fold
import arrow.core.raise.withError
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import org.plan.research.minimization.plugin.errors.SnapshotError
import org.plan.research.minimization.plugin.errors.SnapshotError.*
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.snapshot.SnapshotManager
import org.plan.research.minimization.plugin.model.snapshot.TransactionBody
import org.plan.research.minimization.plugin.model.snapshot.TransactionTranslator
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
            val translator = withError(::TransactionCreationFailed) {
                val sourcePath = context.project.guessProjectDir()?.path
                    ?: raise("Failed to get source path")
                val clonedProjectDir = context.project.guessProjectDir()
                    ?: raise("Failed to get cloned project dir")
                Translator(clonedProjectDir, sourcePath)
            }

            fold<T, _, _>(
                block = {
                    val transaction = TransactionBody(this@fold, translator)
                    transaction.action(context.copy(project = clonedProject))
                },
                catch = { raise(TransactionFailed(it)) },
                recover = { raise(Aborted(it)) },
                transform = ::identity,
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

    private inner class Translator(
        val clonedProjectDir: VirtualFile,
        val originalPath: String,
    ) : TransactionTranslator {
        override fun VirtualFile.translate(): VirtualFile? =
            clonedProjectDir.findFileByRelativePath(path.removePrefix(originalPath))
    }
}
