package org.plan.research.minimization.plugin.benchmark

import kotlinx.serialization.Serializable

@Serializable
data class ExtraInfoStorage(
    val tags: List<String> = emptyList(),
    val issue: String? = null,
)
