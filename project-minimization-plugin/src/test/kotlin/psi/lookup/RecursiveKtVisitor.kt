package psi.lookup

import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtVisitorVoid

open class RecursiveKtVisitor: KtVisitorVoid() {
    override fun visitKtElement(element: KtElement) {
        super.visitKtElement(element)
        element.acceptChildren(this)
    }
}