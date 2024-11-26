package lens

import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.plan.research.minimization.plugin.lenses.FunctionDeletingLens
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.LightIJDDContext
import org.plan.research.minimization.plugin.model.PsiStubDDItem
import org.plan.research.minimization.plugin.psi.stub.KtStub
import org.plan.research.minimization.plugin.psi.stub.KtFunctionStub
import org.plan.research.minimization.plugin.psi.KtSourceImportRefCounter
import org.plan.research.minimization.plugin.psi.withImportRefCounter
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService
import kotlin.test.assertIs

class FunctionDeletingLensTest : PsiLensTestBase<PsiStubDDItem, KtStub>() {
    override fun getLens() = FunctionDeletingLens()
    override suspend fun getAllItems(context: IJDDContext): List<PsiStubDDItem> {
        configureModules(context.indexProject)
        return service<MinimizationPsiManagerService>()
            .findDeletablePsiItems(context)
    }

    override fun getTestDataPath() = "src/test/resources/testData/function-deleting"

    fun testSimpleProject() {
        myFixture.copyDirectoryToProject("project-simple", ".")
        val context = runBlocking { LightIJDDContext(project).withImportRefCounter() }
        kotlin.test.assertNotNull(context.importRefCounter)
        assertIs<LightIJDDContext>(context)

        runBlocking { doTest(context, emptyList(), "project-simple-modified-all") }
    }

    fun testSimpleProjectOdd() {
        myFixture.copyDirectoryToProject("project-simple", ".")
        val context = runBlocking { LightIJDDContext(project).withImportRefCounter() }
        kotlin.test.assertNotNull(context.importRefCounter)
        assertIs<LightIJDDContext>(context)

        val allItems = runBlocking { getAllItems(context) }
        val items = allItems.filterIndexed { index, _ -> index % 2 == 0 }
        runBlocking { doTest(context, items, "project-simple-modified-odd") }
    }

    fun testSimpleProjectMultiStage() {
        myFixture.copyDirectoryToProject("project-simple", ".")
        val context = runBlocking { LightIJDDContext(project).withImportRefCounter() }
        kotlin.test.assertNotNull(context.importRefCounter)
        assertIs<LightIJDDContext>(context)
        runBlocking {
            val firstStage =
                getAllItems(context).filter { (it.childrenPath.singleOrNull() as? KtFunctionStub)?.name != "g" }
            val afterTestContext = doTest(context, firstStage, "project-simple-multistage-1")
            val secondStage = readAction { firstStage.filterByPsi(context) { it !is KtClass } }
            doTest(afterTestContext, secondStage, "project-simple-multistage-2")
        }
    }

    fun testImportOptimizingSingleReference() {
        myFixture.copyDirectoryToProject("project-import-optimizing", ".")
        val context = runBlocking { LightIJDDContext(project).withImportRefCounter() }
        kotlin.test.assertNotNull(context.importRefCounter)
        assertIs<LightIJDDContext>(context)
        val allItems = runBlocking { getAllItems(context) }
        val items = allItems.filter { (it.childrenPath.singleOrNull() as? KtFunctionStub)?.name != "h" }
        runBlocking {
            doTest(context, items, "project-import-optimizing-modified-h")
        }
    }

    fun testImportOptimizingTwoReference() {
        myFixture.copyDirectoryToProject("project-import-optimizing", ".")
        val context = runBlocking { LightIJDDContext(project).withImportRefCounter() }
        kotlin.test.assertNotNull(context.importRefCounter)
        assertIs<LightIJDDContext>(context)
        val allItems = runBlocking { getAllItems(context) }
        val items = allItems.filter { (it.childrenPath.singleOrNull() as? KtFunctionStub)?.name == "h" }
        runBlocking {
            doTest(context, items, "project-import-optimizing-modified-f-g")
        }
    }

    fun testImportOptimizingMultiStage() {
        myFixture.copyDirectoryToProject("project-import-optimizing", ".")
        val context = runBlocking { LightIJDDContext(project).withImportRefCounter() }
        kotlin.test.assertNotNull(context.importRefCounter)
        assertIs<LightIJDDContext>(context)
        runBlocking {
            val firstStage =
                getAllItems(context).filter { (it.childrenPath.singleOrNull() as? KtFunctionStub)?.name != "h" }
            val afterTestContext = doTest(context, firstStage, "project-import-optimizing-modified-h")
            val secondStage = readAction { firstStage.filterByPsi(context) { it !is KtNamedFunction } }
            doTest(afterTestContext, secondStage, "project-import-optimizing-modified-h-stage-2")
        }
    }

    fun testStartImportMultiStage() {
        myFixture.copyDirectoryToProject("project-import-star", ".")
        configureModules(project)
        val context = runBlocking { LightIJDDContext(project).withImportRefCounter() }
        kotlin.test.assertNotNull(context.importRefCounter)
        assertIs<LightIJDDContext>(context)
        runBlocking {
            val firstStage =
                getAllItems(context).filter { (it.childrenPath.singleOrNull() as? KtFunctionStub)?.name != "f" }
            val afterTestContext = doTest(context, firstStage, "project-import-star-stage-1")
            val secondStage = readAction { firstStage.filterByPsi(context) { it !is KtNamedFunction } }
            doTest(afterTestContext, secondStage, "project-import-star-stage-2")
        }
    }
}