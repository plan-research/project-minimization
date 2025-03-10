package org.plan.research.minimization.plugin.settings.data

import arrow.optics.optics
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.Tag

sealed interface MinimizationStageData

@Tag("fileLevelStage")
@optics
data class FileLevelStageData(
    @Property val ddAlgorithm: DDStrategy = DDStrategy.PROBABILISTIC_DD,
) : MinimizationStageData {
    companion object
}

@Tag("functionLevelStage")
@optics
data class FunctionLevelStageData(
    @Property val ddAlgorithm: DDStrategy = DDStrategy.PROBABILISTIC_DD,
) : MinimizationStageData {
    companion object
}

@Tag("declarationGraphStage")
@optics
data class DeclarationGraphStageData(
    @Property val ddAlgorithm: DDStrategy = DDStrategy.PROBABILISTIC_DD,
    @Property val isFunctionParametersEnabled: Boolean = false,
) : MinimizationStageData {
    companion object
}
