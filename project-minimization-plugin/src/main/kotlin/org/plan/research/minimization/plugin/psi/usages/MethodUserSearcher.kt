package org.plan.research.minimization.plugin.psi.usages

import org.plan.research.minimization.plugin.model.context.IJDDContext

import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.light.LightMemberReference
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usageView.UsageInfo
import com.intellij.util.CommonProcessors
import com.intellij.util.FilteredQuery
import com.intellij.util.Processor
import com.intellij.util.Query
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.concurrency.annotations.RequiresReadLockAbsence
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.idea.base.searching.usages.KotlinReferencePreservingUsageInfo
import org.jetbrains.kotlin.idea.base.searching.usages.KotlinReferenceUsageInfo
import org.jetbrains.kotlin.idea.base.util.excludeKotlinSources
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.KotlinSearchUsagesSupport.SearchUtils.dataClassComponentMethodName
import org.jetbrains.kotlin.idea.search.KotlinSearchUsagesSupport.SearchUtils.filterDataClassComponentsIfDisabled
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchOptions
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchParameters
import org.jetbrains.kotlin.idea.search.isImportUsage
import org.jetbrains.kotlin.idea.search.usagesSearch.buildProcessDelegationCallKotlinConstructorUsagesTask
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.containingClass

private typealias ProcessorFunction = () -> Boolean

/**
 * A class that searches all the usages of a specified kotlin call element.
 * It could be a constructor, function, etc.
 *
 * Implementation is mostly based on [org.jetbrains.kotlin.idea.base.searching.usages.handlers.KotlinFindMemberUsagesHandler.MySearcher.buildTaskList]
 */
class MethodUserSearcher(
    private val sourcePsiElement: PsiElement,
    private val context: IJDDContext,
    private val options: FindUsagesOptions = FindUsagesOptions(context.indexProject),
    private val isSearchOnlyKotlin: Boolean = true,
) {
    val kotlinSearchOptions = KotlinReferencesSearchOptions().copy(
        acceptOverloads = true,
        acceptCallableOverrides = true,
        searchForExpectedUsages = true,
        searchForOperatorConventions = false,
        searchNamedArguments = true,
        searchForComponentConventions = false,
        acceptCompanionObjectMembers = true,
        acceptImportAlias = false,
        acceptExtensionsOfDeclarationClass = true,
    )
    private val tasks = mutableListOf<ProcessorFunction>()

    @RequiresReadLock
    fun buildTaskList(processor: Processor<in UsageInfo>) {
        val referenceProcessor = createReferenceProcessor(processor)
        buildSearchUsagesTask(referenceProcessor)

        if (sourcePsiElement is KtElement && !isSearchOnlyKotlin) {
            buildSearchInNonKotlinFilesTask(referenceProcessor)
        }
        buildConstructorLookupTask(referenceProcessor)
    }

    @RequiresReadLockAbsence
    fun executeTasks() = tasks.all { it() }

    private fun buildSearchUsagesTask(referenceProcessor: Processor<PsiReference>) {
        val searchParameters =
            KotlinReferencesSearchParameters(sourcePsiElement, options.searchScope, kotlinOptions = kotlinSearchOptions)
        tasks.add {
            ReferencesSearch.search(searchParameters)
                .applyFilters()
                .forEach(referenceProcessor)
        }
    }

    private fun buildSearchInNonKotlinFilesTask(referencesProcessor: Processor<PsiReference>) {
        val nonKotlinSources = options.searchScope.excludeKotlinSources(context.indexProject)
        val psiMethodScopeSearch = when {
            sourcePsiElement is KtParameter && sourcePsiElement.dataClassComponentMethodName != null -> nonKotlinSources

            else -> options.searchScope
        }

        for (psiMethod in sourcePsiElement.toLightMethods().filterDataClassComponentsIfDisabled(kotlinSearchOptions)) {
            tasks.add {
                // function as property syntax when there is java super
                MethodReferencesSearch.search(psiMethod, psiMethodScopeSearch, true).applyFilters()
                    .forEach(referencesProcessor)
            }
        }

        if (sourcePsiElement is KtPrimaryConstructor) {
            val containingClass = sourcePsiElement.containingClass()
            if (containingClass?.isAnnotation() == true) {
                tasks.add {
                    ReferencesSearch.search(containingClass, nonKotlinSources).applyFilters()
                        .forEach(referencesProcessor)
                }
            }
        }
    }

    private fun buildConstructorLookupTask(referenceProcessor: Processor<PsiReference>) {
        tasks.add(
            sourcePsiElement.buildProcessDelegationCallKotlinConstructorUsagesTask(options.searchScope) { callElement ->
                callElement.calleeExpression?.let { callee ->
                    val reference = callee.mainReference
                    reference == null || referenceProcessor.process(reference)
                } != false
            },
        )
    }

    private fun Query<PsiReference>.applyFilters(): Query<PsiReference> =
        FilteredQuery(this) { !it.isImportUsage() }

    private fun createReferenceProcessor(processor: Processor<in UsageInfo>): Processor<PsiReference> {
        val uniqueProcessor = CommonProcessors.UniqueProcessor(processor)

        return Processor { processUsage(uniqueProcessor, it) }
    }

    companion object {
        @Suppress("FUNCTION_BOOLEAN_PREFIX")
        internal fun processUsage(processor: Processor<in UsageInfo>, ref: PsiReference): Boolean =
            processor.processIfNotNull {
                when {
                    ref is LightMemberReference -> KotlinReferencePreservingUsageInfo(ref)
                    ref.element.isValid -> KotlinReferenceUsageInfo(ref)
                    else -> null
                }
            }

        @Suppress("FUNCTION_BOOLEAN_PREFIX")
        internal fun processUsage(
            processor: Processor<in UsageInfo>,
            element: PsiElement,
        ): Boolean =
            processor.processIfNotNull { if (element.isValid) UsageInfo(element) else null }

        @Suppress("FUNCTION_BOOLEAN_PREFIX")
        private fun Processor<in UsageInfo>.processIfNotNull(callback: () -> UsageInfo?): Boolean {
            ProgressManager.checkCanceled()
            val usageInfo = runReadAction(callback)
            return usageInfo?.let {
                process(usageInfo)
            } ?: true
        }
    }
}
