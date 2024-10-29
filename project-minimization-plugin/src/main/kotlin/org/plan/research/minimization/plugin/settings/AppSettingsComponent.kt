package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.MinimizationStage
import org.plan.research.minimization.plugin.model.state.*

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.*
import com.intellij.util.ui.FormBuilder
import java.util.*

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
    private val hierarchyStrategyComboBox = ComboBox(DefaultComboBoxModel(HierarchyCollectionStrategy.entries.toTypedArray()))
    private val ddAlgorithmComboBox = ComboBox(DefaultComboBoxModel(DDStrategy.entries.toTypedArray()))

    // Transformation List
    private val transformationCheckBoxes = TransformationDescriptors.entries.associateWith { descriptor ->
        JBCheckBox(descriptor.name.replace("_", " ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
    }

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

    var selectedHierarchyStrategy: HierarchyCollectionStrategy
        get() = hierarchyStrategyComboBox.selectedItem as HierarchyCollectionStrategy
        set(value) { hierarchyStrategyComboBox.selectedItem = value }

    var selectedDDStrategy: DDStrategy
        get() = ddAlgorithmComboBox.selectedItem as DDStrategy
        set(value) { ddAlgorithmComboBox.selectedItem = value }

    var transformations: List<TransformationDescriptors>
        get() = transformationCheckBoxes.filter { it.value.isSelected }.keys.toList()
        set(value) {
            transformationCheckBoxes.forEach { (descriptor, checkBox) ->
                checkBox.isSelected = descriptor in value
            }
        }

    var isHierarchyStrategyEnabled: Boolean
        get() = hierarchyStrategyComboBox.isEnabled
        set(value) {hierarchyStrategyComboBox.isEnabled = value}

    var isDDAlgorithmEnabled: Boolean
        get() = ddAlgorithmComboBox.isEnabled
        set(value) {ddAlgorithmComboBox.isEnabled = value}

    private lateinit var stagesPanel: JPanel
    private lateinit var fileStagePanel: JPanel
    private lateinit var fileStageSettings: JPanel

    init {
        stagesPanelInit()

        myMainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Compilation strategy:"), compilationStrategyComboBox, 1, false)
            .addLabeledComponent(JBLabel("Temporary project location:"), temporaryProjectLocationField, 1, false)
            .addLabeledComponent(JBLabel("Snapshot strategy:"), snapshotStrategyComboBox, 1, false)
            .addLabeledComponent(JBLabel("Exception comparing strategy:"), exceptionComparingStrategyComboBox, 1, false)
            .addSeparator()
            .addComponent(stagesPanel, 1)
            .addSeparator()
            .addLabeledComponent(JBLabel("Transformations:"), createTransformationPanel(), 1, false)
            .addSeparator()
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    private fun stagesPanelInit() {
        fileStagePanelInit()
        // add more stages here in future

        stagesPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Configuration mode:"), configModeComboBox, 1, false)
            .addComponent(fileStagePanel, 1)
            .panel

        configModeComboBox.addActionListener {
            updateStageSelectionVisibility()
            updateStages()
        }

        updateStageSelectionVisibility()
    }

    private fun fileStagePanelInit() {
        fileStageSettings = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(0, 20, 0, 0)

            add(FormBuilder.createFormBuilder()
                .addLabeledComponent(JBLabel("Hierarchy strategy:"), hierarchyStrategyComboBox, 1, false)
                .addLabeledComponent(JBLabel("DD algorithm:"), ddAlgorithmComboBox, 1, false)
                .panel,
            )
        }

        fileStagePanel = FormBuilder.createFormBuilder()
            .addComponent(fileStageCheckBox, 1)
            .addComponent(fileStageSettings, 1)
            .panel

        fileStageCheckBox.addActionListener {
            updateFileStageSettingsEnabled()
            updateStages()
        }
        hierarchyStrategyComboBox.addActionListener {
            updateStages()
        }
        ddAlgorithmComboBox.addActionListener {
            updateStages()
        }

        updateFileStageSettingsEnabled()
    }

    private fun updateFileStageSettingsEnabled() {
        val isEnabled = fileStageCheckBox.isSelected
        fileStageSettings.isEnabled = isEnabled
        hierarchyStrategyComboBox.isEnabled = isEnabled
        ddAlgorithmComboBox.isEnabled = isEnabled
    }

    private fun updateStageSelectionVisibility() {
        val isCustom = (configModeComboBox.selectedItem == StageConfigMode.CUSTOM)
        fileStagePanel.isVisible = isCustom
        fileStagePanel.isEnabled = isCustom
    }

    private fun updateStages() {
        stages = if (configModeComboBox.selectedItem == StageConfigMode.DEFAULT) {
            listOf(
                FileLevelStage(
                    hierarchyCollectionStrategy = HierarchyCollectionStrategy.FILE_TREE,
                    ddAlgorithm = DDStrategy.PROBABILISTIC_DD,
                ),
            )
        } else {
            mutableListOf<MinimizationStage>().apply {
                if (fileStageCheckBox.isSelected) {
                    add(FileLevelStage(
                        hierarchyStrategyComboBox.selectedItem as HierarchyCollectionStrategy,
                        ddAlgorithmComboBox.selectedItem as DDStrategy,
                    ),
                    )
                }
            }
        }
    }

    private fun createTransformationPanel(): JPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        transformationCheckBoxes.values.forEach { add(it) }
    }

    fun getPanel(): JPanel = myMainPanel

    fun getPreferredFocusedComponent(): JComponent = compilationStrategyComboBox
}
