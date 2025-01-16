package psi.manager

import AbstractAnalysisKotlinTest
import arrow.core.compareTo
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.item.index.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.model.item.PsiDDItem
import org.plan.research.minimization.plugin.psi.PsiUtils

abstract class MinimizationPsiManagerTestBase : AbstractAnalysisKotlinTest() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/kotlin-psi"
    }

    override fun runInDispatchThread(): Boolean = false

    protected fun <T> List<PsiDDItem<T>>.getPsi(context: IJDDContext) where T : PsiChildrenPathIndex, T : Comparable<T> =
        sortedWith { a, b -> a.childrenPath.compareTo(b.childrenPath) }
            .map { PsiUtils.getPsiElementFromItem(context, it) }
}