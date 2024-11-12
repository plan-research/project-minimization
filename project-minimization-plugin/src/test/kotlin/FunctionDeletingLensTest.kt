import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.plan.research.minimization.plugin.lenses.FunctionDeletingLens
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.LightIJDDContext
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService

class FunctionDeletingLensTest : PsiLensTestBase() {
    override fun getLens() = FunctionDeletingLens()
    override suspend fun getAllItems(context: IJDDContext) =
        service<MinimizationPsiManagerService>().findDeletablePsiItems(context)

    override fun getTestDataPath() = "src/test/resources/testData/function-deleting"

    fun testSimpleProject() {
        myFixture.copyDirectoryToProject("project-simple", ".")
        val context = LightIJDDContext(project)
        runBlocking { doTest(context, emptyList(), "project-simple-modified-all") }
    }

    fun testSimpleProjectOdd() {
        myFixture.copyDirectoryToProject("project-simple", ".")
        val context = LightIJDDContext(project)
        val items = runBlocking { getAllItems(context) }.filterIndexed { index, _ -> index % 2 == 0 }
        runBlocking { doTest(context, items, "project-simple-modified-odd") }
    }

    fun testSimpleProjectMultiStage() {
        myFixture.copyDirectoryToProject("project-simple", ".")
        val context = LightIJDDContext(project)
        runBlocking {
            val firstStage = getAllItems(context).filterIndexed { index, _ -> index != 1 }
            val afterTestContext = doTest(context, firstStage, "project-simple-multistage-1")
            val secondStage = readAction { firstStage.filterByPsi(context) { it !is KtClass } }
            doTest(afterTestContext, secondStage, "project-simple-multistage-2")
        }
    }
}