package org.plan.research.minimization.plugin.settings.ui

import org.plan.research.minimization.plugin.model.DeclarationLevelStage
import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.FunctionLevelStage
import org.plan.research.minimization.plugin.model.MinimizationStage
import org.plan.research.minimization.plugin.model.state.DDStrategy
import org.plan.research.minimization.plugin.model.state.HierarchyCollectionStrategy

import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.observable.properties.PropertyGraph
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
        comboBox(DDStrategy.entries)
            .bindItem(ddAlgorithm)

        val commentText = graph.property(ddAlgorithm.get().getComment()).apply {
            dependsOn(ddAlgorithm) { ddAlgorithm.get().getComment() }
        }

        comment(commentText.get()).bindText(commentText)
    }

    private fun functionLevelPanel(
        graph: PropertyGraph,
        stageProperty: ObservableMutableProperty<FunctionLevelStage>,
    ): DialogPanel = panel {
        val ddAlgorithm = graph.property(stageProperty.get().ddAlgorithm).apply {
            afterChange {
                stageProperty.set(stageProperty.get().copy(ddAlgorithm = it))
            }
        }
        row("Description") {
            text("The algorithm replaces function's bodies with `TODO` statements.")
        }
        row("Function level settings:") { }
        indent {
            row("Minimization strategy:") {
                strategy(graph, ddAlgorithm)
            }
        }
    }

    private fun declarationLevelPanel(
        graph: PropertyGraph,
        stageProperty: ObservableMutableProperty<DeclarationLevelStage>,
    ): DialogPanel = panel {
        val ddAlgorithm = graph.property(stageProperty.get().ddAlgorithm).apply {
            afterChange {
                stageProperty.set(stageProperty.get().copy(ddAlgorithm = it))
            }
        }
        val maxDepth = graph.property(stageProperty.get().depthThreshold).apply { 
            afterChange { 
                stageProperty.set(stageProperty.get().copy(depthThreshold = it))
            }
        }
        row {
            text("⚠ This algorithm will increase execution time dramatically!").applyToComponent {
                foreground = JBColor.RED
            }
        }
        row("Description:") {
            text("The algorithm removes declarations, e.g. classes, functions and fields.")
        }
        row("Declaration level settings:") { }
        indent {
            row("Minimization strategy:") {
                strategy(graph, ddAlgorithm)
            }
            row("A max depth for the algorithm:") {
                intTextField(range = 1..Int.MAX_VALUE)
                    .bindIntText(maxDepth)
            }
        }
    }

    private fun fileLevelPanel(
        graph: PropertyGraph,
        stageProperty: ObservableMutableProperty<FileLevelStage>,
    ): DialogPanel = panel {
        val hierarchyStrategy = graph.property(stageProperty.get().hierarchyCollectionStrategy).apply {
            afterChange {
                stageProperty.set(stageProperty.get().copy(hierarchyCollectionStrategy = it))
            }
        }
        val ddAlgorithm = graph.property(stageProperty.get().ddAlgorithm).apply {
            afterChange {
                stageProperty.set(stageProperty.get().copy(ddAlgorithm = it))
            }
        }
        row("Description:") {
            text("The algorithm deletes files")
        }
        row("File level settings:") { }
        indent {
            row("Hierarchy strategy:") {
                comboBox(HierarchyCollectionStrategy.entries).bindItem(hierarchyStrategy)
            }
            row("Minimization strategy:") {
                strategy(graph, ddAlgorithm)
            }
        }
    }

    private fun showAddDialog(stage: MinimizationStage? = null): MinimizationStage? {
        var current: ObservableMutableProperty<out MinimizationStage>? = null
        val items = createStagesData(stage)
        val dialogPanel = addStageDialogPanel(items) { current = it }

        val dialog = dialog("Stage settings", dialogPanel) {
            dialogPanel.validateAll().takeIf { it.isNotEmpty() }
        }

        return if (dialog.showAndGet()) {
            current?.get()
        } else {
            null
        }
    }

    private fun addStageDialogPanel(
        items: List<MinimizationStageData>,
        setCurrent: (ObservableMutableProperty<out MinimizationStage>?) -> Unit,
    ): DialogPanel {
        lateinit var selected: Cell<ComboBox<String>>

        return panel {
            val index = items.indexOfFirst { it.isFirstSelected }

            row("Stage:") {
                selected = comboBox(items.map { it.name }).applyToComponent { selectedIndex = index }
                    .applyToComponent { selectedIndex = index }.validationOnApply {
                        if (it.selectedIndex == 0) {
                            error("Please select a valid stage.")
                        } else {
                            null
                        }
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

    private fun createStagesData(
        stage: MinimizationStage?,
    ): List<MinimizationStageData> {
        val propertyGraph = PropertyGraph()
        val newFunctionStage = propertyGraph.property((stage as? FunctionLevelStage) ?: FunctionLevelStage())
        val newDeclarationStage = propertyGraph.property((stage as? DeclarationLevelStage) ?: DeclarationLevelStage())
        val newFileStage = propertyGraph.property((stage as? FileLevelStage) ?: FileLevelStage())

        return listOf(
            MinimizationStageData(
                name = "Select stage",
                panel = DialogPanel(),
                stage = null,
                isFirstSelected = stage == null,
            ),
            MinimizationStageData(
                name = "Function level stage",
                panel = functionLevelPanel(propertyGraph, newFunctionStage),
                stage = newFunctionStage,
                isFirstSelected = stage is FunctionLevelStage,
            ),
            MinimizationStageData(
                name = "Declaration level stage",
                panel = declarationLevelPanel(propertyGraph, newDeclarationStage),
                stage = newDeclarationStage,
                isFirstSelected = stage is DeclarationLevelStage,
            ),
            MinimizationStageData(
                name = "File level stage",
                panel = fileLevelPanel(propertyGraph, newFileStage),
                stage = newFileStage,
                isFirstSelected = stage is FileLevelStage,
            ),
        )
    }

    private data class MinimizationStageData(
        val name: String,
        val panel: DialogPanel,
        val stage: ObservableMutableProperty<out MinimizationStage>?,
        val isFirstSelected: Boolean,
    )
}
