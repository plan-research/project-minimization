package org.plan.research.minimization.plugin.context.impl

import org.plan.research.minimization.plugin.context.IJDDContextCloner
import org.plan.research.minimization.plugin.context.LightIJDDContext
import org.plan.research.minimization.plugin.context.WithCallTraceParameterCacheContext
import org.plan.research.minimization.plugin.context.WithImportRefCounterContext
import org.plan.research.minimization.plugin.modification.graph.InstanceLevelGraph
import org.plan.research.minimization.plugin.modification.psi.CallTraceParameterCache
import org.plan.research.minimization.plugin.modification.psi.KtSourceImportRefCounter

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class DeclarationLevelStageContext(
    projectDir: VirtualFile,
    indexProject: Project,
    originalProject: Project,
    val graph: InstanceLevelGraph,
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
            graph,
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
            graph,
            importRefCounter,
            callTraceParameterCache,
        )

    override fun copy(callTraceParameterCache: CallTraceParameterCache): DeclarationLevelStageContext =
        DeclarationLevelStageContext(
            projectDir,
            indexProject,
            originalProject,
            graph,
            importRefCounter,
            callTraceParameterCache,
        )
}
