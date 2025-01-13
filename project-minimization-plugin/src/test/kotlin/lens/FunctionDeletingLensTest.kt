package lens

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.findFile
import com.intellij.testFramework.PlatformTestUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.plan.research.minimization.plugin.lenses.LinearFunctionDeletingLens
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.LightIJDDContext
import org.plan.research.minimization.plugin.model.PsiStubDDItem
import org.plan.research.minimization.plugin.psi.stub.KtStub
import org.plan.research.minimization.plugin.psi.stub.KtFunctionStub
import org.plan.research.minimization.plugin.psi.withImportRefCounter
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService
import org.plan.research.minimization.plugin.services.ProjectCloningService
import kotlin.io.path.exists
import kotlin.test.assertIs

class FunctionDeletingLensTest : PsiLensTestBase<PsiStubDDItem, KtStub>() {
    override fun getLens() = LinearFunctionDeletingLens()
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

    fun testDeletingAll() {
        myFixture.copyDirectoryToProject("project-simple", ".")
        configureModules(project)
        val context = runBlocking { LightIJDDContext(project).withImportRefCounter() }
        kotlin.test.assertNotNull(context.importRefCounter)
        assertIs<LightIJDDContext>(context)

        runBlocking {
            val projectCloningService = project.service<ProjectCloningService>()
            var cloned = projectCloningService.clone(context)
            kotlin.test.assertNotNull(cloned)
            val psiFile = readAction { cloned!!.projectDir.findFile("a.kt")!!.toPsiFile(cloned!!.indexProject)!! }
            readAction { assertTrue(psiFile.isValid) }
            val lens = getLens()
            val items = getAllItems(context)
            cloned = lens.focusOn(emptyList(), cloned.copy(currentLevel = items)) as LightIJDDContext
            withContext(Dispatchers.EDT) {
                PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
            }
            readAction { assertFalse(psiFile.isValid) }
            assertFalse(cloned.projectDir.toNioPath().resolve("a.kt").exists())
        }
    }

    fun testDeletingOverriddenFromMultipleFiles() {
        myFixture.copyDirectoryToProject("project-overridden-multiple-files", ".")
        val context = runBlocking { LightIJDDContext(project).withImportRefCounter() }
        kotlin.test.assertNotNull(context.importRefCounter)
        assertIs<LightIJDDContext>(context)
        val allItems = runBlocking { getAllItems(context) }
        val items = allItems.filter { it.childrenPath.size == 1 }
        runBlocking {
            doTest(context, items, "project-overridden-multiple-files-result")
        }
    }

    fun testDeletingNonReceiver() {
        myFixture.copyDirectoryToProject("project-import-receiver", ".")
        val context = runBlocking { LightIJDDContext(project).withImportRefCounter() }
        kotlin.test.assertNotNull(context.importRefCounter)
        assertIs<LightIJDDContext>(context)
        val allItems = runBlocking { getAllItems(context) }
        val items = allItems.filter { it.childrenPath.size == 1 && it.childrenPath.singleOrNull()?.let {it is KtFunctionStub && it.name == "y"} == true }
        runBlocking {
            doTest(context, items, "project-import-receiver-result")
        }
    }
}