package org.plan.research.minimization.plugin.modification.item

import org.plan.research.minimization.plugin.modification.item.index.InstructionLookupIndex

import java.nio.file.Path

/**
 * This item is used for primary constructor parameters deletion.
 * The purpose is to lead the path to the calls of the primary constructor of the class
 * with a purpose of deleting the specified parameter.
 *
 * The invariant that has been taken in the mind was to store some prefix using [KtStub]
 * and then dive into body using [org.plan.research.minimization.plugin.modification.item.index.IntChildrenIndex].
 */
data class PsiStubChildrenCompositionItem(
    override val localPath: Path,
    override val childrenPath: List<InstructionLookupIndex>,
) : PsiDDItem<InstructionLookupIndex> {
    override fun toString(): String = "PsiStubChildrenCompositionItem(\n" +
        "                              localPath=$localPath,\n" +
        "                              childrenPath=${
            childrenPath.joinToString(
                    separator = ",\n",
                    prefix = "[\n",
                    postfix = "]",
                ) { " ".repeat(34) + it.toString() }
        })\n"
}
