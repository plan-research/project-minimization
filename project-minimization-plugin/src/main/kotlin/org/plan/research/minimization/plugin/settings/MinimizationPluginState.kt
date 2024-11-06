package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.FunctionLevelStage
import org.plan.research.minimization.plugin.model.MinimizationStage
import org.plan.research.minimization.plugin.model.state.*

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.XCollection
import org.jetbrains.annotations.NonNls

@Service(Service.Level.PROJECT)
@State(
    name = "org.intellij.sdk.settings.AppSettings",
    storages = [Storage("ProjectMinimizationSettings.xml")],
)
class MinimizationPluginState : PersistentStateComponent<MinimizationPluginState.State>, BaseState() {
    private var myState = State()
    var stateObservable: MinimizationPluginStateObservable = MinimizationPluginStateObservable(this)

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun freezeSettings(isFrozen: Boolean) {
        state.isFrozen = isFrozen
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
            FunctionLevelStage(
                ddAlgorithm = DDStrategy.PROBABILISTIC_DD,
            ),
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

        var isFrozen: Boolean = false,
        var configMode: StageConfigMode = StageConfigMode.DEFAULT,
        var isFileStageEnabled: Boolean = false,
        var selectedHierarchyStrategy: HierarchyCollectionStrategy = HierarchyCollectionStrategy.FILE_TREE,
        var selectedDDStrategy: DDStrategy = DDStrategy.PROBABILISTIC_DD,
        var isHierarchyStrategyEnabled: Boolean = false,
        var isDDAlgorithmEnabled: Boolean = false,
    )
}
