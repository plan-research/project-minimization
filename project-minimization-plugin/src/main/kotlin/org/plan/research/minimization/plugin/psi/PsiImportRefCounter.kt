@file:Suppress("UnstableApiUsage")

package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.psi.imports.UsedReferencesCollector

import com.intellij.openapi.application.readAction
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.resolve.ImportPath

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toPersistentMap

class PsiImportRefCounter private constructor(private val counter: PersistentMap<ImportPath, Int>) {
    suspend fun decreaseCounterBasedOnKtElement(element: KtElement): PsiImportRefCounter {
        val ktFile = readAction { element.containingKtFile }
        val usedReferences = readAction {
            analyze(element) {
                UsedReferencesCollector(ktFile).run { collectUsedReferencesRecursivelyFrom(element) }
            }
        }
        return PsiImportRefCounter(readAction {
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
        suspend fun create(ktFile: KtFile): PsiImportRefCounter {
            val usedReferences = readAction {
                analyze(ktFile) {
                    UsedReferencesCollector(ktFile).run { collectUsedReferences() }
                }
            }
            val counter = mutableMapOf<ImportPath, Int>()
            readAction {
                usedReferences.processImportDirectives(
                    ktFile.importDirectives,
                    ktFile.packageFqName,
                ) { importPath, times -> counter.merge(importPath, times, Int::plus) }
            }
            return PsiImportRefCounter(counter.toPersistentMap())
        }
    }
}