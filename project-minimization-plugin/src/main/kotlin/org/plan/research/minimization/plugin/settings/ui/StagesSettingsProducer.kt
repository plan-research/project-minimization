package org.plan.research.minimization.plugin.settings.ui

import org.plan.research.minimization.plugin.model.*
import org.plan.research.minimization.plugin.model.state.DDStrategy

import arrow.optics.Lens
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.bind
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.JBColor
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.components.dialog
import com.intellij.ui.dsl.builder.*

import java.awt.CardLayout
import javax.swing.DefaultListModel
import javax.swing.JPanel
import javax.swing.ListSelectionModel

class StagesSettingsProducer {
    @Suppress("TOO_LONG_FUNCTION")
    fun createStagesPanel(changed: ObservableMutableProperty<Boolean>): PanelWithList<MinimizationStage> {
        val stagesListModel = DefaultListModel<MinimizationStage>()

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

    private fun Row.booleanProperty(graph: PropertyGraph, property: GraphProperty<Boolean>, text: String = "") {
        checkBox(text)
            .bindSelected(property)
    }

    private fun <T : MinimizationStage, V> PropertyGraph.stageProperty(
        stageProperty: GraphProperty<T>,
        lens: Lens<T, V>,
    ) = property(lens.get(stageProperty.get())).apply {
        afterChange {
            stageProperty.set(lens.set(stageProperty.get(), it))
        }
    }

    private fun functionLevelPanel(
        graph: PropertyGraph,
        stageProperty: GraphProperty<FunctionLevelStage>,
    ): DialogPanel = panel {
        val ddAlgorithm = graph.stageProperty(stageProperty, FunctionLevelStage.ddAlgorithm)
        row("Description") {
            text("The algorithm replaces function's bodies with `TODO` statements.")
        }
        group("Function Level Settings", indent = false) {
            row("Minimization strategy:") {
                strategy(graph, ddAlgorithm)
            }
        }
    }

    private fun declarationLevelPanel(
        graph: PropertyGraph,
        stageProperty: GraphProperty<DeclarationLevelStage>,
    ): DialogPanel = panel {
        val ddAlgorithm = graph.stageProperty(stageProperty, DeclarationLevelStage.ddAlgorithm)
        val maxDepth = graph.stageProperty(stageProperty, DeclarationLevelStage.depthThreshold)
        row {
            text("⚠ This algorithm will increase execution time dramatically!").applyToComponent {
                foreground = JBColor.RED
            }
        }
        row("Description:") {
            text("The algorithm removes declarations, e.g. classes, functions and fields.")
        }
        group("Declaration Level Settings", indent = false) {
            row("Minimization strategy:") {
                strategy(graph, ddAlgorithm)
            }
            row("A max depth for the algorithm:") {
                intTextField(1..Int.MAX_VALUE)
                    .bindIntText(maxDepth)
                    .validationOnApply {
                        val value = it.text.toIntOrNull()
                        if (value == null || value < 1) {
                            error("Please enter a positive integer.")
                        } else {
                            null
                        }
                    }.comment("Recommended value: 2")
            }
        }
    }

    private fun declarationGraphLevelPanel(
        graph: PropertyGraph,
        stageProperty: GraphProperty<DeclarationGraphStage>,
    ): DialogPanel = panel {
        val ddAlgorithm = graph.stageProperty(stageProperty, DeclarationGraphStage.ddAlgorithm)
        val withFunctionParameters = graph.stageProperty(stageProperty, DeclarationGraphStage.isFunctionParametersEnabled)
        row("Description:") {
            text("The algorithm removes declarations, e.g. classes, functions and fields using a graph approach.")
        }
        group("Declaration Graph Level Settings", indent = false) {
            row("Minimization strategy:") {
                strategy(graph, ddAlgorithm)
            }
            row {
                booleanProperty(graph, withFunctionParameters, "Delete function and constructor parameters")
            }
        }
    }

    private fun fileLevelPanel(
        graph: PropertyGraph,
        stageProperty: GraphProperty<FileLevelStage>,
    ): DialogPanel = panel {
        val ddAlgorithm = graph.stageProperty(stageProperty, FileLevelStage.ddAlgorithm)
        row("Description:") {
            text("The algorithm deletes files")
        }
        group("File Level Settings", indent = false) {
            row("Minimization strategy:") {
                strategy(graph, ddAlgorithm)
            }
        }
    }

    private fun showAddDialog(stage: MinimizationStage? = null): MinimizationStage? {
        lateinit var current: GraphProperty<out MinimizationStage>
        val items = createStagesData(stage)
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
        items: List<MinimizationStageData>,
        setCurrent: (GraphProperty<out MinimizationStage>) -> Unit,
    ): DialogPanel {
        lateinit var selected: Cell<ComboBox<String>>

        return panel {
            val index = items.indexOfFirst { it.isDefaultSelected }
                .coerceAtLeast(0)

            row("Stage:") {
                selected = comboBox(items.map { it.name })
                    .applyToComponent { selectedIndex = index }
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

    private fun createStagesData(
        stage: MinimizationStage?,
    ): List<MinimizationStageData> {
        val propertyGraph = PropertyGraph()
        val newFunctionStage = propertyGraph.property((stage as? FunctionLevelStage) ?: FunctionLevelStage())
        val newDeclarationStage = propertyGraph.property((stage as? DeclarationLevelStage) ?: DeclarationLevelStage())
        val newDeclarationGraphStage = propertyGraph.property((stage as? DeclarationGraphStage) ?: DeclarationGraphStage())
        val newFileStage = propertyGraph.property((stage as? FileLevelStage) ?: FileLevelStage())

        return listOf(
            MinimizationStageData(
                name = "Function level stage",
                panel = functionLevelPanel(propertyGraph, newFunctionStage),
                stage = newFunctionStage,
                isDefaultSelected = stage is FunctionLevelStage,
            ),
            MinimizationStageData(
                name = "Declaration graph level stage",
                panel = declarationGraphLevelPanel(propertyGraph, newDeclarationGraphStage),
                stage = newDeclarationGraphStage,
                isDefaultSelected = stage is DeclarationGraphStage,
            ),
            MinimizationStageData(
                name = "Declaration level stage",
                panel = declarationLevelPanel(propertyGraph, newDeclarationStage),
                stage = newDeclarationStage,
                isDefaultSelected = stage is DeclarationLevelStage,
            ),
            MinimizationStageData(
                name = "File level stage",
                panel = fileLevelPanel(propertyGraph, newFileStage),
                stage = newFileStage,
                isDefaultSelected = stage is FileLevelStage,
            ),
        )
    }

    private data class MinimizationStageData(
        val name: String,
        val panel: DialogPanel,
        val stage: GraphProperty<out MinimizationStage>,
        val isDefaultSelected: Boolean,
    )
}
