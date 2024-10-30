import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem
import org.plan.research.minimization.plugin.psi.FunctionModificationLens
import org.plan.research.minimization.plugin.psi.MinimizationPsiManager
import org.plan.research.minimization.plugin.services.ProjectCloningService
import kotlin.collections.filter
import kotlin.collections.find
import kotlin.collections.findLast
import kotlin.io.path.relativeTo

class FunctionModificationLensTest : JavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/function-modification"
    }

    override fun runInDispatchThread(): Boolean = false

    fun testProjectSimple() {
        myFixture.copyDirectoryToProject("project-simple", ".")
        runBlocking { doTest(emptyList(), "project-simple-modified") }
    }

    fun testMultipleFilesProject() {
        val root = myFixture.copyDirectoryToProject("project-multiple-files", ".")
        runBlocking {
            val elementsA = getAllElements(root.findFile("a.kt")!!, project)
            val elementsB = getAllElements(root.findFile("b.kt")!!, project)
            val elementsC = getAllElements(root.findFile("c.kt")!!, project)
            val elementsD = getAllElements(root.findFile("d.kt")!!, project)
            val savedElements =
                readAction {
                    listOf(
                        elementsA.findByPsi { it is KtNamedFunction && !it.hasBlockBody() && it.name == "f" }!!,
                        elementsB.findLastByPsi { it is KtLambdaExpression }!!,
                        *elementsC.filterByPsi { it is KtPropertyAccessor }.toTypedArray(),
                        elementsD[0],
                        elementsD[1]
                    )
                }

            doTest(savedElements, "project-multiple-files-modified")
        }
    }

    fun testMultiStageProject() {
        val root = myFixture.copyDirectoryToProject("project-multi-stage", ".")
        runBlocking {
            val elementsA = getAllElements(root.findFile("a.kt")!!, project)
            val elementsB = getAllElements(root.findFile("b.kt")!!, project)
            val filter = { it: PsiElement ->
                it is KtNamedFunction &&
                        runBlocking { readAction { it.hasBlockBody() } } &&
                        it.name == "stage2"
            }
            val savedElements = readAction {
                listOf(
                    elementsA.findByPsi(filter)!!,
                    elementsB.findByPsi(filter)!!
                )
            }
            doTest(savedElements, "project-multi-stage-modified-stage-1")
            writeAction {
                root.findChild("project-multi-stage-modified-stage-1")!!.delete(this)
            }
            doTest(emptyList(), "project-multi-stage-modified-stage-2")
        }
    }

    private suspend fun doTest(elements: List<PsiWithBodyDDItem>, expectedFolder: String) {
        val projectCloningService = project.service<ProjectCloningService>()
        val psiGetterService = project.service<MinimizationPsiManager>()
        val cloned = projectCloningService.clone(project)
        kotlin.test.assertNotNull(cloned)
        try {
            runBlocking {
                val lens = FunctionModificationLens()
                val context = IJDDContext(cloned, originalProject = project, currentLevel = psiGetterService.findAllPsiWithBodyItems())
                lens.focusOn(elements, context)

                val fileIndex = ProjectRootManager.getInstance(cloned).fileIndex
                val files = buildList { fileIndex.iterateContent { fileOrDir -> add(fileOrDir); true } }
                val projectRoot = cloned.guessProjectDir()!!.toNioPath()

                files.mapNotNull { readAction { it.toPsiFile(cloned) } }.forEach { file ->
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
        } finally {
            runBlocking(Dispatchers.EDT) {
                ProjectManagerEx.getInstanceEx().forceCloseProjectAsync(cloned)
            }
        }
    }

    private val PsiWithBodyDDItem.psi: KtExpression?
        get() = runBlocking {
            project
                .service<MinimizationPsiManager>()
                .getPsiElementFromItem(this@psi)
        }

    private fun List<PsiWithBodyDDItem>.findByPsi(filter: (PsiElement) -> Boolean) = find { filter(it.psi!!) }
    private fun List<PsiWithBodyDDItem>.findLastByPsi(filter: (PsiElement) -> Boolean) = findLast { filter(it.psi!!) }
    private fun List<PsiWithBodyDDItem>.filterByPsi(filter: (PsiElement) -> Boolean) = filter { filter(it.psi!!) }

    private suspend fun getAllElements(vfs: VirtualFile, project: Project): List<PsiWithBodyDDItem> {
        val service = project.service<MinimizationPsiManager>()
        val elements = service.findAllPsiWithBodyItems()
        val vfsRelativePath = project.guessProjectDir()!!.toNioPath().relativize(vfs.toNioPath())
        return elements.filter { it.localPath == vfsRelativePath }
    }
}