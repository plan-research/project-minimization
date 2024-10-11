package org.plan.research.minimization.plugin.model

import java.nio.file.Path

/**
 * A 0-indexed a position inside a file
 * @property filePath a path to the file: could be absolute or relative
 * @property lineNumber 0-indexed line number of the desired position
 * @property columnNumber 0-indexed column number within that row
 */
data class CaretPosition(val filePath: Path, val lineNumber: Int, val columnNumber: Int) {
    companion object
}