package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.MinimizationStage
import org.plan.research.minimization.plugin.model.state.*

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.XCollection
import org.jetbrains.annotations.NonNls

@State(
    name = "org.intellij.sdk.settings.AppSettings",
    storages = [Storage("ProjectMinimizationSettings.xml")],
)
class AppSettings : PersistentStateComponent<AppSettings.State> {
    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    data class State(
        @NonNls
        var compilationStrategy: CompilationStrategy = CompilationStrategy.GRADLE_IDEA,
        var temporaryProjectLocation: String = "minimization-project-snapshots",
        var snapshotStrategy: SnapshotStrategy = SnapshotStrategy.PROJECT_CLONING,
        var exceptionComparingStrategy: ExceptionComparingStrategy = ExceptionComparingStrategy.SIMPLE,

        @Property(surroundWithTag = false)
        @XCollection(style = XCollection.Style.v1, elementName = "stage")
        var stages: MutableList<MinimizationStage> = mutableListOf(
            FileLevelStage(
                hierarchyCollectionStrategy = HierarchyCollectionStrategy.FILE_TREE,
                ddAlgorithm = DDStrategy.PROBABILISTIC_DD,
            ),
        ),

        @Property(surroundWithTag = false)
        @XCollection(style = XCollection.Style.v1, elementName = "minimizationTransformations")
        var minimizationTransformations: MutableList<TransformationDescriptors> = mutableListOf(
            TransformationDescriptors.PATH_RELATIVIZATION,
        ),

        var configMode: StageConfigMode = StageConfigMode.DEFAULT,
        var isFileStageEnabled: Boolean = false,
        var selectedHierarchyStrategy: HierarchyCollectionStrategy = HierarchyCollectionStrategy.FILE_TREE,
        var selectedDDStrategy: DDStrategy = DDStrategy.PROBABILISTIC_DD,
        var isHierarchyStrategyEnabled: Boolean = false,
        var isDDAlgorithmEnabled: Boolean = false,
    )

    companion object {
        fun getInstance(): AppSettings = ApplicationManager.getApplication().getService(AppSettings::class.java)
    }
}
