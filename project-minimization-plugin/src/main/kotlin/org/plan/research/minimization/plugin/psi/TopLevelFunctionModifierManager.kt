package org.plan.research.minimization.plugin.psi

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtFile
import org.plan.research.minimization.plugin.errors.PsiFileTransformationError
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem

class TopLevelFunctionModifierManager(private val rootProject: Project) {
    val psiElementsWithBody: List<PsiWithBodyDDItem> by lazy {
        val index = ProjectRootManager.getInstance(rootProject).fileIndex
        val collectedElements = buildList<PsiWithBodyDDItem> {
            index.iterateContent { fileOrDir ->
                parse(fileOrDir).onRight { addAll(it) }
                true
            }
        }
        collectedElements
    }

    private fun parse(file: VirtualFile): Either<PsiFileTransformationError, List<PsiWithBodyDDItem>> = either {
        val psiFile = file.toPsiFile(rootProject)
        ensureNotNull(psiFile) { PsiFileTransformationError.NoAssociatedPsiFile }
        ensure(psiFile is KtFile) {
            PsiFileTransformationError.InvalidPsiFileType(
                psiFile::class.qualifiedName ?: "null"
            )
        }
        val visitor = BodyElementAcquiringKtVisitor(rootProject)
        psiFile.accept(visitor)
        visitor.collectedElements
    }

}