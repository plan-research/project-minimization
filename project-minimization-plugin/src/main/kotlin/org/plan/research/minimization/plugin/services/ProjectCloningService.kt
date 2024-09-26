package org.plan.research.minimization.plugin.services

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.findOrCreateDirectory
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings
import java.util.*

@Service(Service.Level.PROJECT)
class ProjectCloningService(private val rootProject: Project) {
    private val tempProjectsDirectoryName = rootProject
        .service<MinimizationPluginSettings>()
        .state
        .temporaryProjectLocation
        ?: ""
    private val tempProjectLocation = rootProject
        .guessProjectDir()
        ?.findOrCreateDirectory(tempProjectsDirectoryName)
        ?: TODO()

    fun clone(project: Project): Project? {
        val clonedProjectPath = tempProjectLocation.findOrCreateDirectory(UUID.randomUUID().toString())
        val projectRoot = project.guessProjectDir() ?: return null
        VfsUtil.copyDirectory(this, projectRoot, clonedProjectPath, null)
        return ProjectUtil.openOrImport(clonedProjectPath.toNioPath())
    }

    fun clone(project: Project, items: List<PsiElement>): Project? {
        val filesToCopy = items.filterIsInstance<PsiFile>().map { it.virtualFile }.toSet() +
                          items.filterIsInstance<PsiDirectory>().map { it.virtualFile }
        val clonedProjectPath = tempProjectLocation.findOrCreateDirectory(UUID.randomUUID().toString())
        val projectRoot = project.guessProjectDir() ?: return null
        VfsUtil.copyDirectory(this, projectRoot, clonedProjectPath) { it in filesToCopy }
        return ProjectUtil.openOrImport(clonedProjectPath.toNioPath())

    }
}