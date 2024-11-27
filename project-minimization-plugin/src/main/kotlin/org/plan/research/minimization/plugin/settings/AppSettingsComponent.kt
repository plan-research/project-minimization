package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.FunctionLevelStage
import org.plan.research.minimization.plugin.model.MinimizationStage
import org.plan.research.minimization.plugin.model.state.*

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.*
import com.intellij.util.ui.FormBuilder

import java.util.*
import javax.swing.*

@Suppress("NO_CORRESPONDING_PROPERTY")
class AppSettingsComponent {
    private val myMainPanel: JPanel

    // Fields from MinimizationPluginState
    private val compilationStrategyComboBox = ComboBox(DefaultComboBoxModel(CompilationStrategy.entries.toTypedArray()))
    private val gradleTaskField = JBTextField().apply {
        emptyText.text = "build"
    }
    private val gradleOptionsField = JBTextField().apply {
        emptyText.text = "--offline --refresh-dependencies etc..."

        inputVerifier = object : InputVerifier() {
            override fun verify(input: JComponent): Boolean {
                val text = (input as JBTextField).text
                val invalidOptions = text.trim().replace("\\s+".toRegex(), " ").split(" ")
                    .map { it.trim() }
                    .filter { !isValidGradleOption(it) }

                if (invalidOptions.isNotEmpty()) {
                    JOptionPane.showMessageDialog(
                        input,
                        "Options: ${invalidOptions.joinToString(", ")} are invalid",
                        "Error",
                        JOptionPane.ERROR_MESSAGE,
                    )
                    return false
                }
                return true
            }

            fun isValidGradleOption(option: String): Boolean {
                val optionRegex = Regex("^\$|^--[a-zA-Z\\-]+(?:=[a-zA-Z0-9]+)?$")
                return optionRegex.matches(option)
            }
        }
    }
    private val temporaryProjectLocationField = JBTextField().apply {
        emptyText.text = "minimization-project-snapshots"
    }
    private val snapshotStrategyComboBox = ComboBox(DefaultComboBoxModel(SnapshotStrategy.entries.toTypedArray()))
    private val exceptionComparingStrategyComboBox =
        ComboBox(DefaultComboBoxModel(ExceptionComparingStrategy.entries.toTypedArray()))

    // Stage List
    private val functionStageCheckBox = JBCheckBox("Enable function level stage")
    private val functionDDAlgorithmComboBox = ComboBox(DefaultComboBoxModel(DDStrategy.entries.toTypedArray()))
    private val fileStageCheckBox = JBCheckBox("Enable file level stage")
    private val fileHierarchyStrategyComboBox =
        ComboBox(DefaultComboBoxModel(HierarchyCollectionStrategy.entries.toTypedArray()))
    private val fileDDAlgorithmComboBox = ComboBox(DefaultComboBoxModel(DDStrategy.entries.toTypedArray()))

    // Transformation List
    private val transformationCheckBoxes = TransformationDescriptors.entries.associateWith { descriptor ->
        JBCheckBox(descriptor.name.replace("_", " ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
    }

    var isFrozen: Boolean = false
        set(value) {
            field = value
            updateUIState()
        }

    var compilationStrategy: CompilationStrategy
        get() = compilationStrategyComboBox.selectedItem as CompilationStrategy
        set(value) {
            compilationStrategyComboBox.selectedItem = value
        }

    var gradleTask: String
        get() = gradleTaskField.text
        set(value) {
            gradleTaskField.text = value
        }

    var gradleOptions: List<String>
        get() = gradleOptionsField.text.trim()
            .replace("\\s+".toRegex(), " ")
            .split(" ")
            .filter { it.isNotBlank() }
        set(value) {
            gradleOptionsField.text = value.joinToString(" ")
        }

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
        set(value) {
            exceptionComparingStrategyComboBox.selectedItem = value
        }

    var stages: List<MinimizationStage>
        get() = buildList {
            if (functionStageCheckBox.isSelected) {
                add(FunctionLevelStage(functionDDStrategy))
            }
            if (fileStageCheckBox.isSelected) {
                add(
                    FileLevelStage(
                        fileHierarchyStrategy,
                        fileDDStrategy,
                    ),
                )
            }
        }
        set(value) {
            functionStageCheckBox.isSelected = value.any { it is FunctionLevelStage }
            functionDDStrategy = value
                .find { it is FunctionLevelStage }
                ?.let { (it as FunctionLevelStage).ddAlgorithm }
                ?: DDStrategy.PROBABILISTIC_DD

            fileStageCheckBox.isSelected = value.any { it is FileLevelStage }
            fileHierarchyStrategy = value
                .find { it is FileLevelStage }
                ?.let { (it as FileLevelStage).hierarchyCollectionStrategy }
                ?: HierarchyCollectionStrategy.FILE_TREE
            fileDDStrategy = value
                .find { it is FileLevelStage }
                ?.let { (it as FileLevelStage).ddAlgorithm }
                ?: DDStrategy.PROBABILISTIC_DD
            updateUIState()
        }

    var transformations: List<TransformationDescriptors>
        get() = transformationCheckBoxes.filter { it.value.isSelected }.keys.toList()
        set(value) {
            transformationCheckBoxes.forEach { (descriptor, checkBox) ->
                checkBox.isSelected = descriptor in value
            }
        }

    private var isFunctionStageEnabled: Boolean
        get() = functionStageCheckBox.isSelected
        set(value) {
            functionStageCheckBox.isSelected = value
        }

    private var functionDDStrategy: DDStrategy
        get() = functionDDAlgorithmComboBox.selectedItem as DDStrategy
        set(value) {
            functionDDAlgorithmComboBox.selectedItem = value
        }

    private var isFileStageEnabled: Boolean
        get() = fileStageCheckBox.isSelected
        set(value) {
            fileStageCheckBox.isSelected = value
        }

    private var fileHierarchyStrategy: HierarchyCollectionStrategy
        get() = fileHierarchyStrategyComboBox.selectedItem as HierarchyCollectionStrategy
        set(value) {
            fileHierarchyStrategyComboBox.selectedItem = value
        }

    private var fileDDStrategy: DDStrategy
        get() = fileDDAlgorithmComboBox.selectedItem as DDStrategy
        set(value) {
            fileDDAlgorithmComboBox.selectedItem = value
        }

    private lateinit var stagesPanel: JPanel
    private lateinit var functionStagePanel: JPanel
    private lateinit var functionStageSettings: JPanel
    private lateinit var fileStagePanel: JPanel
    private lateinit var fileStageSettings: JPanel

    init {
        stagesPanelInit()

        myMainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Compilation strategy:"), compilationStrategyComboBox, 1, false)
            .addLabeledComponent(JBLabel("Gradle task:"), gradleTaskField, 1, false)
            .addLabeledComponent(JBLabel("Gradle options:"), gradleOptionsField, 1, false)
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

        updateUIState()
    }

    private fun updateUIState() {
        compilationStrategyComboBox.isEnabled = !isFrozen
        gradleTaskField.isEnabled = !isFrozen
        gradleOptionsField.isEnabled = !isFrozen
        temporaryProjectLocationField.isEnabled = !isFrozen
        snapshotStrategyComboBox.isEnabled = !isFrozen
        exceptionComparingStrategyComboBox.isEnabled = !isFrozen
        functionStageCheckBox.isEnabled = !isFrozen
        functionDDAlgorithmComboBox.isEnabled = !isFrozen && isFunctionStageEnabled
        fileStageCheckBox.isEnabled = !isFrozen
        fileHierarchyStrategyComboBox.isEnabled = !isFrozen && isFileStageEnabled
        fileDDAlgorithmComboBox.isEnabled = !isFrozen && isFileStageEnabled
        transformationCheckBoxes.values.forEach { it.isEnabled = !isFrozen }
    }

    private fun stagesPanelInit() {
        functionStagePanelInit()
        fileStagePanelInit()
        // add more stages here in future

        stagesPanel = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("Stages settings"))
            .addComponent(functionStagePanel, 1)
            .addComponent(fileStagePanel, 1)
            .panel
    }

    private fun functionStagePanelInit() {
        functionStageSettings = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(0, 20, 0, 0)

            add(
                FormBuilder.createFormBuilder()
                    .addLabeledComponent(JBLabel("DD algorithm:"), functionDDAlgorithmComboBox, 1, false)
                    .panel,
            )
        }

        functionStagePanel = FormBuilder.createFormBuilder()
            .addComponent(functionStageCheckBox, 1)
            .addComponent(functionStageSettings, 1)
            .panel

        functionStageCheckBox.addActionListener {
            updateFunctionStageSettingsEnabled()
        }

        updateFunctionStageSettingsEnabled()
    }

    private fun fileStagePanelInit() {
        fileStageSettings = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(0, 20, 0, 0)

            add(
                FormBuilder.createFormBuilder()
                    .addLabeledComponent(JBLabel("Hierarchy strategy:"), fileHierarchyStrategyComboBox, 1, false)
                    .addLabeledComponent(JBLabel("DD algorithm:"), fileDDAlgorithmComboBox, 1, false)
                    .panel,
            )
        }

        fileStagePanel = FormBuilder.createFormBuilder()
            .addComponent(fileStageCheckBox, 1)
            .addComponent(fileStageSettings, 1)
            .panel

        fileStageCheckBox.addActionListener {
            updateFileStageSettingsEnabled()
        }

        updateFileStageSettingsEnabled()
    }

    private fun updateFileStageSettingsEnabled() {
        val isEnabled = fileStageCheckBox.isSelected
        fileHierarchyStrategyComboBox.isEnabled = isEnabled
        fileDDAlgorithmComboBox.isEnabled = isEnabled
    }

    private fun updateFunctionStageSettingsEnabled() {
        val isEnabled = functionStageCheckBox.isSelected
        functionDDAlgorithmComboBox.isEnabled = isEnabled
    }

    private fun createTransformationPanel(): JPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        transformationCheckBoxes.values.forEach { add(it) }
    }

    fun getPanel(): JPanel = myMainPanel

    fun getPreferredFocusedComponent(): JComponent = compilationStrategyComboBox
}
