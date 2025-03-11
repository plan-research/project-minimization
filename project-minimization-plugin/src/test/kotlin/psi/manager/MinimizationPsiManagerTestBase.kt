package psi.manager

import AbstractAnalysisKotlinTest
import arrow.core.compareTo
import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.modification.item.index.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.modification.item.PsiDDItem
import org.plan.research.minimization.plugin.modification.psi.PsiUtils

abstract class MinimizationPsiManagerTestBase : AbstractAnalysisKotlinTest() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/kotlin-psi"
    }

    override fun runInDispatchThread(): Boolean = false
}