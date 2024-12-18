package org.plan.research.minimization.plugin.benchmark

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings
import org.plan.research.minimization.plugin.settings.MinimizationPluginState

@Serializable
data class BenchmarkProject(
    val name: String,
    val path: String,
    @SerialName("reproduce") val reproduceScriptPath: String,
    val buildSystem: BuildSystemDescriptor,
    val kotlinVersion: String,
    val modules: ProjectModulesType,
    val extra: ExtraInfoStorage? = null,
    val javaVersion: Int,
    val settingsFile: String? = null,
)
