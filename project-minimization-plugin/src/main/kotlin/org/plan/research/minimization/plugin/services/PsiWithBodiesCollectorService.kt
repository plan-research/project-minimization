package org.plan.research.minimization.plugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.plan.research.minimization.plugin.acceptOnAllKotlinFiles
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem
import org.plan.research.minimization.plugin.psi.BodyElementAcquiringKtVisitor

@Service(Service.Level.PROJECT)
class PsiWithBodiesCollectorService(private val rootProject: Project) {
    val psiElementsWithBody: List<PsiWithBodyDDItem>
        get() {
            val visitor = BodyElementAcquiringKtVisitor(rootProject)
            rootProject.acceptOnAllKotlinFiles(visitor)
            return visitor.collectedElements
        }
}