package org.plan.research.minimization.plugin.benchmark

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BuildSystemType {
    @SerialName("gradle") GRADLE,
    @SerialName("maven") MAVEN,
    ;
}
