package org.plan.research.minimization.plugin.model.item

import org.plan.research.minimization.plugin.psi.stub.KtStub

import org.jetbrains.kotlin.psi.*

import java.nio.file.Path

typealias ClassKtExpression = Class<out KtExpression>
typealias ClassDeclarationWithBody = Class<out KtDeclarationWithBody>

sealed interface PsiStubDDItem : PsiDDItem<KtStub> {
    val childrenElements: List<PsiStubDDItem>

    companion object {
        val DELETABLE_PSI_INSIDE_FUNCTION_CLASSES: List<ClassKtExpression> = listOf(
            KtNamedFunction::class.java,
            KtClass::class.java,
            KtObjectDeclaration::class.java,
        )
        val DELETABLE_PSI_JAVA_CLASSES: List<ClassKtExpression> =
            DELETABLE_PSI_INSIDE_FUNCTION_CLASSES + listOf(
                KtProperty::class.java,
            )
    }
    data class NonOverriddenPsiStubDDItem(
        override val localPath: Path,
        override val childrenPath: List<KtStub>,
    ) : PsiStubDDItem {
        override val childrenElements: List<PsiStubDDItem>
            get() = emptyList()
    }

    data class OverriddenPsiStubDDItem(
        override val localPath: Path,
        override val childrenPath: List<KtStub>,
        override val childrenElements: List<PsiStubDDItem>,
    ) : PsiStubDDItem
}
