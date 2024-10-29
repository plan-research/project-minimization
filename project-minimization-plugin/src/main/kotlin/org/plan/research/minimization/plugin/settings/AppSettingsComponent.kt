package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.MinimizationStage
import org.plan.research.minimization.plugin.model.state.*

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.*
import com.intellij.util.ui.FormBuilder

import javax.swing.*

enum class StageConfigMode {
    CUSTOM, DEFAULT
}

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
    private val configModeComboBox = ComboBox(DefaultComboBoxModel(StageConfigMode.entries.toTypedArray()))
    private val fileStageCheckBox = JBCheckBox("Enable file level stage")

    // Transformation List
    private val transformationListModel = DefaultListModel<TransformationDescriptors>()
    private val transformationList = JBList(transformationListModel).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
    }

    // parameters for TransformationDescriptors
    private val transformationComboBox = ComboBox(DefaultComboBoxModel(TransformationDescriptors.entries.toTypedArray()))

    var configMode: StageConfigMode
        get() = configModeComboBox.selectedItem as StageConfigMode
        set(value) { configModeComboBox.selectedItem = value }

    var isFileStageEnabled: Boolean
        get() = fileStageCheckBox.isSelected
        set(value) { fileStageCheckBox.isSelected = value }

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
            .addLabeledComponent(JBLabel("Configuration mode:"), configModeComboBox, 1, false)
            .addComponent(createStageSelectionPanel(), 1)
            .addSeparator()
            .addLabeledComponent(JBLabel("Transformations:"), JBScrollPane(transformationList), 1, false)
            .addLabeledComponent(JBLabel("Transformation descriptor:"), transformationComboBox, 1, false)
            .addComponent(createTransformationButtonPanel(), 1)
            .addSeparator()
            .addComponentFillVertically(JPanel(), 0)
            .panel

        configModeComboBox.addActionListener {
            updateStageSelectionVisibility()
            updateStages()
        }
        fileStageCheckBox.addActionListener {
            updateStages()
        }

        updateStageSelectionVisibility()
    }

    private fun createStageSelectionPanel(): JPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(fileStageCheckBox)
    }

    private fun updateStageSelectionVisibility() {
        val isCustom = configModeComboBox.selectedItem == StageConfigMode.CUSTOM
        fileStageCheckBox.isVisible = isCustom
        fileStageCheckBox.isEnabled = isCustom
    }

    private fun updateStages() {
        stages = if (configModeComboBox.selectedItem == StageConfigMode.DEFAULT) {
            defaultStages()
        } else {
            mutableListOf<MinimizationStage>().apply {
                if (fileStageCheckBox.isSelected) {
                    add(FileLevelStage(HierarchyCollectionStrategy.FILE_TREE, DDStrategy.PROBABILISTIC_DD))
                }
            }
        }
    }

    private fun defaultStages(): List<MinimizationStage> = listOf(
        FileLevelStage(
            hierarchyCollectionStrategy = HierarchyCollectionStrategy.FILE_TREE,
            ddAlgorithm = DDStrategy.PROBABILISTIC_DD,
        ),
    )

    private fun createTransformationButtonPanel(): JPanel {
        val buttonPanel = JPanel()

        buttonPanel.add(TransformationButtonFactory.createAddButton(transformationComboBox, transformationListModel))
        buttonPanel.add(TransformationButtonFactory.createRemoveButton(transformationList, transformationListModel))

        return buttonPanel
    }

    fun getPanel(): JPanel = myMainPanel

    fun getPreferredFocusedComponent(): JComponent = compilationStrategyComboBox
}
