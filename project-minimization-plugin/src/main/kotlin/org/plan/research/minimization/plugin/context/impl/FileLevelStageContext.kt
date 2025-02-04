package org.plan.research.minimization.plugin.context.impl

import org.plan.research.minimization.plugin.context.IJDDContextCloner
import org.plan.research.minimization.plugin.context.LightIJDDContext

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class FileLevelStageContext(
    projectDir: VirtualFile,
    indexProject: Project,
    originalProject: Project,
) : LightIJDDContext<FileLevelStageContext>(projectDir, indexProject, originalProject) {
    override fun copy(projectDir: VirtualFile): FileLevelStageContext =
        FileLevelStageContext(projectDir, indexProject, originalProject)

    override suspend fun clone(cloner: IJDDContextCloner): FileLevelStageContext? =
        cloner.cloneLight(this)
}
