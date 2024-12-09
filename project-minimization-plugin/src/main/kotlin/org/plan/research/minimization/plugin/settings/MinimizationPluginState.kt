package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.model.DeclarationLevelStage
import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.FunctionLevelStage
import org.plan.research.minimization.plugin.model.MinimizationStage
import org.plan.research.minimization.plugin.model.state.CompilationStrategy
import org.plan.research.minimization.plugin.model.state.ExceptionComparingStrategy
import org.plan.research.minimization.plugin.model.state.SnapshotStrategy
import org.plan.research.minimization.plugin.model.state.TransformationDescriptors

import com.intellij.openapi.components.BaseState
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection

class MinimizationPluginState : BaseState() {
    var compilationStrategy by enum(CompilationStrategy.GRADLE_IDEA)
    var gradleTask by property("build") { it == "build" }
    var temporaryProjectLocation by property("minimization-project-snapshots") { it == "minimization-project-snapshots" }
    var snapshotStrategy by enum(SnapshotStrategy.PROJECT_CLONING)
    var exceptionComparingStrategy by enum(ExceptionComparingStrategy.SIMPLE)

    @get:Tag
    @get:XCollection(style = XCollection.Style.v1, elementName = "option")
    var gradleOptions by property(emptyList<String>()) { it.isEmpty() }

    @get:Tag
    @get:XCollection(
        style = XCollection.Style.v1,
        elementName = "stage",
        elementTypes = [FunctionLevelStage::class, DeclarationLevelStage::class, FileLevelStage::class],
    )
    var stages by property(defaultStages) { it == defaultStages }

    @get:Tag
    @get:XCollection(style = XCollection.Style.v1, elementName = "transformation")
    var minimizationTransformations by property(defaultTransformations) { it == defaultTransformations }

    @get:Tag
    @get:XCollection(style = XCollection.Style.v1, elementName = "ignore-paths")
    var ignorePaths by property(emptyList<String>()) { it.isEmpty() }

    companion object {
        private val defaultStages: List<MinimizationStage> = listOf(
            FunctionLevelStage(),
            DeclarationLevelStage(),
            FileLevelStage(),
        )
        private val defaultTransformations: List<TransformationDescriptors> = listOf(
            TransformationDescriptors.PATH_RELATIVIZATION,
        )
    }
}
