package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.item.PsiChildrenIndexDDItem
import org.plan.research.minimization.plugin.model.item.PsiDDItem
import org.plan.research.minimization.plugin.model.item.PsiStubDDItem
import org.plan.research.minimization.plugin.model.item.index.IntChildrenIndex
import org.plan.research.minimization.plugin.model.item.index.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.psi.graph.PsiIJEdge
import org.plan.research.minimization.plugin.psi.stub.KtStub

import arrow.core.None
import arrow.core.Option
import arrow.core.raise.option
import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

import kotlin.io.path.relativeTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private typealias ParentChildPsiProcessor<T> = (PsiElement, PsiElement) -> T

/**
 * The PsiProcessor class provides utilities for fetching PSI elements within a given project.
 */
object PsiUtils {
    /**
     * Collects all dependencies (such as calls, used types, superclasses, and expected elements)
     * for a PSI tree with root [psiElement].
     * The function should be only used for **Instance-level** stages,
     * since [UsedPsiElementGetter] implements an instance-level-specific visitor.
     *
     * @param psiElement Root of the PSI tree
     * @return all referenced PSI elements
     */
    @RequiresReadLock
    fun collectUsages(psiElement: KtElement): List<PsiElement> = buildList {
        val visitor = UsedPsiElementGetter(psiElement is KtNamedFunction)
        psiElement.acceptChildren(visitor)
        addAll(visitor.usedElements)
    }

    /**
     * Collects all parent elements of the given [item].
     * It is used for building [PsiIJEdge.PSITreeEdge] for the instance-level graph.
     * Thus returned items follow the invariant of [PsiStubDDItem]:
     * all returned elements are some of the selected PSI nodes.
     *
     * @param context Context with the project-related information
     * @param item Item to collect parents from
     * @return the list of [PsiStubDDItem], which are the parent elements of [item]
     */
    @RequiresReadLock
    fun findAllDeletableParentElements(context: IJDDContext, item: PsiStubDDItem): PsiStubDDItem? {
        val currentPath = item.childrenPath.toMutableList()
        val file = context.projectDir.findFileByRelativePath(item.localPath.toString())!!
        val ktFile = getKtFile(context, file)!!
        currentPath.removeLast()
        while (currentPath.isNotEmpty()) {
            val currentPsi = getElementByFileAndPath(ktFile, currentPath)
            // if that element has one of the DELETABLE_PSI_JAVA_CLASSES, then it has been collected before
            if (PsiStubDDItem.DELETABLE_PSI_JAVA_CLASSES.any { it.isInstance(currentPsi) }) {
                return PsiStubDDItem.NonOverriddenPsiStubDDItem(item.localPath, currentPath.toList())
            }
            currentPath.removeLast()
        }
        return null
    }

    /**
     * Resolves and retrieves a Kotlin PSI element of type [KtExpression] from a given [PsiDDItem].
     *
     * This function locates the file corresponding to the given [PsiDDItem] using its [PsiDDItem.localPath].
     * parses it as a Kotlin file,
     * and navigates to the specified PSI element within the file following the [PsiDDItem.childrenPath].
     *
     * @param context The context of the current computation, providing access to the project directory and related utilities.
     * @param item The PSI item containing the `localPath` to locate the file and the `childrenPath` to resolve the desired PSI element.
     * @return The resolved [KtExpression] instance, or `null` if the PSI element cannot be resolved.
     */
    @RequiresReadLock
    fun <T : PsiChildrenPathIndex> getPsiElementFromItem(context: IJDDContext, item: PsiDDItem<T>): KtExpression? {
        val file = context.projectDir.findFileByRelativePath(item.localPath.toString())!!
        val ktFile = getKtFile(context, file)!!
        return getElementByFileAndPath(ktFile, item.childrenPath)
    }

    private fun <T : PsiChildrenPathIndex> getElementByFileAndPath(ktFile: KtFile, path: List<T>): KtExpression? {
        var currentDepth = 0
        var element: PsiElement = ktFile
        while (currentDepth < path.size) {
            element = path[currentDepth++].getNext(element) ?: return null
        }
        val psiElement = element as? KtExpression
        return psiElement
    }

    /**
     * Transforms PsiElement into **replaceable** PSIDDItem by traversing the parents and collecting file information.
     * May return null if the element has a parent that could by modified
     *
     * @param element
     * @param context
     */
    @RequiresReadLock
    fun buildReplaceablePsiItem(
        context: IJDDContext,
        element: PsiElement,
    ): PsiChildrenIndexDDItem? {
        val (currentFile, parentPath) = buildParentPath(
            element,
            ::getChildPosition,
        ) { !PsiChildrenIndexDDItem.isCompatible(it) } ?: return null
        val vfs = when (currentFile) {
            is PsiFile -> currentFile.virtualFile
            is PsiDirectory -> currentFile.virtualFile
            else -> return null
        }
        val localPath = vfs.toNioPath().relativeTo(context.projectDir.toNioPath())
        val renderedType = PsiBodyTypeRenderer.transform(element)
        return PsiChildrenIndexDDItem.create(element, parentPath, localPath, renderedType)
    }

    /**
     * Transforms PsiElement into **deletable** PsiDDItem by traversing the parents and collecting file information
     *
     * @param element Element to transform
     * @param context
     * @return a converted PSIDDItem
     */
    @RequiresReadLock
    fun buildDeletablePsiItem(
        context: IJDDContext,
        element: PsiElement,
    ): Option<PsiStubDDItem> = option {
        val (currentFile, parentPath) = buildParentPath(element, { _, element -> KtStub.create(element) }) { true }!!
        val vfs = when (currentFile) {
            is PsiFile -> currentFile.virtualFile
            is PsiDirectory -> currentFile.virtualFile
            else -> raise(None)
        }
        val localPath = vfs.toNioPath().relativeTo(context.projectDir.toNioPath())
        // At that stage we have no clue about the hierarchy of the overridden elements
        PsiStubDDItem.NonOverriddenPsiStubDDItem(localPath, parentPath.map { it.bind() })
    }

    @RequiresReadLock
    @Suppress("TYPE_ALIAS")
    private fun <T> buildParentPath(
        element: PsiElement,
        pathElementProducer: ParentChildPsiProcessor<T>,
        isElementAllowed: (PsiElement) -> Boolean,
    ): Pair<PsiElement, List<T>>? {
        var currentElement: PsiElement = element
        val path = buildList {
            while (currentElement.parent != null && currentElement !is PsiFile && currentElement !is PsiDirectory) {
                val parent = currentElement.parent
                if (!isElementAllowed(parent)) {
                    return null
                }
                val position = pathElementProducer(parent, currentElement)
                add(position)
                currentElement = parent
            }
        }
        return currentElement to path.reversed()
    }

    @RequiresReadLock
    private fun getChildPosition(parent: PsiElement, element: PsiElement): IntChildrenIndex =
        IntChildrenIndex(parent.children.indexOf(element))

    @RequiresReadLock
    fun getKtFile(context: IJDDContext, file: VirtualFile): KtFile? =
        PsiManagerEx.getInstance(context.indexProject).findFile(file) as? KtFile

    suspend inline fun <T> performPsiChangesAndSave(
        context: IJDDContext,
        psiFile: PsiFile,
        commandName: String = "",
        crossinline block: () -> T,
    ): T? = withContext(Dispatchers.EDT) {
        writeCommandAction(context.indexProject, commandName) {
            val documentManager = PsiDocumentManager.getInstance(context.indexProject)
            val document = documentManager.getDocument(psiFile) ?: return@writeCommandAction null

            val editor = EditorFactory.getInstance().createEditor(document, context.indexProject)
            try {
                block()
            } finally {
                documentManager.doPostponedOperationsAndUnblockDocument(document)
                documentManager.commitDocument(document)
                FileDocumentManager.getInstance().saveDocument(document)

                EditorFactory.getInstance().releaseEditor(editor)
            }
        }
    }
}
