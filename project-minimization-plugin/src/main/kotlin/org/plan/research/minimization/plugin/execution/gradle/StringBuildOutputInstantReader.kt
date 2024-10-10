package org.plan.research.minimization.plugin.execution.gradle

import com.intellij.build.output.BuildOutputInstantReader

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
        fun create(parentEventId: String, lines: String) = StringBuildOutputInstantReader(
            parentEventId,
            lines.lines(),
        )
    }
}