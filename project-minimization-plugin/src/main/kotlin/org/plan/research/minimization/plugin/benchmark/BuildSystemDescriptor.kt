package org.plan.research.minimization.plugin.benchmark

import kotlinx.serialization.Serializable

@Serializable
data class BuildSystemDescriptor(val type: BuildSystemType, val version: String)
