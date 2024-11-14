package org.plan.research.minimization.plugin.snapshot

import arrow.core.raise.either
import arrow.core.raise.recover
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.plan.research.minimization.plugin.errors.SnapshotError
import org.plan.research.minimization.plugin.errors.SnapshotError.*
import org.plan.research.minimization.plugin.logging.statLogger
import org.plan.research.minimization.plugin.model.HeavyIJDDContext
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.snapshot.SnapshotManager
import org.plan.research.minimization.plugin.model.snapshot.TransactionBody
import org.plan.research.minimization.plugin.model.snapshot.TransactionResult
import org.plan.research.minimization.plugin.services.ProjectCloningGitService
import org.plan.research.minimization.plugin.services.ProjectCloningService
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand

class ProjectCloningGitSnapshotManager(rootProject: Project) : SnapshotManager {
    private val projectCloning = rootProject.service<ProjectCloningGitService>()
    private val generalLogger = KotlinLogging.logger {}

    override suspend fun <T, C : IJDDContext> transaction(
        context: C,
        action: suspend TransactionBody<T>.(newContext: C) -> C
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
    }.onLeft { it.log() }

    // TODO: JBRes-2103 Resource Management
    private suspend fun closeProject(context: IJDDContext) {
        withContext<Unit>(NonCancellable) {
//            if (context is HeavyIJDDContext) {
//                ProjectManagerEx.getInstanceEx().forceCloseProjectAsync(context.project)
//            } else {
//                // avoid unnecessary project deletion, bad design (poebat' for now)
//                if (context.projectDir != context.indexProjectDir) {
//                    writeAction {
//                        context.projectDir.run { delete(fileSystem) }
//                    }
//                }
//            }
            Git.open(context.projectDir.toNioPath().toFile()).apply {
                reset().setMode(ResetCommand.ResetType.HARD).call()
                clean().setCleanDirectories(true).call() // Do we want to remove untracked files?
                close()
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