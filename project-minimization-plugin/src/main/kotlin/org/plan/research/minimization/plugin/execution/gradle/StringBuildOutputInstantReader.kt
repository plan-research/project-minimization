package org.plan.research.minimization.plugin.execution.gradle

import com.intellij.build.output.BuildOutputInstantReader

/**
 * A simple implementation of [com.intellij.build.output.BuildOutputInstantReader] to use it in [com.intellij.build.output.KotlincOutputParser]
 * that uses already read content to pass into parsers.
 *
 * @property parentEventId Identifier for the parent event in the build process.
 * @property lines List of lines representing the build output.
 */
class StringBuildOutputInstantReader(
    private val parentEventId: String,
    private val lines: List<String>,
) : BuildOutputInstantReader {
    private var currentIndex = 0
    override fun getParentEventId() = parentEventId

    override fun readLine(): String? = when (currentIndex) {
        in lines.indices -> lines[currentIndex++]
        else -> null
    }


    override fun pushBack() = pushBack(1)

    override fun pushBack(numberOfLines: Int) {
        val diff = numberOfLines.coerceAtMost(currentIndex)
        currentIndex -= diff
    }

    companion object {
        /**
         * A basic constructor to make it from a concatenated output
         */
        fun create(parentEventId: String, lines: String) = StringBuildOutputInstantReader(
            parentEventId,
            lines.lines(),
        )
    }
}