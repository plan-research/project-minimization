package org.plan.research.minimization.plugin.context.impl

import org.plan.research.minimization.plugin.context.IJDDContextCloner
import org.plan.research.minimization.plugin.context.LightIJDDContext

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class FunctionLevelStageContext(
    projectDir: VirtualFile,
    indexProject: Project,
    originalProject: Project,
) : LightIJDDContext<FunctionLevelStageContext>(projectDir, indexProject, originalProject) {
    override fun copy(projectDir: VirtualFile): FunctionLevelStageContext =
        FunctionLevelStageContext(projectDir, indexProject, originalProject)

    override suspend fun clone(cloner: IJDDContextCloner): FunctionLevelStageContext? =
        cloner.cloneLight(this)
}
