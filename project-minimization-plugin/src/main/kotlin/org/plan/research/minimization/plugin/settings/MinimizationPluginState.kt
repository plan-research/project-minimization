package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.MinimizationStage
import org.plan.research.minimization.plugin.model.state.*

import com.intellij.openapi.components.BaseState
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.XCollection

class MinimizationPluginState : BaseState() {
    var currentCompilationStrategy by enum<CompilationStrategy>(CompilationStrategy.GRADLE_IDEA)

    /**
     * A location for cloned projects
     */
    var temporaryProjectLocation by string("minimization-project-snapshots")
    var snapshotStrategy by enum<SnapshotStrategy>(SnapshotStrategy.PROJECT_CLONING)

    @Property(surroundWithTag = false)
    @XCollection(style = XCollection.Style.v1, elementName = "stage")
    val stages: MutableList<MinimizationStage> = mutableListOf(
        FileLevelStage(
            hierarchyCollectionStrategy = HierarchyCollectionStrategy.FILE_TREE,
            ddAlgorithm = DDStrategy.PROBABILISTIC_DD,
        ),
    )

    @Property(surroundWithTag = false)
    @XCollection(style = XCollection.Style.v1, elementName = "minimizationTransformations")
    var minimizationTransformations: MutableList<TransformationDescriptors> = mutableListOf(
        TransformationDescriptors.PATH_RELATIVIZATION,
    )
    var exceptionComparingStrategy by enum<ExceptionComparingStrategy>(ExceptionComparingStrategy.SIMPLE)
    var propertyCheckerProjectModificationLens by enum<ProjectItemLensDescriptor>(ProjectItemLensDescriptor.FILE_DELETING)
}
