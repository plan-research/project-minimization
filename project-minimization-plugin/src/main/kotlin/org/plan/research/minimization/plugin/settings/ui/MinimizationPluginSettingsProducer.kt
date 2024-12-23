package org.plan.research.minimization.plugin.settings.ui

import org.plan.research.minimization.plugin.execution.gradle.GradleBuildExceptionProvider
import org.plan.research.minimization.plugin.model.MinimizationStage
import org.plan.research.minimization.plugin.model.state.CompilationStrategy
import org.plan.research.minimization.plugin.model.state.ExceptionComparingStrategy
import org.plan.research.minimization.plugin.model.state.SnapshotStrategy
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings
import org.plan.research.minimization.plugin.settings.MinimizationPluginState

import com.intellij.openapi.components.service
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.bindEnabled
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.intellij.util.execution.ParametersListUtil

import javax.swing.DefaultListModel

@Suppress("NO_CORRESPONDING_PROPERTY")
class MinimizationPluginSettingsProducer(private val project: Project) {
    private val settings = project.service<MinimizationPluginSettings>()
    private val state = settings.stateObservable
    private val stagesSettingsProducer = StagesSettingsProducer()
    private val ignoreFilesSettingsProducer = IgnoreFilesSettingsProducer()

    fun getPanel(): DialogPanel =
        panel {
            compilationStrategy()
            gradleTask()
            gradleOptions()
            tempProjectLocation()
            snapshotStrategy()
            exceptionComparingStrategy()

            separator()

            stagesSettings()

            separator()

            transformations()

            separator()

            pathPanel()
        }.bindEnabled(settings.settingsEnabled)

    private fun Panel.compilationStrategy() {
        row("Compilation strategy:") {
            comboBox(CompilationStrategy.entries)
                .bindItem(state.compilationStrategy::get, state.compilationStrategy::set)
        }
    }

    private fun Panel.gradleTask() {
        row("Gradle task:") {
            textField().bindText(state.gradleTask::get, state.gradleTask::set)
        }
    }

    private fun Panel.gradleOptions() {
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
                    "These options are always passed to Gradle:<br>" +
                        ParametersListUtil.join(*GradleBuildExceptionProvider.defaultArguments),
                )
        }
    }

    private fun Panel.tempProjectLocation() {
        row("Temporary project location:") {
            textField().bindText(state.temporaryProjectLocation::get, state.temporaryProjectLocation::set)
        }
    }

    private fun Panel.snapshotStrategy() {
        row("Snapshot strategy:") {
            comboBox(SnapshotStrategy.entries)
                .bindItem(state.snapshotStrategy::get, state.snapshotStrategy::set)
        }
    }

    private fun Panel.exceptionComparingStrategy() {
        row("Exception comparing strategy:") {
            comboBox(ExceptionComparingStrategy.entries)
                .bindItem(state.exceptionComparingStrategy::get, state.exceptionComparingStrategy::set)
        }
    }

    private fun DefaultListModel<MinimizationStage>.isDefault(): Boolean =
        toList() == MinimizationPluginState.defaultStages

    private fun Panel.stagesSettings() {
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

    private fun Panel.transformations() {
        val transformationsAdapter = TransformationsAdapter(state)
        buttonsGroup("Transformations:") {
            row { checkBox("PATH RELATIVIZATION").bindSelected(transformationsAdapter::pathRelativization) }
        }
    }

    private fun Panel.pathPanel() {
        val projectDir = project.guessProjectDir() ?: return
        val (ignorePathsPanel, ignoreFilesList) = ignoreFilesSettingsProducer.createIgnorePathsPanel(projectDir)

        row {
            label("Add here directories/files of your project if you are sure that they are important or can not be minimized")
        }
        row {
            cell(ignorePathsPanel)
                .bindList(ignoreFilesList, state.ignorePaths)
                .resizableColumn()
                .align(Align.FILL)
        }.resizableRow()
    }
}
