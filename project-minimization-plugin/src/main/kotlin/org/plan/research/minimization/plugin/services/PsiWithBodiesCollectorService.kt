package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.acceptOnAllKotlinFiles
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem
import org.plan.research.minimization.plugin.psi.BodyElementAcquiringKtVisitor

import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class PsiWithBodiesCollectorService(private val rootProject: Project) {
    suspend fun getElementsWithBody(): List<PsiWithBodyDDItem> {
        val visitor = BodyElementAcquiringKtVisitor(rootProject)
        readAction {
            rootProject.acceptOnAllKotlinFiles(visitor)
        }
        return visitor.collectedElements
    }
}
