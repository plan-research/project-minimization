package org.plan.research.minimization.plugin.model.context.impl

import org.plan.research.minimization.plugin.model.context.LightIJDDContext

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class FunctionLevelStageContext(
    projectDir: VirtualFile,
    indexProject: Project,
    originalProject: Project,
) : LightIJDDContext<FunctionLevelStageContext>(projectDir, indexProject, originalProject) {
    override fun copy(projectDir: VirtualFile): FunctionLevelStageContext =
        FunctionLevelStageContext(projectDir, indexProject, originalProject)

    override fun getThis(): FunctionLevelStageContext = this
}
