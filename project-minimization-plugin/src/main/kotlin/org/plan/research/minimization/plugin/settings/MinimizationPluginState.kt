package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.FunctionLevelStage
import org.plan.research.minimization.plugin.model.MinimizationStage
import org.plan.research.minimization.plugin.model.state.*

import com.intellij.openapi.components.BaseState
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.XCollection
import org.plan.research.minimization.plugin.model.FileLevelSlicing

class MinimizationPluginState : BaseState() {
    var currentCompilationStrategy by enum<CompilationStrategy>(CompilationStrategy.GRADLE_IDEA)

    /**
     * A location for cloned projects
     */
    var temporaryProjectLocation by string("minimization-project-snapshots")
    var snapshotStrategy by enum<SnapshotStrategy>(SnapshotStrategy.PROJECT_CLONING)

    @Property(surroundWithTag = false)
    @XCollection(style = XCollection.Style.v1, elementName = "stage")
    var stages: MutableList<MinimizationStage> = mutableListOf(
        FileLevelSlicing,
//        FunctionLevelStage(
//            ddAlgorithm = DDStrategy.PROBABILISTIC_DD,
//        ),
//        FileLevelStage(
//            hierarchyCollectionStrategy = HierarchyCollectionStrategy.FILE_TREE,
//            ddAlgorithm = DDStrategy.PROBABILISTIC_DD,
//        ),
    )

    @Property(surroundWithTag = false)
    @XCollection(style = XCollection.Style.v1, elementName = "minimizationTransformations")
    var minimizationTransformations: MutableList<TransformationDescriptors> = mutableListOf(
        TransformationDescriptors.PATH_RELATIVIZATION,
    )
    var exceptionComparingStrategy by enum<ExceptionComparingStrategy>(ExceptionComparingStrategy.SIMPLE)
}
