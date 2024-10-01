package org.plan.research.minimization.plugin.snapshot

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.withError
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
    ): Either<SnapshotError<T>, IJDDContext> {
        val clonedProject = projectCloning.clone(context.project)
            ?: return TransactionCreationFailed<T>("Failed to create project").left()

        return either<SnapshotError<T>, IJDDContext> {
            val translator = withError(::TransactionCreationFailed) {
                val sourcePath = context.project.guessProjectDir()?.path
                    ?: raise("Failed to get source path")
                val clonedProjectDir = context.project.guessProjectDir()
                    ?: raise("Failed to get cloned project dir")
                Translator(clonedProjectDir, sourcePath)
            }

            try {
                withError(::Aborted) {
                    val transaction = TransactionBody<T>(this@withError, translator)
                    transaction.action(context.copy(project = clonedProject))
                }
            } catch (e: Throwable) {
                raise(TransactionFailed(e))
            }
        }.onLeft {
            ProjectManager.getInstance().closeAndDispose(clonedProject)
        }.onRight {
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
