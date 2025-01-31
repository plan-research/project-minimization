package org.plan.research.minimization

import org.plan.research.minimization.plugin.benchmark.LogParser
import org.plan.research.minimization.scripts.logs.LogsCSVWriter
import java.nio.file.Path

// Usage: <Benchmark-Project-Dir> <outputCsvPath> <stageName1> [<stageName2> ...]
fun main(args: Array<String>) {
    if (args.size < 2) {
        return
    }

    val baseDir = Path.of(args[0])
    val outputCsvPath = Path.of(args[1])
    val stageNames = args.slice(2 until args.size)
    val parsedLogs = LogParser().parseLogs(baseDir, stageNames)
    LogsCSVWriter(parsedLogs).writeToCsv(outputCsvPath)
}
