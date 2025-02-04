package org.plan.research.minimization.plugin.modification.item

import org.plan.research.minimization.plugin.modification.psi.stub.KtStub

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
            KtTypeAlias::class.java,
            KtSecondaryConstructor::class.java,
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

    data class CallablePsiStubDDItem(
        override val childrenElements: List<PsiStubDDItem>,
        override val localPath: Path,
        override val childrenPath: List<KtStub>,
        val callTraces: List<PsiStubChildrenCompositionItem>,
    ) : PsiStubDDItem {
        companion object {
            fun create(
                from: PsiStubDDItem,
                traces: List<PsiStubChildrenCompositionItem>,
            ) = CallablePsiStubDDItem(
                from.childrenElements,
                from.localPath,
                from.childrenPath,
                traces.distinct(),
            )
        }
    }
}
