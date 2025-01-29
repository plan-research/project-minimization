package org.plan.research.minimization.plugin.model.context.impl

import org.plan.research.minimization.plugin.model.context.IJDDContextCloner
import org.plan.research.minimization.plugin.model.context.LightIJDDContext
import org.plan.research.minimization.plugin.model.context.WithCallTraceParameterCacheContext
import org.plan.research.minimization.plugin.model.context.WithImportRefCounterContext
import org.plan.research.minimization.plugin.psi.CallTraceParameterCache
import org.plan.research.minimization.plugin.psi.KtSourceImportRefCounter

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class DeclarationLevelStageContext(
    projectDir: VirtualFile,
    indexProject: Project,
    originalProject: Project,
    override val importRefCounter: KtSourceImportRefCounter,
    override val callTraceParameterCache: CallTraceParameterCache,
) : LightIJDDContext<DeclarationLevelStageContext>(projectDir, indexProject, originalProject),
WithImportRefCounterContext<DeclarationLevelStageContext>,
WithCallTraceParameterCacheContext<DeclarationLevelStageContext> {
    override fun copy(projectDir: VirtualFile): DeclarationLevelStageContext =
        DeclarationLevelStageContext(
            projectDir,
            indexProject,
            originalProject,
            importRefCounter,
            callTraceParameterCache,
        )

    override suspend fun clone(cloner: IJDDContextCloner): DeclarationLevelStageContext? =
        cloner.cloneLight(this)

    override fun copy(importRefCounter: KtSourceImportRefCounter): DeclarationLevelStageContext =
        DeclarationLevelStageContext(
            projectDir,
            indexProject,
            originalProject,
            importRefCounter,
            callTraceParameterCache,
        )

    override fun copy(callTraceParameterCache: CallTraceParameterCache): DeclarationLevelStageContext =
        DeclarationLevelStageContext(
            projectDir,
            indexProject,
            originalProject,
            importRefCounter,
            callTraceParameterCache,
        )
}
