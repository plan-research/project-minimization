package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.context.WithImportRefCounterContext
import org.plan.research.minimization.plugin.model.item.PsiStubChildrenCompositionItem
import org.plan.research.minimization.plugin.model.item.PsiStubDDItem
import org.plan.research.minimization.plugin.model.item.PsiStubDDItem.CallablePsiStubDDItem
import org.plan.research.minimization.plugin.model.monad.IJDDContextMonad
import org.plan.research.minimization.plugin.psi.PsiImportRefCounter
import org.plan.research.minimization.plugin.psi.PsiUtils
import org.plan.research.minimization.plugin.psi.stub.KtStub
import org.plan.research.minimization.plugin.psi.trie.PsiTrie

import arrow.core.None
import arrow.core.raise.option
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.vfs.findFileOrDirectory
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.concurrency.annotations.RequiresWriteLock
import mu.KotlinLogging
import org.jetbrains.kotlin.idea.base.psi.deleteSingle
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.refactoring.deleteSeparatingComma
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtValueArgument

import java.nio.file.Path

import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

private typealias IndexesForItem = Map<PsiStubChildrenCompositionItem, List<Int>>

class FunctionDeletingLens<C : WithImportRefCounterContext<C>> : BasePsiLens<C, PsiStubDDItem, KtStub>() {
    private val logger = KotlinLogging.logger {}
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

    private suspend fun deleteImportInTraces(
        refCounter: PsiImportRefCounter,
        call: PsiStubChildrenCompositionItem,
        indexToDelete: Int,
        originalFile: KtFile,
        context: C,
    ) = refCounter.run {
        logger.trace { "Deleting imports in  call=${call.childrenPath} (in file=${call.localPath})" }
        val callExpression = readAction {
            PsiUtils.getElementByFileAndPath(originalFile, call.childrenPath) as? KtCallExpression
        }
            ?: let {
                logger.trace { "call=${call.childrenPath} (in file=${call.localPath}) is not callable expression" }
                return@run this
            }

        val element = readAction {
            callExpression.getArgument(indexToDelete)
        } ?: let {
            logger.trace { "index=$indexToDelete can't be found. " }
            return@run this
        }
        return refCounter.decreaseCounterBasedOnKtElement(context, element)
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
    private suspend fun processImportsForCallsInFile(
        refCounter: PsiImportRefCounter,
        psiFile: KtFile,
        allIndexesForItem: IndexesForItem,
    ): PsiImportRefCounter {
        val originalFile = readAction { context.getKtFileInIndexProject(psiFile) } ?: let {
            logger.warn {
                "can't find original file for ${
                    psiFile.virtualFile.toNioPath().relativeTo(context.projectDir.toNioPath())
                }"
            }
            return refCounter
        }
        val modifiedRefCounter = allIndexesForItem.keys.fold(refCounter) { refCounter, item ->
            val indexes = allIndexesForItem[item]
                ?: let {
                    logger.debug { "Can't find indexes for ${item.childrenPath}" }
                    return@fold refCounter
                }
            indexes.fold(refCounter) { refCounter, index ->
                deleteImportInTraces(
                    refCounter,
                    item,
                    index,
                    originalFile,
                    context,
                )
            }
        }
        context.removeUnusedImports(psiFile, modifiedRefCounter)
        return refCounter.purgeUnusedImports()
    }

    context(IJDDContextMonad<C>)
    private suspend fun deleteCallsInFile(
        psiFile: KtFile,
        allIndexesForItem: IndexesForItem,
    ) = PsiUtils.performPsiChangesAndSave(context, psiFile, "Removing calls to functions/constructors") {
        allIndexesForItem.keys.forEach { item ->
            val indexes = allIndexesForItem[item]
                ?: let {
                    logger.debug { "Can't find indexes for ${item.childrenPath}" }
                    return@forEach
                }
            indexes.forEach { index -> deleteTrace(item, index) }
        }
    }

    context(IJDDContextMonad<C>)
    private suspend fun processCallableItemsInFile(file: Path, items: List<IntermediateCallInfo>) {
        logger.trace { "Removing all needed calls from $file" }
        val psiFile = readAction {
            context
                .projectDir
                .findFileOrDirectory(file.pathString)
                ?.toPsiFile(context.indexProject)
                as? KtFile
        } ?: let {
            logger.info { "$file is not PSI File" }
            return
        }
        val allIndexesForItem = items
            .groupBy { it.item }
            .mapValues { (_, v) ->
                v
                    .map { it.parameterIndex }
                    .sorted()
                    .distinct()
                    .reversed()
            }
        val modifiedRefCounter =
            context.importRefCounter[file].map { processImportsForCallsInFile(it, psiFile, allIndexesForItem) }
        deleteCallsInFile(psiFile, allIndexesForItem)
        modifiedRefCounter.onSome { counter ->
            updateContext {
                it.copy(
                    importRefCounter = it.importRefCounter
                        .performAction { put(file, counter) },
                )
            }
        }
    }

    context(IJDDContextMonad<C>)
    private suspend fun CallablePsiStubDDItem.getParameterIndex() = option {
        logger.debug { "Found a callable psi stub DD item: $childrenPath" }
        val psiElement = readAction {
            PsiUtils.getPsiElementFromItem(context, this@getParameterIndex) as? KtParameter
        } ?: let {
            logger.warn { "callable element=$childrenPath is not a KtParameter" }
            raise(None)
        }

        val owner = readAction { psiElement.ownerFunction } ?: let {
            logger.warn { "callable element=$childrenPath has an owner that is not a KtCallExpression" }
            raise(None)
        }

        val parameterList = readAction { owner.valueParameters }
        parameterList.indexOf(psiElement)
    }

    context(IJDDContextMonad<C>)
    private suspend fun processCallablePsiItems(items: List<CallablePsiStubDDItem>) {
        logger.debug { "Found $items.size} callable items" }
        val sortedTraces = items
            .flatMap { item ->
                val parameterIndex = item.getParameterIndex().getOrNull() ?: let {
                    logger.warn { "Can't find parameter index for ${item.childrenPath}" }
                    return@flatMap emptyList<IntermediateCallInfo>()
                }
                item.callTraces.map { IntermediateCallInfo(it, parameterIndex) }
            }
            .groupBy { it.item.localPath }
        logger.debug { "Found ${sortedTraces.size} files with ${sortedTraces.values.sumOf { it.size }} traces " }
        sortedTraces.forEach { (file, items) -> processCallableItemsInFile(file, items) }
    }

    context(IJDDContextMonad<C>)
    override suspend fun useTrie(
        trie: PsiTrie<PsiStubDDItem, KtStub>,
        ktFile: KtFile,
    ) {
        super.useTrie(trie, ktFile)
        val localPath = ktFile.getLocalPath(context)
        if (readAction { !ktFile.isValid }) {
            logger.debug { "All top-level declarations has been removed from $localPath. Invalidating the ref counter for it" }
            // See [KtClassOrObject::delete] —
            // on deleting a single top-level declaration,
            // the file will be deleted
            updateContext { it.copyWithout(localPath) }
            return
        }

        logger.debug { "Optimizing imports in $localPath" }
        val terminalElements = context.getTerminalElements(ktFile, trie)
            ?: run {
                // If any searching problem with the file occurred, then the file should be removed completely
                updateContext { it.copyWithout(localPath) }
                return
            }

        val modifiedCounter = context.processRefs(ktFile, terminalElements)
        updateContext {
            it.copy(
                importRefCounter = it.importRefCounter
                    .performAction { put(localPath, modifiedCounter) },
            )
        }
    }

    @RequiresReadLock
    private fun IJDDContext.getKtFileInIndexProject(ktFile: KtFile): KtFile? {
        val rootPath = projectDir.toNioPath()
        val localPath = ktFile.virtualFile.toNioPath().relativeTo(rootPath)
        val indexFile = indexProjectDir.findFileByRelativePath(localPath.toString()) ?: run {
            logger.error { "Can't find a local file with path $localPath in index project" }
            return null
        }
        val indexKtFile = indexFile.toPsiFile(indexProject) ?: run {
            logger.error { "Can't find a PSI file for a local file with path $localPath in index project" }
            return null
        }
        return indexKtFile as? KtFile ?: run {
            logger.error { "KtFile with localPath=$localPath is not a KtFile in index project" }
            null
        }
    }

    override fun transformSelectedElements(item: PsiStubDDItem, context: C): List<PsiStubDDItem> =
        item.childrenElements + item

    private fun KtFile.getLocalPath(context: C): Path {
        val rootPath = context.projectDir.toNioPath()
        return this.virtualFile.toNioPath().relativeTo(rootPath)
    }

    private suspend fun C.getTerminalElements(
        ktFile: KtFile,
        trie: PsiTrie<PsiStubDDItem, KtStub>,
    ) = readAction {
        val indexKtFile = getKtFileInIndexProject(ktFile) ?: return@readAction null
        buildList {
            trie.processMarkedElements(indexKtFile) { _, psiElement -> add(psiElement) }
        }.filterIsInstance<KtElement>()
    }

    private suspend fun C.removeUnusedImports(
        ktFile: KtFile,
        refCounter: PsiImportRefCounter,
    ) {
        val unusedImports = readAction { refCounter.getUnusedImports(ktFile.importDirectives) }
        PsiUtils.performPsiChangesAndSave(this, ktFile, "Optimizing import after instance level focusing") {
            unusedImports.forEach(PsiElement::delete)
        }
    }

    private suspend fun List<KtElement>.processElements(context: IJDDContext, initialCounter: PsiImportRefCounter) =
        fold(initialCounter) { currentCounter, psiElement ->
            currentCounter.decreaseCounterBasedOnKtElement(context, psiElement)
        }

    private suspend fun C.processRefs(ktFile: KtFile, currentRefs: List<KtElement>): PsiImportRefCounter {
        val counterForCurrentFile = importRefCounter[ktFile.getLocalPath(this)]
            .getOrNull()
            ?: error("Couldn't find a ref counter for localPath=${ktFile.getLocalPath(this)}")
        val modifiedCounter = currentRefs.processElements(this, counterForCurrentFile)
        removeUnusedImports(ktFile, modifiedCounter)
        return modifiedCounter.purgeUnusedImports()
    }

    private fun C.copyWithout(localPath: Path) = copy(
        importRefCounter = importRefCounter.performAction { remove(localPath) },
    )

    @RequiresReadLock
    private fun KtCallExpression.getArgument(index: Int): KtValueArgument? {
        val arguments = valueArguments + lambdaArguments
        return arguments.getOrNull(index)
    }

    private data class IntermediateCallInfo(val item: PsiStubChildrenCompositionItem, val parameterIndex: Int)
}
