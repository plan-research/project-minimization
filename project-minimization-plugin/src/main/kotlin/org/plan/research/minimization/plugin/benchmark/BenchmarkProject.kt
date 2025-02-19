package org.plan.research.minimization.plugin.benchmark

import kotlinx.serialization.Serializable

@Serializable
data class BenchmarkProject(
    val name: String,
    val path: String,
    val buildSystem: BuildSystemDescriptor,
    val kotlinVersion: String,
    val modules: ProjectModulesType,
    val extra: ExtraInfoStorage? = null,
    val javaVersion: Int,
    val settingsFile: String? = null,
)
