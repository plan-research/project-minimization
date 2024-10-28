package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.model.MinimizationStage
import org.plan.research.minimization.plugin.model.state.*

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder

import javax.swing.*

@Suppress("NO_CORRESPONDING_PROPERTY")
class AppSettingsComponent {
    private val myMainPanel: JPanel

    // Fields from MinimizationPluginState
    private val compilationStrategyComboBox = ComboBox(DefaultComboBoxModel(CompilationStrategy.entries.toTypedArray()))
    private val temporaryProjectLocationField = JBTextField()
    private val snapshotStrategyComboBox = ComboBox(DefaultComboBoxModel(SnapshotStrategy.entries.toTypedArray()))
    private val exceptionComparingStrategyComboBox = ComboBox(DefaultComboBoxModel(ExceptionComparingStrategy.entries.toTypedArray()))

    // Stage List
    private val stageListModel = DefaultListModel<MinimizationStage>()
    private val stageList = JBList(stageListModel).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        dropMode = DropMode.INSERT
    }

    // parameters for MinimizationStage
    private val stageComboBox = ComboBox(DefaultComboBoxModel(MinimizationStage.stageTypes()))
    private val hierarchyStrategyComboBox = ComboBox(DefaultComboBoxModel(HierarchyCollectionStrategy.entries.toTypedArray()))
    private val ddAlgorithmComboBox = ComboBox(DefaultComboBoxModel(DDStrategy.entries.toTypedArray()))

    // Transformation List
    private val transformationListModel = DefaultListModel<TransformationDescriptors>()
    private val transformationList = JBList(transformationListModel).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
    }

    // parameters for TransformationDescriptors
    private val transformationComboBox = ComboBox(DefaultComboBoxModel(TransformationDescriptors.entries.toTypedArray()))

    var compilationStrategy: CompilationStrategy
        get() = compilationStrategyComboBox.selectedItem as CompilationStrategy
        set(value) {
            compilationStrategyComboBox.selectedItem = value}

    var temporaryProjectLocation: String
        get() = temporaryProjectLocationField.text
        set(value) {
            temporaryProjectLocationField.text = value
        }

    var snapshotStrategy: SnapshotStrategy
        get() = snapshotStrategyComboBox.selectedItem as SnapshotStrategy
        set(value) {
            snapshotStrategyComboBox.selectedItem = value
        }

    var exceptionComparingStrategy: ExceptionComparingStrategy
        get() = exceptionComparingStrategyComboBox.selectedItem as ExceptionComparingStrategy
        set(value) {exceptionComparingStrategyComboBox.selectedItem = value}

    var stages: List<MinimizationStage>
        get() = stageListModel.elements().toList()
        set(value) {
            stageListModel.clear()
            value.forEach { stageListModel.addElement(it) }
        }

    var transformations: List<TransformationDescriptors>
        get() = transformationListModel.elements().toList()
        set(value) {
            transformationListModel.clear()
            value.forEach { transformationListModel.addElement(it) }
        }

    init {
        myMainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Compilation strategy:"), compilationStrategyComboBox, 1, false)
            .addLabeledComponent(JBLabel("Temporary project location:"), temporaryProjectLocationField, 1, false)
            .addLabeledComponent(JBLabel("Snapshot strategy:"), snapshotStrategyComboBox, 1, false)
            .addLabeledComponent(JBLabel("Exception comparing strategy:"), exceptionComparingStrategyComboBox, 1, false)
            .addSeparator()
            .addLabeledComponent(JBLabel("Stages:"), JBScrollPane(stageList), 1)
            .addLabeledComponent(JBLabel("Stage:"), stageComboBox, 1)
            .addLabeledComponent(JBLabel("Hierarchy strategy:"), hierarchyStrategyComboBox, 1, false)
            .addLabeledComponent(JBLabel("DD algorithm:"), ddAlgorithmComboBox, 1, false)
            .addComponent(createStageButtonPanel(), 1)
            .addSeparator()
            .addLabeledComponent(JBLabel("Transformations:"), JBScrollPane(transformationList), 1, false)
            .addLabeledComponent(JBLabel("Transformation descriptor:"), transformationComboBox, 1, false)
            .addComponent(createTransformationButtonPanel(), 1)
            .addSeparator()
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    private fun createStageButtonPanel(): JPanel {
        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
        }

        buttonPanel.add(
            StageButtonFactory.createAddButton(
                stageComboBox,
                hierarchyStrategyComboBox,
                ddAlgorithmComboBox,
                stageListModel,
            ),
        )
        buttonPanel.add(StageButtonFactory.createRemoveButton(stageList, stageListModel))
        buttonPanel.add(StageButtonFactory.createMoveUpButton(stageList, stageListModel))
        buttonPanel.add(StageButtonFactory.createMoveDownButton(stageList, stageListModel))

        return buttonPanel
    }

    private fun createTransformationButtonPanel(): JPanel {
        val buttonPanel = JPanel()

        buttonPanel.add(TransformationButtonFactory.createAddButton(transformationComboBox, transformationListModel))
        buttonPanel.add(TransformationButtonFactory.createRemoveButton(transformationList, transformationListModel))

        return buttonPanel
    }

    fun getPanel(): JPanel = myMainPanel

    fun getPreferredFocusedComponent(): JComponent = compilationStrategyComboBox
}
