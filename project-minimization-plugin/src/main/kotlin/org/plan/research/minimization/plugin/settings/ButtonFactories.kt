package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.MinimizationStage
import org.plan.research.minimization.plugin.model.state.DDStrategy
import org.plan.research.minimization.plugin.model.state.HierarchyCollectionStrategy
import org.plan.research.minimization.plugin.model.state.TransformationDescriptors

import com.intellij.openapi.ui.ComboBox

import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JList

object StageButtonFactory {
    private fun nameToMinStage(s: String, hierarchyStrategy: HierarchyCollectionStrategy, ddAlgorithm: DDStrategy): MinimizationStage = when (s) {
        "FileLevelStage" -> FileLevelStage(
            hierarchyCollectionStrategy = hierarchyStrategy,
            ddAlgorithm = ddAlgorithm,
        )
        else -> FileLevelStage(
            hierarchyCollectionStrategy = hierarchyStrategy,
            ddAlgorithm = ddAlgorithm,
        )
    }

    fun createAddButton(
        stageComboBox: JComboBox<String>,
        hierarchyStrategyComboBox: JComboBox<HierarchyCollectionStrategy>,
        ddAlgorithmComboBox: JComboBox<DDStrategy>,
        stageListModel: DefaultListModel<MinimizationStage>,
    ): JButton = JButton("Add Stage").apply {
        addActionListener {
            val hierarchyStrategy = hierarchyStrategyComboBox.selectedItem as HierarchyCollectionStrategy
            val ddAlgorithm = ddAlgorithmComboBox.selectedItem as DDStrategy
            val selectedStage = nameToMinStage(stageComboBox.selectedItem as String, hierarchyStrategy, ddAlgorithm)
            stageListModel.addElement(selectedStage)
        }
    }

    fun createRemoveButton(
        stageList: JList<MinimizationStage>,
        stageListModel: DefaultListModel<MinimizationStage>,
    ): JButton = JButton("Remove Selected").apply {
        addActionListener {
            val selectedIndex = stageList.selectedIndex
            if (selectedIndex != -1) {
                stageListModel.remove(selectedIndex)
            }
        }
    }

    fun createMoveUpButton(
        stageList: JList<MinimizationStage>,
        stageListModel: DefaultListModel<MinimizationStage>,
    ): JButton = JButton("Move Up").apply {
        addActionListener {
            val selectedIndex = stageList.selectedIndex
            if (selectedIndex > 0) {
                val item = stageListModel.getElementAt(selectedIndex)
                stageListModel.remove(selectedIndex)
                stageListModel.add(selectedIndex - 1, item)
                stageList.selectedIndex = selectedIndex - 1
            }
        }
    }

    fun createMoveDownButton(
        stageList: JList<MinimizationStage>,
        stageListModel: DefaultListModel<MinimizationStage>,
    ): JButton = JButton("Move Down").apply {
        addActionListener {
            val selectedIndex = stageList.selectedIndex
            if (selectedIndex < stageListModel.size - 1) {
                val item = stageListModel.getElementAt(selectedIndex)
                stageListModel.remove(selectedIndex)
                stageListModel.add(selectedIndex + 1, item)
                stageList.selectedIndex = selectedIndex + 1
            }
        }
    }
}

object TransformationButtonFactory {
    fun createAddButton(
        transformationComboBox: ComboBox<TransformationDescriptors>,
        transformationListModel: DefaultListModel<TransformationDescriptors>,
    ): JButton = JButton("Add Transformation").apply {
        addActionListener {
            val selectedTransformation = transformationComboBox.selectedItem as TransformationDescriptors
            if (!transformationListModel.contains(selectedTransformation)) {
                transformationListModel.addElement(selectedTransformation)
            }
        }
    }

    fun createRemoveButton(
        transformationList: JList<TransformationDescriptors>,
        transformationListModel: DefaultListModel<TransformationDescriptors>,
    ): JButton = JButton("Remove Selected").apply {
        addActionListener {
            val selectedIndex = transformationList.selectedIndex
            if (selectedIndex != -1) {
                transformationListModel.remove(selectedIndex)
            }
        }
    }
}
