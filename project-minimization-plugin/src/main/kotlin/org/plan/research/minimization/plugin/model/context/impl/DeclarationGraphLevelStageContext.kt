package org.plan.research.minimization.plugin.model.context.impl

import org.plan.research.minimization.plugin.model.context.IJDDContextCloner
import org.plan.research.minimization.plugin.model.context.LightIJDDContext
import org.plan.research.minimization.plugin.model.context.WithImportRefCounterContext
import org.plan.research.minimization.plugin.model.context.WithInstanceLevelGraphContext
import org.plan.research.minimization.plugin.psi.KtSourceImportRefCounter
import org.plan.research.minimization.plugin.psi.graph.CondensedInstanceLevelGraph

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class DeclarationGraphLevelStageContext(
    projectDir: VirtualFile,
    indexProject: Project,
    originalProject: Project,
    override val importRefCounter: KtSourceImportRefCounter,
    override val graph: CondensedInstanceLevelGraph,
) : LightIJDDContext<DeclarationGraphLevelStageContext>(projectDir, indexProject, originalProject),
WithImportRefCounterContext<DeclarationGraphLevelStageContext>,
WithInstanceLevelGraphContext<DeclarationGraphLevelStageContext> {
    override fun copy(projectDir: VirtualFile): DeclarationGraphLevelStageContext =
        DeclarationGraphLevelStageContext(projectDir, indexProject, originalProject, importRefCounter, graph)

    override suspend fun clone(cloner: IJDDContextCloner): DeclarationGraphLevelStageContext? =
        cloner.cloneLight(this)

    override fun copy(importRefCounter: KtSourceImportRefCounter): DeclarationGraphLevelStageContext =
        DeclarationGraphLevelStageContext(projectDir, indexProject, originalProject, importRefCounter, graph)

    override fun copy(graph: CondensedInstanceLevelGraph): DeclarationGraphLevelStageContext =
        DeclarationGraphLevelStageContext(projectDir, indexProject, originalProject, importRefCounter, graph)
}
