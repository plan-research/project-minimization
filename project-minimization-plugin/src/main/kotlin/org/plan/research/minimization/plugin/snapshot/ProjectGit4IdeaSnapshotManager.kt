package org.plan.research.minimization.plugin.snapshot

import org.plan.research.minimization.plugin.errors.SnapshotError
import org.plan.research.minimization.plugin.errors.SnapshotError.*
import org.plan.research.minimization.plugin.logging.statLogger
import org.plan.research.minimization.plugin.model.context.IJDDContextBase
import org.plan.research.minimization.plugin.model.monad.IJDDContextMonad
import org.plan.research.minimization.plugin.model.monad.SnapshotMonad
import org.plan.research.minimization.plugin.model.monad.TransactionAction
import org.plan.research.minimization.plugin.model.monad.TransactionResult
import org.plan.research.minimization.plugin.model.snapshot.SnapshotManager
import org.plan.research.minimization.plugin.services.GitWrapperService
import org.plan.research.minimization.plugin.services.ProjectCloningService

import arrow.core.raise.either
import arrow.core.raise.recover
import com.intellij.openapi.components.service
import com.intellij.util.application
import mu.KotlinLogging
import org.eclipse.jgit.api.Git
import org.plan.research.minimization.plugin.services.Git4IdeaWrapperService

class ProjectGit4IdeaSnapshotManager : SnapshotManager {
    private val logger = KotlinLogging.logger {}
    private val git4IdeaWrapperService = application.service<Git4IdeaWrapperService>()

    override suspend fun <C : IJDDContextBase<C>> createMonad(context: C): SnapshotMonad<C> {
//        val git = gitWrapperService.gitInit(context.indexProjectDir) { file ->
//            ProjectCloningService.isImportant(
//                file,
//                context.projectDir,
//            )
//        }

        logger.info { "Initializing Git4Idea SnapshotManager" }
        git4IdeaWrapperService.gitInit(context) { file ->
            ProjectCloningService.isImportant(
                file,
                context.projectDir,
            )
        }

        return ProjectCloningMonad(context)
    }

    private fun <T> SnapshotError<T>.log() = when (this) {
        is Aborted<*> -> {
            logger.info { "Transaction aborted. Reason: $reason" }
            statLogger.info { "Transaction aborted" }
        }

        is TransactionFailed -> {
            logger.error(error) { "Transaction failed with error" }
            statLogger.info { "Transaction failed with error" }
        }

        is TransactionCreationFailed -> {
            logger.error { "Failed to create project transaction. Reason: $reason" }
            statLogger.info { "Failed to create project transaction" }
        }
    }

    private inner class ProjectCloningMonad<C : IJDDContextBase<C>>(context: C) : SnapshotMonad<C> {
        override var context: C = context
            private set

        override suspend fun <T> transaction(action: TransactionAction<T, C>): TransactionResult<T> = either {
            statLogger.info { "Snapshot manager start's transaction" }
            logger.info { "Snapshot manager start's transaction" }

            val monad = IJDDContextMonad(context)

            logger.info { "Commits: ${git4IdeaWrapperService.getCommitList(context)}" }

            try {
                recover<T, _>(
                    block = {
                        action(monad, this)
                    },
                    recover = { raise(Aborted(it)) },
                    catch = { raise(TransactionFailed(it)) },
                )
            } catch (e: Throwable) {
                git4IdeaWrapperService.resetChanges(context)
                throw e
            }
        }.onRight {
            logger.info { "Transaction completed successfully" }
            statLogger.info { "Transaction result: success" }
            git4IdeaWrapperService.commitChanges(context)
        }.onLeft { it.log() }
    }
}
