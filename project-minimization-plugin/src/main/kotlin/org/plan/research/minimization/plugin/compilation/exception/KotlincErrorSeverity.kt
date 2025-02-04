package org.plan.research.minimization.plugin.compilation.exception

import kotlinx.serialization.Serializable

@Serializable
enum class KotlincErrorSeverity {
    ERROR, INFO, UNKNOWN, WARNING
}
