package org.plan.research.minimization.plugin.psi

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.structuralsearch.visitor.KotlinRecursiveElementVisitor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasBody
import java.nio.file.Path
import kotlin.io.path.relativeTo

/**
 * The visitor
 * that basically follows the rules
 * described in [BodyElementAcquiringKtVisitor] to access the top-level elements with body.
 * In that case the body is replaced using [PsiModificationManager]
 *
 * @param rootProject The root project containing the original Kotlin files.
 * @param newProject The new project where modifications should  be applied.
 */
class ModifyingBodyKtVisitor(
    private val rootProject: Project,
    newProject: Project,
) : KotlinRecursiveElementVisitor() {
    private val files: Map<Path, KtFile>
    private val newProjectRoot = newProject.guessProjectDir()!!.toNioPath()
    private val modificationManager = newProject.service<PsiModificationManager>()

    init {
        val projectFiles = ProjectRootManagerEx.getInstance(rootProject).fileIndex
        val projectRoot = rootProject.guessProjectDir()!!.toNioPath()
        files = buildMap {
            projectFiles.iterateContent { file ->
                val psiFile = file.toPsiFile(rootProject)
                if (psiFile is KtFile) {
                    val relativizedPath = file.toNioPath().relativeTo(projectRoot)
                    put(relativizedPath, psiFile)
                }
                true
            }
        }
    }

    override fun visitClassInitializer(initializer: KtClassInitializer) {
        if (!initializer.shouldDelete() || initializer.body == null) {
            return
        }
        modificationManager.replaceBody(initializer)
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        if (!function.shouldDelete() || !function.hasBody()) {
            return
        }
        modificationManager.replaceBody(function)
    }

    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        if (!lambdaExpression.shouldDelete()) {
            return
        }
        modificationManager.replaceBody(lambdaExpression)
    }

    override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
        if (!accessor.hasBody() || !accessor.shouldDelete()) {
            return
        }
        modificationManager.replaceBody(accessor)
    }

    override fun visitElement(element: PsiElement) {
        if (element is KtFile) {
            initKtFile(element)
        }

        val zippedChildren = element.getUserData(ZIPPED_NODE_KEY)?.children ?: return
        val children = element.children
        if (children.size != zippedChildren.size) {
            return
        }
        val zipped = children
            .zip(zippedChildren)

        if (zipped.any { (child, zippedChild) -> child::class != zippedChild::class }) {
            return
        }
        zipped.forEach { (child, zippedChild) -> child.putUserData(ZIPPED_NODE_KEY, zippedChild) }

        super.visitElement(element)
    }

    private fun initKtFile(file: KtFile) {
        val copiedPath = file.virtualFile.toNioPathOrNull() ?: return
        val pathToLookup = copiedPath.relativeTo(newProjectRoot)
        val originalElement = files[pathToLookup] ?: return  // Extra file, haven't found during original lookup
        file.putUserData(ZIPPED_NODE_KEY, originalElement)
    }

    private fun PsiElement.shouldDelete(): Boolean =
        getUserData(ZIPPED_NODE_KEY)?.getUserData(MAPPED_AS_STORED_KEY) != true

    companion object {
        private val ZIPPED_NODE_KEY = Key<PsiElement>("MINIMIZATION_ZIPPED_NODE")
        val MAPPED_AS_STORED_KEY = Key<Boolean>("MAPPED_AS_STORED_FOR_MINIMIZATION")
    }
}
