package org.plan.research.minimization.plugin.model.context.impl

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.plan.research.minimization.plugin.model.context.LightIJDDContext
import org.plan.research.minimization.plugin.model.context.WithImportRefCounterContext
import org.plan.research.minimization.plugin.psi.KtSourceImportRefCounter

class DeclarationLevelStageContext(
    projectDir: VirtualFile,
    indexProject: Project,
    originalProject: Project,
    override val importRefCounter: KtSourceImportRefCounter,
) : LightIJDDContext<DeclarationLevelStageContext>(projectDir, indexProject, originalProject),
    WithImportRefCounterContext<DeclarationLevelStageContext> {

    override fun copy(projectDir: VirtualFile): DeclarationLevelStageContext =
        DeclarationLevelStageContext(projectDir, indexProject, originalProject, importRefCounter)

    override fun copy(importRefCounter: KtSourceImportRefCounter): DeclarationLevelStageContext =
        DeclarationLevelStageContext(projectDir, indexProject, originalProject, importRefCounter)

}