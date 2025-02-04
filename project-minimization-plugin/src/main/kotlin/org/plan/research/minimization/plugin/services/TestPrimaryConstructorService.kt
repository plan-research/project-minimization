package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.modification.psi.PsiUtils

import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.searches.ReferencesSearch
import mu.KotlinLogging
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class TestPrimaryConstructorService(private val project: Project, private val cs: CoroutineScope) {
    private val logger = KotlinLogging.logger { }
    fun printAllConstructorCalls(context: IJDDContext) = cs.launch {
        val manager = service<MinimizationPsiManagerService>()
        val allClasses = readAction {
            manager.findPsiInKotlinFiles<KtClass>(context, listOf(KtClass::class.java))
        }
        allClasses.forEach { ktClass ->
            val primaryConstructor = readAction { ktClass.primaryConstructor } ?: return@forEach
            val references = ReferencesSearch.search(primaryConstructor)
            references.forEach { ref ->
                val callExpression = ref.element.parent as? KtCallExpression ?: return@forEach
                val builtItem = readAction { PsiUtils.buildCompositeStubItem(context, callExpression) }
                readAction {
                    logger.debug { "Found primary constructor call for class: ${ktClass.name}. Path to item: $builtItem" }
                }
            }
        }
    }
}
