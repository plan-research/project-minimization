package org.plan.research.minimization.plugin.modification.psi

import org.plan.research.minimization.plugin.context.HeavyIJDDContext
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService

import arrow.core.raise.option
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.toNioPathOrNull
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtFile

import java.nio.file.Path

import kotlin.io.path.relativeTo
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toPersistentHashMap

class KtSourceImportRefCounter private constructor(private val refs: PersistentMap<Path, PsiImportRefCounter>) {
    operator fun get(path: Path) = option {
        val refCounter = refs[path]
        ensureNotNull(refCounter)
        refCounter
    }
    fun performAction(action: MutableMap<Path, PsiImportRefCounter>.() -> Unit): KtSourceImportRefCounter = KtSourceImportRefCounter(refs.mutate(action))

    companion object {
        suspend fun create(context: HeavyIJDDContext<*>) = option {
            smartReadAction(context.indexProject) {
                val vfs = service<MinimizationPsiManagerService>().findAllKotlinFilesInIndexProject(context)
                val ktFiles = vfs
                    .toList()
                    .map { it.toPsiFile(context.indexProject) }
                    .filterIsInstance<KtFile>()
                val projectDir = context.projectDir.toNioPathOrNull()
                ensureNotNull(projectDir)
                val psiRefCounters = ktFiles
                    .mapNotNull {
                        val localPath =
                            it.virtualFile.toNioPathOrNull()?.relativeTo(projectDir) ?: return@mapNotNull null
                        localPath to PsiImportRefCounter.create(it)
                    }
                    .toList()
                    .associate { it }
                    .toPersistentHashMap()
                KtSourceImportRefCounter(psiRefCounters)
            }
        }
    }
}
