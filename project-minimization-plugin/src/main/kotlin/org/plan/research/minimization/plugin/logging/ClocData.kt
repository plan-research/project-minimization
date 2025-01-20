package org.plan.research.minimization.plugin.logging

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class ClocStatistics(
    val header: Header,
    @SerialName("Kotlin") val kotlin: LanguageStatistics? = null,
    val SUM: SummaryStatistics,
    @Transient val otherLanguages: Map<String, JsonObject> = emptyMap()
)

@Serializable
data class Header(
    val cloc_url: String,
    val cloc_version: String,
    val elapsed_seconds: Double,
    val n_files: Int,
    val n_lines: Int,
    val files_per_second: Double,
    val lines_per_second: Double
)

@Serializable
data class LanguageStatistics(
    val nFiles: Int,
    val blank: Int,
    val comment: Int,
    val code: Int
)

@Serializable
data class SummaryStatistics(
    val blank: Int,
    val comment: Int,
    val code: Int,
    val nFiles: Int
)
