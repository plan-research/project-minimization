package org.plan.research.minimization.plugin.modification.lenses

import org.plan.research.minimization.plugin.context.IJDDContextMonad
import org.plan.research.minimization.plugin.context.WithCallTraceParameterCacheContext
import org.plan.research.minimization.plugin.context.WithImportRefCounterContext
import org.plan.research.minimization.plugin.modification.item.PsiStubChildrenCompositionItem
import org.plan.research.minimization.plugin.modification.item.PsiStubDDItem
import org.plan.research.minimization.plugin.modification.item.PsiStubDDItem.CallablePsiStubDDItem
import org.plan.research.minimization.plugin.modification.item.index.InstructionLookupIndex
import org.plan.research.minimization.plugin.modification.item.index.InstructionLookupIndex.ChildrenNonDeclarationIndex
import org.plan.research.minimization.plugin.modification.psi.PsiUtils
import org.plan.research.minimization.plugin.modification.psi.stub.KtStub

import arrow.core.None
import arrow.core.filterOption
import arrow.core.raise.option
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.vfs.findFileOrDirectory
import com.intellij.psi.PsiElement
import mu.KotlinLogging
import org.jetbrains.kotlin.idea.base.psi.deleteSingle
import org.jetbrains.kotlin.idea.refactoring.deleteSeparatingComma
import org.jetbrains.kotlin.psi.KtCallElement

import kotlin.io.path.pathString

class FunctionDeletingLens<C> :
    AbstractImportRefLens<C, PsiStubDDItem, KtStub>() where C : WithImportRefCounterContext<C>, C : WithCallTraceParameterCacheContext<C> {
    private val logger = KotlinLogging.logger {}
    private val callTraceDeletionLens = CallTraceDeletionLens()

    override fun focusOnPsiElement(
        item: PsiStubDDItem,
        psiElement: PsiElement,
        context: C,
    ) {
        deleteSeparatingComma(psiElement)
        psiElement.deleteSingle()
    }

    context(IJDDContextMonad<C>)
    override suspend fun prepare(itemsToDelete: List<PsiStubDDItem>) =
        processCallablePsiItems(itemsToDelete.filterIsInstance<CallablePsiStubDDItem>())

    override suspend fun focusOnFilesAndDirectories(
        itemsToDelete: List<PsiStubDDItem>,
        context: C,
    ) = writeAction {
        itemsToDelete.forEach {
            context
                .projectDir
                .findFileOrDirectory(it.localPath.pathString)
                ?.delete(this@FunctionDeletingLens)
        }
    }

    context(IJDDContextMonad<C>)
    private fun CallablePsiStubDDItem.getParameterName() = option {
        logger.trace { "Found a callable psi stub DD item: $childrenPath" }
        childrenPath.lastOrNull()?.name ?: run {
            logger.warn { "Callable element=${this@CallablePsiStubDDItem.childrenPath} has no name" }
            raise(None)
        }
    }

    context(IJDDContextMonad<C>)
    private suspend fun processCallablePsiItems(items: List<CallablePsiStubDDItem>) {
        logger.debug { "Found ${items.size} callable items" }
        val sortedTraces = items
            .flatMap { item ->
                val parameterName = item.getParameterName().getOrNull() ?: run {
                    logger.warn { "Can't find parameter name for ${item.childrenPath}" }
                    return@flatMap emptyList<PsiStubChildrenCompositionItem>()
                }
                item.callTraces.map { it.transformToParameterPath(context, parameterName) }.filterOption()
            }
        logger.debug { "Found ${sortedTraces.size} traces" }
        callTraceDeletionLens.focusOn(sortedTraces)
        logger.debug { "Deleted all call traces" }
        updateContext {
            context.copy(
                callTraceParameterCache = items.fold(context.callTraceParameterCache) { cache, item ->
                    item.callTraces.fold(cache) { cache, callTrace ->
                        val parameterName = item.getParameterName().getOrNull() ?: let {
                            logger.warn { "Can't find parameter index for ${item.childrenPath}" }
                            return@fold cache
                        }
                        cache.deleteParameter(parameterName, callTrace)
                    }
                },
            )
        }
    }

    override fun transformSelectedElements(item: PsiStubDDItem, context: C): List<PsiStubDDItem> =
        item.childrenElements + item

    private suspend fun PsiStubChildrenCompositionItem.transformToParameterPath(context: C, parameterName: String) =
        option {
            val psiItem = readAction { PsiUtils.getPsiElementFromItem(context, this@transformToParameterPath) }
            ensure(psiItem is KtCallElement)
            // Name -> index in value argument list
            val parameterIndex = context
                .callTraceParameterCache
                .getIndexOf(parameterName, this@transformToParameterPath)
                .bind()
            val valueArgumentList = readAction { (psiItem as KtCallElement).valueArguments }
            val argument = valueArgumentList.getOrNull(parameterIndex) ?: run {
                logger.error { "Could not find the argument $parameterName with $parameterIndex. It is in index, but not found as value argument." }
                raise(None)
            }
            val psiArgument = readAction { argument.asElement() }
            // Perfectly crafted path to the parameter itself instead of call expression
            copy(
                childrenPath = childrenPath +
                    ChildrenNonDeclarationIndex.createFromAncestor(psiItem!!, psiArgument).bind(),
            )
        }

    /**
     * A specific implementation of `AbstractImportRefLens` used to handle deletion of call traces in a PSI.
     * This lens operates within a specific deletion context and is designed to work in conjunction with other lenses for more complex operations.
     */
    private inner class CallTraceDeletionLens :
        AbstractImportRefLens<C, PsiStubChildrenCompositionItem, InstructionLookupIndex>() {
        override fun focusOnPsiElement(
            item: PsiStubChildrenCompositionItem,
            psiElement: PsiElement,
            context: C,
        ) {
            deleteSeparatingComma(psiElement)
            psiElement.deleteSingle()
        }

        override suspend fun focusOnFilesAndDirectories(
            itemsToDelete: List<PsiStubChildrenCompositionItem>,
            context: C,
        ) {
            if (itemsToDelete.isEmpty()) {
                return
            }
            throw UnsupportedOperationException("This lens should be not called on their own but only as part of FunctionDeletionLens")
        }
    }
}
