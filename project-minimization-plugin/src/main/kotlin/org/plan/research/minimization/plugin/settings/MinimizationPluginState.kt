package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.model.MinimizationStage
import org.plan.research.minimization.plugin.model.state.*

import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.XCollection

data class MinimizationPluginState(val state: AppSettings) {
    var compilationStrategy = state.state.compilationStrategy
    var temporaryProjectLocation = state.state.temporaryProjectLocation
    var snapshotStrategy = state.state.snapshotStrategy
    var exceptionComparingStrategy = state.state.exceptionComparingStrategy

    @Property(surroundWithTag = false)
    @XCollection(style = XCollection.Style.v1, elementName = "stage")
    var stages: MutableList<MinimizationStage> = state.state.stages

    @Property(surroundWithTag = false)
    @XCollection(style = XCollection.Style.v1, elementName = "minimizationTransformations")
    var minimizationTransformations: MutableList<TransformationDescriptors> = state.state.minimizationTransformations
}
