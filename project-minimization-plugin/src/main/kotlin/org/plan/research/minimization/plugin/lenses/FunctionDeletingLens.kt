package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.context.WithCallTraceParameterCacheContext
import org.plan.research.minimization.plugin.model.context.WithImportRefCounterContext
import org.plan.research.minimization.plugin.model.item.PsiStubChildrenCompositionItem
import org.plan.research.minimization.plugin.model.item.PsiStubDDItem
import org.plan.research.minimization.plugin.model.item.PsiStubDDItem.CallablePsiStubDDItem
import org.plan.research.minimization.plugin.model.item.index.InstructionLookupIndex.ChildrenNonDeclarationIndex
import org.plan.research.minimization.plugin.model.monad.IJDDContextMonad
import org.plan.research.minimization.plugin.psi.PsiUtils
import org.plan.research.minimization.plugin.psi.stub.KtStub

import arrow.core.None
import arrow.core.filterOption
import arrow.core.raise.option
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.vfs.findFileOrDirectory
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresWriteLock
import mu.KotlinLogging
import org.jetbrains.kotlin.idea.base.psi.deleteSingle
import org.jetbrains.kotlin.idea.refactoring.deleteSeparatingComma
import org.jetbrains.kotlin.psi.KtCallElement

import kotlin.io.path.pathString

class FunctionDeletingLens<C> :
    AbstractImportRefLens<C, PsiStubDDItem, KtStub>() where C : WithImportRefCounterContext<C>, C : WithCallTraceParameterCacheContext<C> {
    private val logger = KotlinLogging.logger {}
    private val callTraceDeletionLens = CallTraceDeletionLens<C>()

    override fun focusOnPsiElement(
        item: PsiStubDDItem,
        psiElement: PsiElement,
        context: C,
    ) {
        deleteSeparatingComma(psiElement)
        psiElement.deleteSingle()
    }

    context(IJDDContextMonad<C>)
    @RequiresWriteLock
    private fun deleteTrace(call: PsiStubChildrenCompositionItem, indexToDelete: Int) {
        logger.trace { "Deleting call=${call.childrenPath} (in file=${call.localPath})" }
        val callExpression = PsiUtils.getPsiElementFromItem(context, call) as? KtCallElement
            ?: let {
                logger.error { "call=${call.childrenPath} (in file=${call.localPath}) is not callable expression" }
                return
            }

        val arguments = callExpression.valueArguments + callExpression.lambdaArguments
        val element = arguments.getOrNull(indexToDelete)?.asElement()
            ?: let {
                logger.error { "index=$indexToDelete can't be found. " }
                return
            }
        deleteSeparatingComma(element)
        element.delete()
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
    private fun CallablePsiStubDDItem.getParameterIndex() = option {
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
                val parameterName = item.getParameterIndex().getOrNull() ?: let {
                    logger.warn { "Can't find parameter index for ${item.childrenPath}" }
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
                        val parameterName = item.getParameterIndex().getOrNull() ?: let {
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
            // Perfectly crafted path to the parameter itself instead of just call expression
            copy(childrenPath = childrenPath + ChildrenNonDeclarationIndex.createFromAncestor(psiItem!!, psiArgument).bind())
        }
}
