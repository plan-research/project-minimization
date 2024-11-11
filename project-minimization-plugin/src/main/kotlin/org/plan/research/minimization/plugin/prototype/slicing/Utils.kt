package org.plan.research.minimization.plugin.prototype.slicing

import com.intellij.openapi.application.readAction
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportInfo
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.plan.research.minimization.core.algorithm.slicing.impl.SlicingGraphTraversal
import org.plan.research.minimization.plugin.psi.PsiUtils
import kotlin.io.path.relativeTo

private suspend fun PsiSlicingNode.getName(): String {
    val psi = readAction { PsiUtils.getPsiElementOrFileFromItem(this.context, this.underlyingItem) } ?: return "\"null (null)\""
    val name = readAction {
        when (psi) {
            is KtImportDirective -> psi.text
            is KtNameReferenceExpression -> psi.text
            is KtTypeReference -> psi.text
            is KtUserType -> psi.text
            is KtFile -> psi.containingFile.virtualFile.toNioPath().relativeTo(this.context.projectDir.toNioPath())
            is KtImportInfo -> ""
            is KtPackageDirective -> psi.qualifiedName
            is KtExpression -> if (psi.text.lines().size == 1) psi.text else psi.name

            else -> "unknown"
        }
    }
    return "\"${"${psi::class.simpleName} ($name)".replace("\"", "\\\"")}\""
}

suspend fun List<PsiSlicingNode>.toDot(): String {
    val edges = mutableListOf<Pair<String, String>>()
    val traversal = object : SlicingGraphTraversal<PsiSlicingNode>() {
        override suspend fun onEdge(from: PsiSlicingNode, to: PsiSlicingNode) {
            edges.add(from.getName() to to.getName())
        }
    }
    traversal.visitAll(this)
    val edgesDot = edges.joinToString(separator = System.lineSeparator()) { (from, to) ->
        "$from -> $to"
    }
    return "digraph {\n $edgesDot \n}"
}

suspend fun PsiSlicingNode.toDot(): String = listOf(this).toDot()