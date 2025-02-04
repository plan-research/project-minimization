@file:Suppress("UnstableApiUsage")

package org.plan.research.minimization.plugin.modification.psi.imports

import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.ContributedReferenceHost
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtLabelReferenceExpression
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtVisitorVoid

internal class UsedReferencesCollector(private val file: KtFile) {
    private val unresolvedNames: MutableList<Name> = mutableListOf()

    @Suppress("TYPE_ALIAS")
    private val usedDeclarations: HashMap<FqName, MutableList<Name>> = hashMapOf()
    private val aliases: Map<FqName, List<Name>> = collectImportAliases(file)

    fun KaSession.collectUsedReferences(): UsedReferencesResult = collectUsedReferencesRecursivelyFrom(file)

    fun KaSession.collectUsedReferencesRecursivelyFrom(element: KtElement): UsedReferencesResult {
        element.accept(object : KtVisitorVoid() {
            override fun visitElement(element: PsiElement) {
                ProgressIndicatorProvider.checkCanceled()
                element.acceptChildren(this)
            }

            override fun visitImportList(importList: KtImportList) {}

            override fun visitPackageDirective(directive: KtPackageDirective) {}

            override fun visitKtElement(element: KtElement) {
                super.visitKtElement(element)

                collectReferencesFrom(element)
            }
        })

        return UsedReferencesResult(usedDeclarations, unresolvedNames)
    }
    @Suppress("TOO_LONG_FUNCTION")
    private fun KaSession.collectReferencesFrom(element: KtElement) {
        // we ignore such elements because resolving them leads to UAST resolution,
        // and that in turn leads to KT-68601 when import optimization is called after move refactoring
        if (element is ContributedReferenceHost) {
            return
        }

        if (element is KtLabelReferenceExpression) {
            return
        }

        val references = element.references
            .filterIsInstance<KtReference>()
            .mapNotNull { CollectedReference.run { createFrom(it) } }

        if (references.isEmpty()) {
            return
        }

        for (reference in references) {
            ProgressIndicatorProvider.checkCanceled()

            val isResolved = reference.run { isResolved() }

            val names = reference.run { resolvesByNames() }
            if (!isResolved) {
                unresolvedNames += names
                continue
            }

            val symbols = reference.run { resolveToImportableSymbols() }

            for (symbol in symbols) {
                if (!symbol.run { isResolvedWithImport() }) {
                    continue
                }

                val importableName = symbol.run { computeImportableFqName() } ?: continue

                // Do not save symbols from the current package unless they are aliased
                if (importableName.parent() == file.packageFqName && importableName !in aliases) {
                    continue
                }

                ProgressIndicatorProvider.checkCanceled()

                val newNames = (aliases[importableName].orEmpty() + importableName.shortName()).intersect(names)
                usedDeclarations.getOrPut(importableName) { mutableListOf() } += newNames
            }
        }
    }
}

private fun collectImportAliases(file: KtFile): Map<FqName, List<Name>> = if (file.hasImportAlias()) {
    file.importDirectives
        .asSequence()
        .filter { !it.isAllUnder && it.alias != null }
        .mapNotNull { it.importPath }
        .groupBy(keySelector = { it.fqName }, valueTransform = { it.importedName as Name })
} else {
    emptyMap()
}
