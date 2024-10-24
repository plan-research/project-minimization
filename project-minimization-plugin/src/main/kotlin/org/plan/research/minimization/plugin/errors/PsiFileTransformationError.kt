package org.plan.research.minimization.plugin.errors

sealed interface PsiFileTransformationError {
    data object NoAssociatedPsiFile: PsiFileTransformationError
    data class InvalidPsiFileType(val className: String): PsiFileTransformationError
}