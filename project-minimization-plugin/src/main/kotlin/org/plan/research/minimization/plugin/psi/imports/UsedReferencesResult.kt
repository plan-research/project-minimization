@file:Suppress("UnstableApiUsage")

package org.plan.research.minimization.plugin.psi.imports

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.resolve.ImportPath
import kotlin.collections.component1
import kotlin.collections.component2

class UsedReferencesResult internal constructor(
    usedDeclarationsList: Map<FqName, List<Name>>,
    unresolvedNamesList: List<Name>,
) {
    private val unresolvedNames = unresolvedNamesList.groupBy({ it }).mapValues { it.value.size }
    private val usedDeclarations = usedDeclarationsList
        .mapValues {
            it
                .value
                .groupBy({ it })
                .mapValues { it.value.size }
        }

    fun processImportDirectives(
        existingImports: List<KtImportDirective>,
        packageFqName: FqName,
        usedImportProcessor: (ImportPath, Int) -> Unit,
    ) {
        val explicitImports = existingImports.explicitImports

        val referencesEntities = usedDeclarations
            .filter { (fqName, referencedByNames) ->
                val isFromCurrentPackage = fqName.parentOrNull() == packageFqName
                val isAliasedImport = referencedByNames.keys.singleOrNull() != fqName.shortName()
                !isFromCurrentPackage || isAliasedImport
            }
        // if references has been x.y.z then we need an import like x.y.*
        val importRequiresStar = referencesEntities.keys.requiresStarImport(explicitImports)
        for (import in existingImports) {
            val importPath = import.importPath ?: continue
            val unresolvedUsedTimes = unresolvedNames[importPath.importedName] ?: 0
            val starUsed = importRequiresStar[importPath.fqName]
                ?.takeIf { unresolvedNames.isNotEmpty() && importPath.isAllUnder }
                ?: 0

            val referenceEntitiesUsed = referencesEntities[importPath.fqName]
                ?.get(importPath.importedName)
                ?: 0
            val usedTimes = unresolvedUsedTimes + starUsed + referenceEntitiesUsed
        if (usedTimes != 0) {
            usedImportProcessor(importPath, usedTimes)
        }
    }
}

private fun Iterable<FqName>.requiresStarImport(explicitImports: Set<FqName>) =
    asSequence()
        .filterNot { it in explicitImports }
        .mapNotNull { it.parentOrNull() }
        .filterNot { it.isRoot }
        .groupBy({ it })
        .mapValues { it.value.size }

private val List<KtImportDirective>.explicitImports
    get() = this.asSequence()
        .mapNotNull { it.importPath }
        .filter { !it.isAllUnder && !it.hasAlias() }
        .map { it.fqName }
        .toSet()
}
