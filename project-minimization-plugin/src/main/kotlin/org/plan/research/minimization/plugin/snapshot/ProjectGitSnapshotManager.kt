package org.plan.research.minimization.plugin.snapshot

import org.plan.research.minimization.plugin.errors.SnapshotError
import org.plan.research.minimization.plugin.errors.SnapshotError.*
import org.plan.research.minimization.plugin.logging.statLogger
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.snapshot.SnapshotManager
import org.plan.research.minimization.plugin.model.snapshot.TransactionBody
import org.plan.research.minimization.plugin.model.snapshot.TransactionResult
import org.plan.research.minimization.plugin.services.GitWrapperService

import arrow.core.raise.either
import arrow.core.raise.recover
import com.intellij.openapi.components.service
import com.intellij.util.application
import mu.KotlinLogging

/**
 * Manages the creation and handling of project git snapshots for transactions.
 */
class ProjectGitSnapshotManager : SnapshotManager {
    private val gitWrapperService = application.service<GitWrapperService>()
    private val generalLogger = KotlinLogging.logger {}

    /**
     * Executes a transaction within the provided context.
     * Snapshots are managed with Git operations (via JGit wrapper).
     *
     * On successful transaction, "git commit" will be executed.
     * On failure, "git reset --HARD" will be executed.
     *
     * It is expected that there was at least one commit before any transaction is executed.
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

        try {
            recover<T, _>(
                block = {
                    val transaction = TransactionBody(this@recover)
                    transaction.action(context as C)
                },
                recover = { raise(Aborted(it)) },
                catch = { raise(TransactionFailed(it)) },
            )
        } catch (e: Throwable) {
            gitWrapperService.resetChanges(context)
            throw e
        }
    }.onRight {
        generalLogger.info { "Transaction completed successfully" }
        statLogger.info { "Transaction result: success" }
        gitWrapperService.commitChanges(context)
    }.onLeft { it.log() }

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
