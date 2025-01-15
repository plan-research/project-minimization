package org.plan.research.minimization.plugin.model.context.impl

import org.plan.research.minimization.plugin.model.context.LightIJDDContext

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class FileLevelStageContext(
    projectDir: VirtualFile,
    indexProject: Project,
    originalProject: Project,
) : LightIJDDContext<FileLevelStageContext>(projectDir, indexProject, originalProject) {
    override fun copy(projectDir: VirtualFile): FileLevelStageContext =
        FileLevelStageContext(projectDir, indexProject, originalProject)

    override fun getThis(): FileLevelStageContext = this
}
