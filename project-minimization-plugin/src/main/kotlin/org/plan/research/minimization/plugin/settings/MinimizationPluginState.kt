package org.plan.research.minimization.plugin.settings

import com.intellij.openapi.components.BaseState
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.XCollection
import org.plan.research.minimization.plugin.model.*

class MinimizationPluginState : BaseState() {
    var currentCompilationStrategy by enum<CompilationStrategy>(CompilationStrategy.GRADLE_IDEA)
    var temporaryProjectLocation by string("minimization-project-snapshots")

    @Property(surroundWithTag = false)
    @XCollection(style = XCollection.Style.v1, elementName = "stage")
    val stages: MutableList<MinimizationStage> = mutableListOf(
        FileLevelStage(
            hierarchyCollectionStrategy = HierarchyCollectionStrategy.FILE_TREE,
            ddAlgorithm = DDStrategy.DD_MIN,
        )
    )
}