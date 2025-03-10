package org.plan.research.minimization.plugin.modification.lenses

import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.context.IJDDContextMonad
import org.plan.research.minimization.plugin.context.WithImportRefCounterContext
import org.plan.research.minimization.plugin.modification.item.PsiDDItem
import org.plan.research.minimization.plugin.modification.item.index.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.modification.psi.PsiImportRefCounter
import org.plan.research.minimization.plugin.modification.psi.PsiUtils
import org.plan.research.minimization.plugin.modification.psi.trie.PsiTrie

import com.intellij.openapi.application.readAction
import com.intellij.psi.PsiElement
import mu.KotlinLogging
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

import java.nio.file.Path

/**
 * Abstract class used for managing and optimizing imports in Kotlin PSI files.
 * This class extends the `BasePsiLens` and relies on a reference counting mechanism
 * to manage and remove unused import statements in Kotlin files.
 */
abstract class AbstractImportRefLens<C, I, T> : BasePsiLens<C, I, T>()
where C : WithImportRefCounterContext<C>,
I : PsiDDItem<T>,
T : Comparable<T>, T : PsiChildrenPathIndex {
    private val logger = KotlinLogging.logger {}
    context(IJDDContextMonad<C>)
    override suspend fun useTrie(
        trie: PsiTrie<I, T>,
        ktFile: KtFile,
    ) {
        // Usual PSI removing stuff
        super.useTrie(trie, ktFile)

        // Import optimization part
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

    /**
     * A function that gets all terminal elements from the [PsiTrie].
     * This process is basically disassembly of the trie to a list
     *
     * @param ktFile
     * @param trie
     */
    protected suspend fun C.getTerminalElements(
        ktFile: KtFile,
        trie: PsiTrie<I, T>,
    ) = readAction {
        val indexKtFile = getKtFileInIndexProject(ktFile) ?: return@readAction null
        buildList {
            trie.processMarkedElements(indexKtFile) { _, psiElement -> add(psiElement) }
        }.filterIsInstance<KtElement>()
    }

    /**
     * Removes all unused imports in [ktFile] using information from [refCounter]
     *
     * @param ktFile
     * @param refCounter
     */
    protected suspend fun C.removeUnusedImports(
        ktFile: KtFile,
        refCounter: PsiImportRefCounter,
    ) {
        val unusedImports = readAction { refCounter.getUnusedImports(ktFile.importDirectives) }
        PsiUtils.performPsiChangesAndSave(this, ktFile, "Optimizing import after instance level focusing") {
            unusedImports.forEach(PsiElement::delete)
        }
    }

    protected suspend fun List<KtElement>.processElements(context: IJDDContext, initialCounter: PsiImportRefCounter) =
        fold(initialCounter) { currentCounter, psiElement ->
            currentCounter.decreaseCounterBasedOnKtElement(context, psiElement)
        }

    protected suspend fun C.processRefs(ktFile: KtFile, currentRefs: List<KtElement>): PsiImportRefCounter {
        val counterForCurrentFile = importRefCounter[ktFile.getLocalPath(this)]
            .getOrNull()
            ?: error("Couldn't find a ref counter for localPath=${ktFile.getLocalPath(this)}")
        val modifiedCounter = currentRefs.processElements(this, counterForCurrentFile)
        removeUnusedImports(ktFile, modifiedCounter)
        return modifiedCounter.purgeUnusedImports()
    }

    protected fun C.copyWithout(localPath: Path) = copy(
        importRefCounter = importRefCounter.performAction { remove(localPath) },
    )
}
