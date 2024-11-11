package org.plan.research.minimization.plugin.prototype.slicing

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.plan.research.minimization.plugin.model.LightIJDDContext
import kotlin.io.path.name
import kotlin.io.path.writeText

class TestSlicingAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val currentFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val slicingService = service<SlicingService>()
        slicingService.dumpGraph(LightIJDDContext(project), currentFile) {
            val dotContent = it.toDot()
            val currentPath = currentFile.virtualFile.toNioPath()
            val basePath = currentPath.parent
            val dumpFile = basePath.resolve("${currentPath.name}.dot")
            withContext(Dispatchers.IO) { dumpFile.writeText(dotContent) }
        }
    }
}