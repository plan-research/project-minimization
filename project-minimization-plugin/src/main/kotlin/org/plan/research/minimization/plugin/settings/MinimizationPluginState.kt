package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.model.DeclarationGraphStage
import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.FunctionLevelStage
import org.plan.research.minimization.plugin.model.MinimizationStage
import org.plan.research.minimization.plugin.model.state.CompilationStrategy
import org.plan.research.minimization.plugin.model.state.ExceptionComparingStrategy
import org.plan.research.minimization.plugin.model.state.SnapshotStrategy
import org.plan.research.minimization.plugin.model.state.TransformationDescriptor

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory
import kotlin.io.path.div

class MinimizationPluginState(project: Project? = null) : BaseState() {
    @Transient
    private val defaultSnapshotLocation: String = project
        ?.let { getSnapshotLocationFromProject(it).absolutePathString() }
        ?: ""
    var compilationStrategy by enum(CompilationStrategy.GRADLE_IDEA)
    var gradleTask by property(DEFAULT_GRADLE_TASK) { it == DEFAULT_GRADLE_TASK }
    var temporaryProjectLocation by property(defaultSnapshotLocation) { it == defaultSnapshotLocation }
    var logsLocation by property(DEFAULT_LOGS_LOCATION) { it == DEFAULT_LOGS_LOCATION }
    var snapshotStrategy by enum(SnapshotStrategy.PROJECT_CLONING)
    var exceptionComparingStrategy by enum(ExceptionComparingStrategy.STACKTRACE)

    @get:Tag
    @get:XCollection(style = XCollection.Style.v1, elementName = "option")
    var gradleOptions by property(emptyList<String>()) { it.isEmpty() }

    @get:Tag
    @get:XCollection(
        style = XCollection.Style.v1,
        elementName = "stage",
        elementTypes = [FunctionLevelStage::class, DeclarationGraphStage::class, FileLevelStage::class],
    )
    var stages by property(defaultStages) { it == defaultStages }

    @get:Tag
    @get:XCollection(style = XCollection.Style.v1, elementName = "transformation")
    var minimizationTransformations by property(defaultTransformations) { it == defaultTransformations }

    @get:Tag
    @get:XCollection(style = XCollection.Style.v1, elementName = "ignore-paths")
    var ignorePaths by property(emptyList<String>()) { it.isEmpty() }

    companion object {
        const val DEFAULT_GRADLE_TASK = "build"
        const val DEFAULT_LOGS_LOCATION: String = "minimization-logs"
        val defaultStages: List<MinimizationStage> = listOf(
            FunctionLevelStage(),
            DeclarationGraphStage(),
            FileLevelStage(),
        )
        val defaultTransformations: List<TransformationDescriptor> = listOf(
            TransformationDescriptor.PATH_RELATIVIZATION,
        )
    }
}

fun getSnapshotLocationFromProject(project: Project): Path =
    project.guessProjectDir()
        ?.toNioPath()
        ?.parent
        ?.div("${project.name}-minimization-snapshots")
        ?: createTempDirectory("${project.name}-minimization-snapshots")
