package org.plan.research.minimization.plugin.modification.psi

import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.modification.graph.PsiIJEdge
import org.plan.research.minimization.plugin.modification.item.PsiChildrenIndexDDItem
import org.plan.research.minimization.plugin.modification.item.PsiDDItem
import org.plan.research.minimization.plugin.modification.item.PsiStubChildrenCompositionItem
import org.plan.research.minimization.plugin.modification.item.PsiStubDDItem
import org.plan.research.minimization.plugin.modification.item.index.InstructionLookupIndex
import org.plan.research.minimization.plugin.modification.item.index.IntChildrenIndex
import org.plan.research.minimization.plugin.modification.item.index.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.modification.psi.stub.KtStub

import arrow.core.None
import arrow.core.Option
import arrow.core.raise.option
import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

import kotlin.io.path.relativeTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private typealias ParentChildPsiProcessor<T> = (PsiElement, PsiElement) -> T

/**
 * The PsiProcessor class provides utilities for fetching PSI elements within a given project.
 */
object PsiUtils {
    /**
     * Builds a [PsiStubChildrenCompositionItem] from a [PsiElement].
     * Should be used with [KtCallExpression] to acquire a trace for a function calls.
     *
     * @param context A context with required information
     * @param element [PsiElement] to build trace to
     * @return [Option] with a built item
     * @see PsiStubChildrenCompositionItem
     */
    @RequiresReadLock
    fun buildCompositeStubItem(context: IJDDContext, element: PsiElement) = option {
        ensure(element !is PsiFile && element is KtElement)
        val parents = element.parentsWithSelf.toList()
        val file = parents.last() as? KtFile
        ensureNotNull(file)

        val (stubPart, childrenPart) = parents.dropLast(1).reversed().splitWhile(KtStub::canBeCreated)
        ensure(stubPart.isNotEmpty())

        val stubs = stubPart.map { InstructionLookupIndex.StubDeclarationIndex(KtStub.create(it).bind()) }
        val children = buildList {
            add(InstructionLookupIndex.ChildrenNonDeclarationIndex.create(stubPart.last(), childrenPart.first()).bind())
            childrenPart
                .zipWithNext()
                .forEach { (parent, child) ->
                    add(InstructionLookupIndex.ChildrenNonDeclarationIndex.create(parent, child).bind())
                }
        }

        val path = file.virtualFile.toNioPath().relativeTo(context.projectDir.toNioPath())
        PsiStubChildrenCompositionItem(
            localPath = path,
            childrenPath = stubs + children,
        )
    }

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
     * It is used for building [PsiIJEdge.PSITreeEdge] for the instance-level adapters.
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
     * Resolve [PsiElement] from a given [PsiDDItem].
     *
     * This function locates the file corresponding to the given [PsiDDItem] using its [PsiDDItem.localPath]
     * and navigates to the [PsiElement] within the file following the [PsiDDItem.childrenPath].
     *
     * @param context The context of the current computation, providing access to the project directory and related utilities.
     * @param item The item containing the `localPath` to locate the file and the `childrenPath` to resolve the desired PSI element.
     * @return The resolved [PsiElement], or `null` if it cannot be resolved.
     */
    @RequiresReadLock
    fun <T : PsiChildrenPathIndex> getPsiElementFromItem(context: IJDDContext, item: PsiDDItem<T>): PsiElement? {
        val virtualFile = context.projectDir.findFileByRelativePath(item.localPath.toString())!!
        if (virtualFile.isDirectory) {
            return virtualFile.toPsiDirectory(context.indexProject)
        }
        val file = getPsiFile(context, virtualFile)!!
        return getPsiByPath(file, item.childrenPath)
    }

    fun <T : PsiChildrenPathIndex> getElementByFileAndPath(ktFile: KtFile, path: List<T>): KtElement? =
        getPsiByPath(ktFile, path) as? KtElement

    private fun <T : PsiChildrenPathIndex> getPsiByPath(root: PsiElement, path: List<T>): PsiElement? =
        path.fold(root) { element, idx -> idx.getNext(element) ?: return@getPsiByPath null }

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

    /**
     * Builds a path of parent-child relationships from a given PsiElement to
     * the first encountered file or directory.
     *
     * @param element The starting PsiElement from which the path will be constructed.
     * @param pathElementProducer A function from a parent and a child to their relationship.
     * @param isElementAllowed A predicate of allowed PsiElement in the path.
     * @return A pair of the topmost PsiElement in the path and the list of processed
     *         relationships, or null if any element in the path is not allowed.
     */
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
        getPsiFile(context, file) as? KtFile

    @RequiresReadLock
    fun getJFile(context: IJDDContext, file: VirtualFile): PsiJavaFile? =
        getPsiFile(context, file) as? PsiJavaFile

    @RequiresReadLock
    fun getPsiFile(context: IJDDContext, file: VirtualFile): PsiFile? =
        PsiManagerEx.getInstance(context.indexProject).findFile(file)

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

    private fun <T> List<T>.splitWhile(predicate: (T) -> Boolean) = takeWhile(predicate) to dropWhile(predicate)
}
