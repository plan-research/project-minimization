package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.PsiDDItem

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.util.concurrency.annotations.RequiresReadLock
import mu.KotlinLogging
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtPsiFactory

import kotlin.io.path.relativeTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The PsiProcessor class provides utilities for fetching PSI elements within a given project.
 *
 */
class PsiProcessor(private val project: Project) {
    private val rootPath = project.guessProjectDir()!!.toNioPath()
    private val psiFactory = KtPsiFactory(project)
    private val logger = KotlinLogging.logger { }

    /**
     * Transforms PsiElement into **replaceable** PSIDDItem by traversing the parents and collecting file information.
     * May return null if the element has a parent that could by modified
     *
     * @param element
     */
    @RequiresReadLock
    fun buildReplaceablePsiItem(
        element: PsiElement,
    ): PsiDDItem? {
        val (currentFile, parentPath) = buildParentPath(element) { !PsiDDItem.isCompatible(it) } ?: return null
        val localPath = currentFile.virtualFile.toNioPath().relativeTo(rootPath)
        return PsiDDItem.create(parentPath, localPath)
    }

    /**
     * Transforms PsiElement into **deletable** PsiDDItem by traversing the parents and collecting file information
     *
     * @param element Element to transform
     * @return a converted PSIDDItem
     */
    @RequiresReadLock
    fun buildDeletablePsiItem(
        element: PsiElement,
    ): PsiDDItem {
        val (currentFile, parentPath) = buildParentPath(element) { true }!!
        val localPath = currentFile.virtualFile.toNioPath().relativeTo(rootPath)
        return PsiDDItem.create(parentPath, localPath)
    }

    suspend fun replaceBody(classInitializer: KtClassInitializer) {
        withContext(Dispatchers.EDT) {
            writeCommandAction(project, "Replacing Class Initializer") {
                logger.debug { "Replacing class initializer body: ${classInitializer.name}" }
                classInitializer.body?.replace(psiFactory.createBlock(BLOCKLESS_TEXT))
            }
        }
    }

    suspend fun replaceBody(function: KtNamedFunction) {
        val (hasBlockBody, hasBody) = readAction { function.hasBlockBody() to function.hasBody() }
        when {
            hasBlockBody -> withContext(Dispatchers.EDT) {
                writeCommandAction(project, "Replacing Function Body Block") {
                    logger.debug { "Replacing function body block: ${function.name} in ${function.containingFile.virtualFile.path}" }
                    function.bodyBlockExpression?.replace(
                        psiFactory.createBlock(
                            BLOCKLESS_TEXT,
                        ),
                    )
                }
            }

            hasBody -> withContext(Dispatchers.EDT) {
                writeCommandAction(project, "Replacing Function Body Expression") {
                    logger.debug { "Replacing function body without block: ${function.name} in ${function.containingFile.virtualFile.path}" }
                    function.bodyExpression!!.replace(
                        psiFactory.createExpression(
                            BLOCKLESS_TEXT,
                        ),
                    )
                }
            }

            else -> {}
        }
    }

    suspend fun replaceBody(lambdaExpression: KtLambdaExpression): Unit = withContext(Dispatchers.EDT) {
        writeCommandAction(project, "Replacing Lambda Body Expression") {
            logger.debug { "Replacing lambda expression in ${lambdaExpression.containingFile.virtualFile.path}" }
            lambdaExpression.bodyExpression!!.replace(
                psiFactory.createLambdaExpression(
                    "",
                    BLOCKLESS_TEXT,
                ).bodyExpression!!,
            )
        }
    }

    suspend fun replaceBody(accessor: KtPropertyAccessor): Unit = withContext(Dispatchers.EDT) {
        writeCommandAction(project, "Replacing Accessor Body") {
            logger.debug { "Replacing accessor body: ${accessor.name} in ${accessor.containingFile.virtualFile.path}" }
            when {
                accessor.hasBlockBody() -> accessor.bodyBlockExpression!!.replace(psiFactory.createBlock(BLOCKLESS_TEXT))
                accessor.hasBody() -> accessor.bodyExpression!!.replace(psiFactory.createExpression(BLOCKLESS_TEXT))
            }
        }
    }

    @RequiresReadLock
    private fun buildParentPath(
        element: PsiElement,
        isElementAllowed: (PsiElement) -> Boolean,
    ): Pair<PsiFile, List<Int>>? {
        var currentElement: PsiElement = element
        val path = buildList {
            while (currentElement.parent != null && currentElement !is PsiFile) {
                val parent = currentElement.parent
                if (!isElementAllowed(parent)) {
                    return null
                }
                val position = getChildPosition(parent, currentElement)
                add(position)
                currentElement = parent
            }
        }
        require(currentElement is PsiFile)
        return (currentElement as PsiFile) to path.reversed()
    }

    @RequiresReadLock
    private fun getChildPosition(parent: PsiElement, element: PsiElement): Int =
        parent.children.indexOf(element)

    @RequiresReadLock
    fun getKtFile(file: VirtualFile): KtFile? =
        PsiManagerEx.getInstance(this@PsiProcessor.project).findFile(file) as? KtFile

    companion object {
        private const val BLOCKLESS_TEXT = "TODO(\"Removed by DD\")"
    }
}
