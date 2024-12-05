package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiStubDDItem
import org.plan.research.minimization.plugin.psi.PsiUtils
import org.plan.research.minimization.plugin.psi.stub.KtStub
import org.plan.research.minimization.plugin.psi.trie.PsiTrie

import com.intellij.openapi.application.readAction
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresReadLock
import mu.KotlinLogging
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.util.isComma
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

import kotlin.io.path.relativeTo

class FunctionDeletingLens : BasePsiLens<PsiStubDDItem, KtStub>() {
    private val logger = KotlinLogging.logger {}
    override fun focusOnPsiElement(
        item: PsiStubDDItem,
        psiElement: PsiElement,
        context: IJDDContext,
    ) {
        val nextSibling = psiElement.nextSibling
        psiElement.delete()
        if (nextSibling?.isComma == true) {
            nextSibling.delete()
        }
    }

    override fun getWriteCommandActionName(
        psiFile: KtFile,
        context: IJDDContext,
    ): String = "Deleting PSI elements from ${psiFile.name}"

    override suspend fun useTrie(
        trie: PsiTrie<PsiStubDDItem, KtStub>,
        context: IJDDContext,
        ktFile: KtFile,
    ): IJDDContext {
        val context = super.useTrie(trie, context, ktFile)
        val rootPath = context.projectDir.toNioPath()
        val localPath = ktFile.virtualFile.toNioPath().relativeTo(rootPath)
        if (readAction { !ktFile.isValid }) {
            // See [KtClassOrObject::delete] —
            // on deleting a single top-level declaration,
            // the file will be deleted
            return context.copy(
                importRefCounter = context.importRefCounter?.performAction { remove(localPath) },
            )
        }

        logger.debug { "Optimizing imports in $localPath" }
        val indexKtFile = readAction { getKtFileInIndexProject(ktFile, context) } ?: return context
        val terminalElements = readAction {
            buildList {
                trie.processMarkedElements(indexKtFile) { item, psiElement -> add(psiElement) }
            }.filterIsInstance<KtElement>()
        }

        val counters = context.importRefCounter ?: error("The ref counter couldn't be null in the FunctionDeletingLens")
        val counterForCurrentFile = counters[localPath]
            .getOrNull()
            ?: error("Couldn't find a ref counter for localPath=$localPath")
        val modifiedCounter = terminalElements.fold(counterForCurrentFile) { currentCounter, psiElement ->
            currentCounter.decreaseCounterBasedOnKtElement(psiElement)
        }
        val unusedImports = readAction { modifiedCounter.getUnusedImports(ktFile.importDirectives) }
        PsiUtils.performPsiChangesAndSave(context, ktFile, "Optimizing import after instance level focusing") {
            unusedImports.forEach(PsiElement::delete)
        }
        return context.copy(
            importRefCounter = counters.performAction { this[localPath] = modifiedCounter.purgeUnusedImports() },
        )
    }

    @RequiresReadLock
    private fun getKtFileInIndexProject(ktFile: KtFile, context: IJDDContext): KtFile? {
        val rootPath = context.projectDir.toNioPath()
        val localPath = ktFile.virtualFile.toNioPath().relativeTo(rootPath)
        val indexFile = context.indexProjectDir.findFileByRelativePath(localPath.toString()) ?: run {
            logger.error { "Can't find a local file with path $localPath in index project" }
            return null
        }
        val indexKtFile = indexFile.toPsiFile(context.indexProject) ?: run {
            logger.error { "Can't find a PSI file for a local file with path $localPath in index project" }
            return null
        }
        return indexKtFile as? KtFile ?: run {
            logger.error { "KtFile with localPath=$localPath is not a KtFile in index project" }
            null
        }
    }

    override fun transformSelectedElements(item: PsiStubDDItem, context: IJDDContext): List<PsiStubDDItem> =
        item.childrenElements + item
}
