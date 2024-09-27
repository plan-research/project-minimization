package org.plan.research.minimization.plugin.hierarchy

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.plugin.model.PsiDDItem

class FileTreeHierarchicalDDGenerator(
    val project: Project,
    propertyTester: PropertyTester<PsiDDItem>
) : AbstractIJHierarchicalDDGenerator(propertyTester) {
    override suspend fun generateFirstLevel(): HDDLevel<PsiDDItem> {
        val projectRoot = project.guessProjectDir()
        val psiManager = PsiManager.getInstance(project)
        val level = projectRoot?.let { listOfNotNull(psiManager.findDirectory(it)) } ?: emptyList()
        return HDDLevel(level.map { PsiDDItem(it) }, propertyTester)
    }

    override suspend fun generateNextLevel(minimizedLevel: List<PsiDDItem>): HDDLevel<PsiDDItem>? {
        val superResult = super.generateNextLevel(minimizedLevel)
        return superResult
            ?.copy(items = superResult.items.filter { it.psi is PsiDirectory || it.psi is PsiFile })
            ?.takeIf { it.items.isNotEmpty() }
    }
}