package org.plan.research.minimization.plugin.settings.ui

import org.plan.research.minimization.plugin.algorithm.MinimizationStage
import org.plan.research.minimization.plugin.compilation.gradle.GradleBuildExceptionProvider
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings
import org.plan.research.minimization.plugin.settings.MinimizationPluginState
import org.plan.research.minimization.plugin.settings.enums.CompilationStrategy
import org.plan.research.minimization.plugin.settings.enums.ExceptionComparingStrategy
import org.plan.research.minimization.plugin.settings.enums.SnapshotStrategy

import com.intellij.openapi.components.service
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.intellij.util.execution.ParametersListUtil

import javax.swing.DefaultListModel
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.relativeTo

@Suppress("NO_CORRESPONDING_PROPERTY")
abstract class SettingsProducerBase(protected val project: Project) {
    protected val settings = project.service<MinimizationPluginSettings>()
    protected val state = settings.stateObservable
    private val stagesSettingsProducer by lazy { StagesSettingsProducer() }
    private val ignoreFilesSettingsProducer by lazy { IgnoreFilesSettingsProducer() }
    private val tempProjectLocationProducer by lazy { TempProjectLocationProducer() }

    abstract fun getPanel(): DialogPanel

    protected fun Panel.compilationStrategy() {
        row("Compilation strategy:") {
            comboBox(CompilationStrategy.entries)
                .bindItem(state.compilationStrategy::get, state.compilationStrategy::set)
        }
    }

    protected fun Panel.gradleTask() {
        row("Gradle task:") {
            textField()
                .bindText(state.gradleTask::get, state.gradleTask::set)
        }
    }

    protected fun Panel.gradleOptions() {
        row("Gradle options:") {
            textField()
                .bindText(
                    getter = { ParametersListUtil.join(state.gradleOptions.get()) },
                    setter = { state.gradleOptions.set(ParametersListUtil.parse(it)) },
                )
                .applyToComponent {
                    emptyText.text = "--offline --refresh-dependencies etc..."
                }
                .comment(
                    "These options are always passed to Gradle by default:<br>" +
                        ParametersListUtil.join(*GradleBuildExceptionProvider.defaultArguments),
                )
        }
    }

    protected fun Panel.tempProjectLocation() {
        val projectLocation = project.guessProjectDir()?.toNioPath()
        row("Temporary project location:") {
            tempProjectLocationProducer
                .run { create(project, projectLocation) }
                .bindText(
                    getter = {
                        projectLocation?.let {
                            Path(state.temporaryProjectLocation.get()).relativeTo(it).toString()
                        } ?: state.temporaryProjectLocation.get()
                    },
                    setter = {
                        val newPath = projectLocation?.resolve(it)?.absolutePathString() ?: it
                        state.temporaryProjectLocation.set(newPath)
                    },
                )
        }
    }

    protected fun Panel.logsLocation() {
        row("Logs location:") {
            textField()
                .bindText(state.logsLocation::get, state.logsLocation::set)
        }
    }

    protected fun Panel.snapshotStrategy() {
        row("Snapshot strategy:") {
            comboBox(SnapshotStrategy.entries)
                .bindItem(state.snapshotStrategy::get, state.snapshotStrategy::set)
        }
    }

    protected fun Panel.exceptionComparingStrategy() {
        row("Exception comparing strategy:") {
            comboBox(ExceptionComparingStrategy.entries)
                .bindItem(state.exceptionComparingStrategy::get, state.exceptionComparingStrategy::set)
        }
    }

    private fun DefaultListModel<MinimizationStage>.isDefault(): Boolean =
        toList() == MinimizationPluginState.defaultStages

    protected fun Panel.stagesSettings() {
        val propertyGraph = PropertyGraph(isBlockPropagation = false)
        val changed = propertyGraph.property(false)
        val (stagesPanel, listModel) = stagesSettingsProducer.createStagesPanel(changed)

        val isNotDefault = propertyGraph.property(!listModel.isDefault())

        isNotDefault.dependsOn(changed) {
            changed.set(false)
            !listModel.isDefault()
        }

        row("Stages:") {
            link("Reset to default") {
                listModel.clear()
                listModel.addAll(MinimizationPluginState.defaultStages)
                changed.set(true)
            }
                .visibleIf(isNotDefault)
                .align(AlignX.RIGHT)
        }
        row {
            cell(stagesPanel)
                .bindList(listModel, state.stages) { changed.set(true) }
                .resizableColumn()
                .align(Align.FILL)
        }.resizableRow()
    }

    protected fun Panel.transformations() {
        val transformationsAdapter = TransformationsAdapter(state)
        buttonsGroup("Transformations:") {
            row { checkBox("PATH RELATIVIZATION").bindSelected(transformationsAdapter::pathRelativization) }
        }
    }

    protected fun Panel.pathPanel() {
        val projectDir = project.guessProjectDir() ?: return
        val (ignorePathsPanel, ignoreFilesList) = ignoreFilesSettingsProducer.createIgnorePathsPanel(projectDir)

        row {
            label("Ignore paths:")
                .comment("Add here directories/files of your project if you are sure that they are important or can not be minimized")
        }
        row {
            cell(ignorePathsPanel)
                .bindList(ignoreFilesList, state.ignorePaths)
                .resizableColumn()
                .align(Align.FILL)
        }.resizableRow()
    }
}
