package org.plan.research.minimization.plugin.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.idea.structuralsearch.visitor.KotlinRecursiveElementVisitor
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem

class BodyElementAcquiringKtVisitor(rootProject: Project): KotlinRecursiveElementVisitor() {
    private val smartPointerManager = SmartPointerManager.getInstance(rootProject)

    val collectedElements: List<PsiWithBodyDDItem>
        get() = resultedElements
    private val resultedElements: MutableList<PsiWithBodyDDItem> = mutableListOf()

    @RequiresReadLock
    override fun visitClassInitializer(initializer: KtClassInitializer) {
        val pointer = smartPointerManager.createSmartPsiElementPointer(initializer)
        resultedElements.add(PsiWithBodyDDItem.ClassInitializer(pointer))
    }
    @RequiresReadLock
    override fun visitNamedFunction(function: KtNamedFunction) {
        val pointer = smartPointerManager.createSmartPsiElementPointer<KtNamedFunction>(function)
        if (!function.hasBody()) return
        if (function.hasBlockBody())
            resultedElements.add(PsiWithBodyDDItem.NamedFunctionWithBlock(pointer))
        else
            resultedElements.add(PsiWithBodyDDItem.NamedFunctionWithoutBlock(pointer))
    }

    @RequiresReadLock
    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        val pointer = smartPointerManager.createSmartPsiElementPointer<KtLambdaExpression>(lambdaExpression)
        resultedElements.add(PsiWithBodyDDItem.LambdaExpression(pointer))
    }

    @RequiresReadLock
    override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
        if (!accessor.hasBody()) return
        val pointer = smartPointerManager.createSmartPsiElementPointer<KtPropertyAccessor>(accessor)
        resultedElements.add(PsiWithBodyDDItem.PropertyAccessor(pointer))
    }

}