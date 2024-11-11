package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.FunctionLevelBodyReplacementStage
import org.plan.research.minimization.plugin.model.MinimizationStage
import org.plan.research.minimization.plugin.model.state.*

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.XCollection
import org.jetbrains.annotations.NonNls

class MinimizationPluginState : BaseState() {
    @NonNls
    var compilationStrategy: CompilationStrategy = CompilationStrategy.GRADLE_IDEA
    var gradleTask: String = "build"
    var gradleOptions: List<String> = emptyList()
    var temporaryProjectLocation: String = "minimization-project-snapshots"
    var snapshotStrategy: SnapshotStrategy = SnapshotStrategy.PROJECT_CLONING
    var exceptionComparingStrategy: ExceptionComparingStrategy = ExceptionComparingStrategy.SIMPLE

    @Property(surroundWithTag = false)
    @XCollection(style = XCollection.Style.v1, elementName = "stage")
    var stages: List<MinimizationStage> = listOf(
        FunctionLevelBodyReplacementStage(
            ddAlgorithm = DDStrategy.PROBABILISTIC_DD,
        ),
        FileLevelStage(
            hierarchyCollectionStrategy = HierarchyCollectionStrategy.FILE_TREE,
            ddAlgorithm = DDStrategy.PROBABILISTIC_DD,
        ),
    )

    @Property(surroundWithTag = false)
    @XCollection(style = XCollection.Style.v1, elementName = "minimizationTransformations")
    var minimizationTransformations: List<TransformationDescriptors> = listOf(
        TransformationDescriptors.PATH_RELATIVIZATION,
    )
}
