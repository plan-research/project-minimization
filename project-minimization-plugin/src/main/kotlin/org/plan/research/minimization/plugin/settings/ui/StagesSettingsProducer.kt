package org.plan.research.minimization.plugin.settings.ui

import org.plan.research.minimization.plugin.settings.data.DDStrategy
import org.plan.research.minimization.plugin.settings.data.DeclarationGraphStageData
import org.plan.research.minimization.plugin.settings.data.FileLevelStageData
import org.plan.research.minimization.plugin.settings.data.FunctionLevelStageData
import org.plan.research.minimization.plugin.settings.data.MinimizationStageData
import org.plan.research.minimization.plugin.settings.data.ddAlgorithm
import org.plan.research.minimization.plugin.settings.data.isFunctionParametersEnabled

import arrow.optics.Lens
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.bind
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.components.dialog
import com.intellij.ui.dsl.builder.*

import java.awt.CardLayout
import java.awt.Dimension
import javax.swing.DefaultListModel
import javax.swing.JPanel
import javax.swing.ListSelectionModel

class StagesSettingsProducer {
    @Suppress("TOO_LONG_FUNCTION")
    fun createStagesPanel(changed: ObservableMutableProperty<Boolean>): PanelWithList<MinimizationStageData> {
        val stagesListModel = DefaultListModel<MinimizationStageData>()

        val stagesList = JBList(stagesListModel).apply {
            emptyText.text = "Stages"
            selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        }

        val panel = ToolbarDecorator.createDecorator(stagesList)
            .setMoveUpAction {
                val selected = stagesList.selectedIndex
                if (selected > 0) {
                    val value = stagesListModel.get(selected)
                    val upValue = stagesListModel.get(selected - 1)
                    stagesListModel.set(selected, upValue)
                    stagesListModel.set(selected - 1, value)
                    stagesList.selectedIndex = selected - 1
                    changed.set(true)
                }
            }
            .setMoveDownAction {
                val selected = stagesList.selectedIndex
                if (selected < stagesListModel.size - 1) {
                    val value = stagesListModel.get(selected)
                    val downValue = stagesListModel.get(selected + 1)
                    stagesListModel.set(selected, downValue)
                    stagesListModel.set(selected + 1, value)
                    stagesList.selectedIndex = selected + 1
                    changed.set(true)
                }
            }
            .setEditAction {
                val index = stagesList.selectedIndex
                val edited = showAddDialog(stagesList.selectedValue)
                edited?.let {
                    stagesListModel.set(index, edited)
                    changed.set(true)
                }
            }
            .setAddAction {
                val index = stagesList.selectedIndex
                val newStage = showAddDialog()
                newStage?.let {
                    stagesListModel.add(index + 1, newStage)
                    stagesList.selectedIndex = index + 1
                    changed.set(true)
                }
            }
            .setRemoveAction {
                val selected = stagesList.selectedIndex
                stagesListModel.remove(selected)
                changed.set(true)
            }
            .createPanel()

        return panel to stagesListModel
    }

    private fun DDStrategy.getComment(): String = when (this) {
        DDStrategy.DD_MIN -> "Default algorithm but less efficient"
        DDStrategy.PROBABILISTIC_DD -> "Faster algorithm but less precise (recommended)"
    }

    private fun Row.strategy(graph: PropertyGraph, ddAlgorithm: GraphProperty<DDStrategy>) {
        val commentText = graph.property(ddAlgorithm.get().getComment()).apply {
            dependsOn(ddAlgorithm) { ddAlgorithm.get().getComment() }
        }

        comboBox(DDStrategy.entries)
            .bindItem(ddAlgorithm)
            .comment(commentText.get())
            .apply { comment?.bind(commentText) }
    }

    private fun <T : MinimizationStageData, V> PropertyGraph.stageProperty(
        stageProperty: GraphProperty<T>,
        lens: Lens<T, V>,
    ) = property(lens.get(stageProperty.get())).apply {
        afterChange {
            stageProperty.set(lens.set(stageProperty.get(), it))
        }
    }

    private fun functionLevelPanel(
        graph: PropertyGraph,
        stageProperty: GraphProperty<FunctionLevelStageData>,
    ): DialogPanel = panel {
        val ddAlgorithm = graph.stageProperty(stageProperty, FunctionLevelStageData.ddAlgorithm)
        row("Description") {
            text("The algorithm replaces function's bodies with `TODO` statements.")
        }
        group("Function Level Settings", indent = false) {
            row("Minimization strategy:") {
                strategy(graph, ddAlgorithm)
            }
        }
    }

    private fun declarationGraphLevelPanel(
        graph: PropertyGraph,
        stageProperty: GraphProperty<DeclarationGraphStageData>,
    ): DialogPanel = panel {
        val ddAlgorithm = graph.stageProperty(stageProperty, DeclarationGraphStageData.ddAlgorithm)
        val withFunctionParameters = graph.stageProperty(stageProperty, DeclarationGraphStageData.isFunctionParametersEnabled)
        row("Description:") {
            text("The algorithm removes declarations, e.g. classes, functions and fields using a adapters approach.")
        }
        group("Declaration Graph Level Settings", indent = false) {
            row("Minimization strategy:") {
                strategy(graph, ddAlgorithm)
            }
            row {
                checkBox("Delete function and constructor parameters")
                    .bindSelected(withFunctionParameters)
                    .comment("Experimental feature")
            }
        }
    }

    private fun fileLevelPanel(
        graph: PropertyGraph,
        stageProperty: GraphProperty<FileLevelStageData>,
    ): DialogPanel = panel {
        val ddAlgorithm = graph.stageProperty(stageProperty, FileLevelStageData.ddAlgorithm)
        row("Description:") {
            text("The algorithm deletes files")
        }
        group("File Level Settings", indent = false) {
            row("Minimization strategy:") {
                strategy(graph, ddAlgorithm)
            }
        }
    }

    private fun showAddDialog(stage: MinimizationStageData? = null): MinimizationStageData? {
        lateinit var current: GraphProperty<out MinimizationStageData>
        val items = createStagesUIData(stage)
        val dialogPanel = addStageDialogPanel(items) { current = it }

        val dialog = dialog("Stage Settings", dialogPanel) {
            dialogPanel.validateAll() + items.filter { it.panel.isVisible }.flatMap { it.panel.validateAll() }
        }

        items.forEach { it.panel.registerValidators(dialog.disposable) }

        return if (dialog.showAndGet()) {
            current.get()
        } else {
            null
        }
    }

    private fun addStageDialogPanel(
        items: List<MinimizationStageUIData>,
        setCurrent: (GraphProperty<out MinimizationStageData>) -> Unit,
    ): DialogPanel {
        lateinit var selected: Cell<ComboBox<String>>

        return panel {
            val index = items.indexOfFirst { it.isDefaultSelected }
                .coerceAtLeast(0)

            row("Stage:") {
                selected = comboBox(items.map { it.name })
                    .applyToComponent {
                        selectedIndex = index

                        val metrics = getFontMetrics(font)
                        val maxWidth = items.maxOf { metrics.stringWidth(it.name) }

                        size = Dimension(
                            maxWidth + 2,
                            size.height,
                        )
                    }
            }

            val cardLayout = CardLayout()
            val cardPanel = JPanel(cardLayout)

            items.forEach { (name, panel, _) -> cardPanel.add(panel, name) }

            setCurrent(items[index].stage)
            cardLayout.show(cardPanel, items[index].name)

            selected.component.addActionListener {
                val selectedIndex = selected.component.selectedIndex
                val item = items[selectedIndex]
                setCurrent(item.stage)
                cardLayout.show(cardPanel, item.name)
            }

            row {
                cell(cardPanel)
            }
        }
    }

    private fun createStagesUIData(
        stage: MinimizationStageData?,
    ): List<MinimizationStageUIData> {
        val propertyGraph = PropertyGraph()
        val newFunctionStage = propertyGraph.property((stage as? FunctionLevelStageData) ?: FunctionLevelStageData())
        val newDeclarationGraphStage = propertyGraph.property((stage as? DeclarationGraphStageData) ?: DeclarationGraphStageData())
        val newFileStage = propertyGraph.property((stage as? FileLevelStageData) ?: FileLevelStageData())

        return listOf(
            MinimizationStageUIData(
                name = "Function level stage",
                panel = functionLevelPanel(propertyGraph, newFunctionStage),
                stage = newFunctionStage,
                isDefaultSelected = stage is FunctionLevelStageData,
            ),
            MinimizationStageUIData(
                name = "Declaration adapters level stage",
                panel = declarationGraphLevelPanel(propertyGraph, newDeclarationGraphStage),
                stage = newDeclarationGraphStage,
                isDefaultSelected = stage is DeclarationGraphStageData,
            ),
            MinimizationStageUIData(
                name = "File level stage",
                panel = fileLevelPanel(propertyGraph, newFileStage),
                stage = newFileStage,
                isDefaultSelected = stage is FileLevelStageData,
            ),
        )
    }

    private data class MinimizationStageUIData(
        val name: String,
        val panel: DialogPanel,
        val stage: GraphProperty<out MinimizationStageData>,
        val isDefaultSelected: Boolean,
    )
}
