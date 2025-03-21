package org.plan.research.minimization.plugin.modification.psi

import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.modification.item.PsiStubChildrenCompositionItem
import org.plan.research.minimization.plugin.modification.item.PsiStubDDItem
import org.plan.research.minimization.plugin.modification.psi.lookup.ParameterLookup
import org.plan.research.minimization.plugin.modification.psi.trie.PsiTrie

import arrow.core.raise.option
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.vfs.findFileOrDirectory
import com.intellij.psi.PsiElement
import mu.KotlinLogging
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtFile

import kotlin.io.path.pathString
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toPersistentHashMap

private typealias PersistentCache = PersistentMap<PsiStubChildrenCompositionItem, List<String>>
private typealias MutableCache = MutableMap<PsiStubChildrenCompositionItem, List<String>>

/**
 * A cache that stores the order of the parameters in the call.
 *
 * Since Kotlin support named parameters (e.g. `fun f(y = 2, x = 1)`) and default parameters,
 * the only way to look up the correct parameter to delete in call is to use analysis API.
 *
 */
class CallTraceParameterCache private constructor(
    val initialMap: PersistentCache,
) {
    fun getIndexOf(parameter: String, item: PsiStubChildrenCompositionItem) = option {
        val result = initialMap[item]?.indexOf(parameter)
        ensureNotNull(result)  // `item` is not found in map
        ensure(result != -1)  // `parameter` is not found in list of parameters
        result
    }

    fun deleteParameter(parameter: String, item: PsiStubChildrenCompositionItem) = CallTraceParameterCache(
        initialMap = initialMap.mutate { it.compute(item) { _, v -> v?.filter { it != parameter } } },
    )

    companion object {
        private val logger = KotlinLogging.logger { }
        suspend fun create(
            context: IJDDContext,
            items: List<PsiStubDDItem.CallablePsiStubDDItem>,
        ): CallTraceParameterCache {
            val callTraces = items.flatMap { it.callTraces }
            val map = buildMap {
                callTraces
                    .groupBy { it.localPath }
                    .forEach { (localPath, callTraces) ->
                        val vfs = readAction { context.projectDir.findFileOrDirectory(localPath.pathString) } ?: run {
                            logger.error { "Can't find file with localPath=$localPath" }
                            return@forEach
                        }
                        val ktFile = readAction { vfs.toPsiFile(context.indexProject) } as? KtFile ?: return@forEach
                        val trie = readAction { PsiTrie.create(callTraces) }
                        smartReadAction(context.indexProject) {
                            trie.processMarkedElements(ktFile) { a, b -> trieProcessor(a, b) }
                        }
                    }
            }
            return CallTraceParameterCache(map.toPersistentHashMap())
        }

        private fun MutableCache.trieProcessor(
            item: PsiStubChildrenCompositionItem,
            element: PsiElement,
        ) {
            element as? KtCallElement ?: run {
                logger.error { "Trie processed element ${item.childrenPath} but it is not KtCallElement" }
                return
            }
            val parameterOrder = ParameterLookup.lookupFunctionParametersOrder(element)
                ?: return
            put(item, parameterOrder)
        }
    }
}
