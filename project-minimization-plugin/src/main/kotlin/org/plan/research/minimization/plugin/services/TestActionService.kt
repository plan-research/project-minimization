package org.plan.research.minimization.plugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.plan.research.minimization.plugin.model.LightIJDDContext
import org.plan.research.minimization.plugin.model.PsiStubDDItem

@Service(Service.Level.PROJECT)
class TestActionService(project: Project, private val cs: CoroutineScope) {
    private val context = LightIJDDContext(project)
    fun dumpDeletableElements(callback: (List<PsiStubDDItem>) -> Unit) = cs.launch {
        val psiManager = service<MinimizationPsiManagerService>()
        val items = psiManager.findDeletablePsiItems(context)
        callback(items)
    }
}