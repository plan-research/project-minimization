package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.context.WithImportRefCounterContext
import org.plan.research.minimization.plugin.model.item.PsiStubDDItem
import org.plan.research.minimization.plugin.model.monad.IJDDContextMonad
import org.plan.research.minimization.plugin.psi.PsiImportRefCounter
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

import java.nio.file.Path

import kotlin.io.path.relativeTo

class FunctionDeletingLens<C : WithImportRefCounterContext<C>> : BasePsiLens<C, PsiStubDDItem, KtStub>() {
    private val logger = KotlinLogging.logger {}
    override fun focusOnPsiElement(
        item: PsiStubDDItem,
        psiElement: PsiElement,
        context: C,
    ) {
        val nextSibling = psiElement.nextSibling
        psiElement.delete()
        if (nextSibling?.isComma == true) {
            nextSibling.delete()
        }
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
}
