package org.plan.research.minimization.plugin.settings.ui

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import javax.swing.DefaultListModel
import javax.swing.ListSelectionModel
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

class IgnoreFilesSettingsProducer {
    private fun createFileChooser(projectDir: VirtualFile) = FileChooserDescriptor(
        true, true, false, true, false, true,
    ).apply {
        withRoots(projectDir)
    }

    fun createIgnorePathsPanel(projectDir: VirtualFile): PanelWithList<String> {
        val fileChooserDescriptor = createFileChooser(projectDir)

        val pathListModel = DefaultListModel<String>()

        val pathList = JBList(pathListModel).apply {
            emptyText.text = "Exclude files/directories"
            selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        }

        val panel = ToolbarDecorator.createDecorator(pathList)
            .disableUpDownActions()
            .setAddAction {
                val chosenFiles = FileChooser.chooseFiles(fileChooserDescriptor, null, null)
                for (file in chosenFiles) {
                    val relativePath = file.toNioPath()
                        .relativeTo(projectDir.toNioPath())
                        .pathString

                    if (pathListModel.indexOf(relativePath) == -1) {
                        pathListModel.addElement(relativePath)
                    }
                }
            }
            .setRemoveAction {
                val selected = pathList.selectedIndex
                pathListModel.remove(selected)
            }
            .createPanel()

        return panel to pathListModel
    }
}
