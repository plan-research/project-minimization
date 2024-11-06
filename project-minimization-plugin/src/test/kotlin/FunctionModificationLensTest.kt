import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.plan.research.minimization.plugin.lenses.FunctionModificationLens
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.LightIJDDContext
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem
import org.plan.research.minimization.plugin.psi.PsiUtils
import org.plan.research.minimization.plugin.services.MinimizationPsiManager
import org.plan.research.minimization.plugin.services.ProjectCloningService
import kotlin.io.path.relativeTo

class FunctionModificationLensTest : JavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/function-modification"
    }

    override fun runInDispatchThread(): Boolean = false

    fun testProjectSimple() {
        myFixture.copyDirectoryToProject("project-simple", ".")
        val context = LightIJDDContext(project)
        runBlocking { doTest(context, emptyList(), "project-simple-modified") }
    }

    fun testMultipleFilesProject() {
        val root = myFixture.copyDirectoryToProject("project-multiple-files", ".")
        val context = LightIJDDContext(project)
        runBlocking {
            val elementsA = getAllElements(context, root.findFile("a.kt")!!)
            val elementsB = getAllElements(context, root.findFile("b.kt")!!)
            val elementsC = getAllElements(context, root.findFile("c.kt")!!)
            val elementsD = getAllElements(context, root.findFile("d.kt")!!)
            val savedElements =
                readAction {
                    listOf(
                        elementsA.findByPsi(context) { it is KtNamedFunction && !it.hasBlockBody() && it.name == "f" }!!,
                        elementsB.findLastByPsi(context) { it is KtLambdaExpression }!!,
                        *elementsC.filterByPsi(context) { it is KtPropertyAccessor }.toTypedArray(),
                        elementsD[0],
                        elementsD[1]
                    )
                }

            doTest(context, savedElements, "project-multiple-files-modified")
        }
    }

    fun testMultiStageProject() {
        val root = myFixture.copyDirectoryToProject("project-multi-stage", ".")
        val context = LightIJDDContext(project)
        runBlocking {
            val elementsA = getAllElements(context, root.findFile("a.kt")!!)
            val elementsB = getAllElements(context, root.findFile("b.kt")!!)
            val filter = { it: PsiElement ->
                it is KtNamedFunction &&
                        runBlocking { readAction { it.hasBlockBody() } } &&
                        it.name == "stage2"
            }
            val savedElements = readAction {
                listOf(
                    elementsA.findByPsi(context, filter)!!,
                    elementsB.findByPsi(context, filter)!!
                )
            }
            doTest(context, savedElements, "project-multi-stage-modified-stage-1")
            writeAction {
                root.findChild("project-multi-stage-modified-stage-1")!!.delete(this)
            }
            doTest(context, emptyList(), "project-multi-stage-modified-stage-2")
        }
    }

    private suspend fun doTest(context: LightIJDDContext, elements: List<PsiWithBodyDDItem>, expectedFolder: String) {
        val projectCloningService = project.service<ProjectCloningService>()
        val psiGetterService = service<MinimizationPsiManager>()
        var cloned = projectCloningService.clone(context)
        kotlin.test.assertNotNull(cloned)
        val lens = FunctionModificationLens()
        val items = psiGetterService.findAllPsiWithBodyItems(cloned)
        cloned = cloned.copy(currentLevel = items)
        lens.focusOn(elements, cloned)

        val files = smartReadAction(cloned.indexProject) {
            val fileIndex = ProjectRootManager.getInstance(cloned.indexProject).fileIndex
            buildList { fileIndex.iterateContentUnderDirectory(cloned.projectDir) { fileOrDir -> add(fileOrDir); true } }
        }
        val projectRoot = cloned.projectDir.toNioPath()

        files.mapNotNull { smartReadAction(cloned.indexProject) { it.toPsiFile(cloned.indexProject) } }
            .forEach { file ->
                val relativePath = file.virtualFile.toNioPathOrNull()!!.relativeTo(projectRoot)
                val expectedPsiFile = myFixture.configureByFile("$expectedFolder/$relativePath")
                readAction {
                    kotlin.test.assertEquals(
                        expectedPsiFile.text,
                        file.text,
                        "File $relativePath is not equal. Expected:\n${expectedPsiFile.text}\nActual:\n${file.text}\n"
                    )
                }
            }
    }

    private fun List<PsiWithBodyDDItem>.findByPsi(context: IJDDContext, filter: (PsiElement) -> Boolean) =
        find { filter(PsiUtils.getPsiElementFromItem(context, it)!!) }

    private fun List<PsiWithBodyDDItem>.findLastByPsi(context: IJDDContext, filter: (PsiElement) -> Boolean) =
        findLast { filter(PsiUtils.getPsiElementFromItem(context, it)!!) }

    private fun List<PsiWithBodyDDItem>.filterByPsi(context: IJDDContext, filter: (PsiElement) -> Boolean) =
        filter { filter(PsiUtils.getPsiElementFromItem(context, it)!!) }

    private suspend fun getAllElements(context: IJDDContext, vfs: VirtualFile): List<PsiWithBodyDDItem> {
        val service = service<MinimizationPsiManager>()
        val elements = service.findAllPsiWithBodyItems(context)
        val vfsRelativePath = context.projectDir.toNioPath().relativize(vfs.toNioPath())
        return elements.filter { it.localPath == vfsRelativePath }
    }
}