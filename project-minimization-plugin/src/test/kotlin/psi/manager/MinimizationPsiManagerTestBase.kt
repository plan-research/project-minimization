package psi.manager

import arrow.core.compareTo
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.model.PsiDDItem
import org.plan.research.minimization.plugin.psi.PsiUtils

abstract class MinimizationPsiManagerTestBase : JavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/kotlin-psi"
    }

    override fun runInDispatchThread(): Boolean = false

    protected fun <T> List<PsiDDItem<T>>.getPsi(context: IJDDContext) where T : PsiChildrenPathIndex, T : Comparable<T> =
        sortedWith { a, b -> a.childrenPath.compareTo(b.childrenPath) }
            .map { PsiUtils.getPsiElementFromItem(context, it) }
}