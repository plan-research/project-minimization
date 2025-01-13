@file:Suppress("UnstableApiUsage")

package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.psi.imports.UsedReferencesCollector

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.resolve.ImportPath

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toPersistentMap
import org.plan.research.minimization.plugin.model.IJDDContext

class PsiImportRefCounter private constructor(private val counter: PersistentMap<ImportPath, Int>) {
    suspend fun decreaseCounterBasedOnKtElement(context: IJDDContext, element: KtElement): PsiImportRefCounter {
        val ktFile = readAction { element.containingKtFile }
        val usedReferences = smartReadAction(context.indexProject) {
            analyze(element) {
                UsedReferencesCollector(ktFile).run { collectUsedReferencesRecursivelyFrom(element) }
            }
        }
        return PsiImportRefCounter(smartReadAction(context.indexProject) {
            counter.mutate { obj ->
                usedReferences.processImportDirectives(
                    ktFile.importDirectives,
                    ktFile.packageFqName,
                ) { importPath, times -> obj.merge(importPath, times, Int::minus) }
            }
        })
    }

    @RequiresReadLock
    fun getUnusedImports(imports: List<KtImportDirective>) = imports
        .filter {
            val importPath = it.importPath ?: return@filter false
            counter[importPath] == 0
        }

    fun purgeUnusedImports(): PsiImportRefCounter = PsiImportRefCounter(
        counter.mutate { obj -> obj.entries.removeIf { it.value == 0 } },
    )

    companion object {
        @RequiresReadLock
        fun create(ktFile: KtFile): PsiImportRefCounter {
            val usedReferences = analyze(ktFile) {
                UsedReferencesCollector(ktFile).run { collectUsedReferences() }
            }
            val counter = mutableMapOf<ImportPath, Int>()
            usedReferences.processImportDirectives(
                ktFile.importDirectives,
                ktFile.packageFqName,
            ) { importPath, times -> counter.merge(importPath, times, Int::plus) }
            return PsiImportRefCounter(counter.toPersistentMap())
        }
    }
}
