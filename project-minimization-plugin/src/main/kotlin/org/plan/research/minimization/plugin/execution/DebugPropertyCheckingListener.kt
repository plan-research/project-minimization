package org.plan.research.minimization.plugin.execution

import org.plan.research.minimization.plugin.execution.SameExceptionPropertyTester.PropertyCheckingListener
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.IJDDItem
import org.plan.research.minimization.plugin.model.exception.CompilationException

import mu.KotlinLogging

import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DebugPropertyCheckingListener<T : IJDDItem>(folderSuffix: String) : PropertyCheckingListener<T> {
    private val logLocation = Path(System.getProperty("idea.log.path"), "property-checking-log-$folderSuffix")
    private val entries = mutableMapOf<String, LogStage>()
    private val logger = KotlinLogging.logger { }

    init {
        logLocation.createDirectories()
    }

    override fun onComparedExceptions(
        context: IJDDContext,
        initialException: CompilationException,
        newException: CompilationException,
        result: Boolean,
    ) {
        val id = context.projectDir.name
        val entry = entries[id] as? LogStage.BeforeFocus
            ?: logger.error { "No entry found for $id after failed compilation" }.let { return }
        val initialException = initialException as? IdeaCompilationException
            ?: logger.warn { "Initial exception is not IdeaCompilationException" }
                .let { return }
        val newException = newException as? IdeaCompilationException
            ?: logger.warn { "New exception is not IdeaCompilationException" }
                .let { return }
        val newEntry = LogStage.ComparisonCompleted(
            entry.items,
            initialException,
            newException,
            result,
        )
        fileById(id).writeText(json.encodeToString(newEntry))
    }

    override fun beforeFocus(context: IJDDContext, items: List<T>) {
        entries[context.projectDir.name] = LogStage.BeforeFocus(items.map { it.toString() })
    }

    override fun onEmptyLevel(context: IJDDContext) {
        fileById(context.projectDir.name).writeText("{\"status\": \"emptyLevel\"}")
    }

    override fun onSuccessfulCompilation(context: IJDDContext) {
        val id = context.projectDir.name
        val entry = entries[id] as? LogStage.BeforeFocus
            ?: logger.error { "No entry found for $id on successful compilation" }.let { return }
        fileById(context.projectDir.name).writeText(
            Json.encodeToString(
                LogStage.SuccessfulCompilation(
                    entry.items,
                    "compilationSuccessful",
                ),
            ),
        )
    }

    private fun fileById(id: String) = logLocation.resolve("$id.json")

    @Serializable
    private sealed interface LogStage {
        @Serializable
        data class BeforeFocus(val items: List<String>) : LogStage

        @Serializable
        data class SuccessfulCompilation(val items: List<String>, val status: String) : LogStage

        @Serializable
        data class ComparisonCompleted(
            val items: List<String>,
            val initialException: IdeaCompilationException,
            val compilationResult: IdeaCompilationException,
            val result: Boolean,
        ) : LogStage
    }

    companion object {
        private val json = Json { prettyPrint = true }
        fun<T : IJDDItem> create(name: String): DebugPropertyCheckingListener<T>? {
            val logger = KotlinLogging.logger { }
            return if (logger.isTraceEnabled) {
                DebugPropertyCheckingListener(name)
            } else {
                null
            }
        }
    }
}