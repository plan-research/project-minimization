package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.plugin.model.exception.CompilationResult

import com.intellij.build.events.BuildEvent

/**
 * An interface for parsing build events acquired after running the intermediate parsers to [CompilationResult]
 */
interface BuildEventTranslator {
    fun parseException(event: BuildEvent): CompilationResult
}
