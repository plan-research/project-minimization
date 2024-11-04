import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.runBlocking
import org.plan.research.minimization.plugin.model.PsiDDItem
import org.plan.research.minimization.plugin.services.MinimizationPsiManager

abstract class MinimizationPsiManagerTestBase : JavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/kotlin-psi"
    }

    override fun runInDispatchThread(): Boolean = false

    private fun compare(a: List<Int>, b: List<Int>): Int {
        val maxIndex = a.size.coerceAtLeast(b.size)
        for (i in 0 until maxIndex) {
            val aValue = a.getOrNull(i) ?: return -1
            val bValue = b.getOrNull(i) ?: return 1
            if (aValue != bValue) {
                return aValue - bValue
            }
        }
        return 0
    }
    protected fun List<PsiDDItem>.getPsi(service: MinimizationPsiManager) = runBlocking {
        sortedWith { a, b -> compare(a.childrenPath, b.childrenPath) }
            .map { service.getPsiElementFromItem(it) }
    }
}