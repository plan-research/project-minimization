package org.plan.research.minimization.plugin.execution.exception

import kotlinx.serialization.Serializable

@Serializable
enum class KotlincErrorSeverity {
    ERROR, INFO, UNKNOWN, WARNING
}
