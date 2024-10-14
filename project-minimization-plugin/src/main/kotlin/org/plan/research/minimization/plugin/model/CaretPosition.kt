package org.plan.research.minimization.plugin.model

import com.intellij.build.FilePosition
import java.nio.file.Path

/**
 * A 0-indexed a position inside a file
 * @property filePath a path to the file: could be absolute or relative
 * @property lineNumber 0-indexed line number of the desired position
 * @property columnNumber 0-indexed column number within that row
 */
data class CaretPosition(val filePath: Path, val lineNumber: Int, val columnNumber: Int) {
    companion object {
        fun fromFilePosition(from: FilePosition) = CaretPosition(
            filePath = from.file.toPath(),
            lineNumber = from.startLine,
            columnNumber = from.startColumn
        )
        fun fromString(from: String): CaretPosition {
            val (filePath, line, column) = from.split(":") // In general paths with ":" will break it. But why should we care now?
            return CaretPosition(Path.of(filePath), line.toInt(), column.toInt())
        }
    }
}