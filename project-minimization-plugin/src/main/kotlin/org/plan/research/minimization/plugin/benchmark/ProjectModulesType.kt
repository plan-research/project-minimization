package org.plan.research.minimization.plugin.benchmark

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ProjectModulesType {
    @SerialName("multiple") MULTIPLE,
    @SerialName("single") SINGLE,
    ;
}
