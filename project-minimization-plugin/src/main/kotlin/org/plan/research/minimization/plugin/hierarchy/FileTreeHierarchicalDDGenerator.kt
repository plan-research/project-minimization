package org.plan.research.minimization.plugin.hierarchy

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.testFramework.utils.vfs.getPsiFile
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HDDLevel
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.plugin.model.PsiDDItem

class FileTreeHierarchicalDDGenerator(
    val project: Project,
    propertyTester: PropertyTester<PsiDDItem>
) : AbstractIJHierarchicalDDGenerator(propertyTester) {
    override suspend fun generateFirstLevel(): HDDLevel<PsiDDItem> {
        val projectRoot = project.guessProjectDir()
        val level = projectRoot?.let { listOf(projectRoot.getPsiFile(project)) } ?: emptyList()
        return HDDLevel(level.map { PsiDDItem(it) }, propertyTester)
    }
}