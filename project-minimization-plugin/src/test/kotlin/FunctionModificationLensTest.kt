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
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtFile
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem
import org.plan.research.minimization.plugin.psi.BodyElementAcquiringKtVisitor
import org.plan.research.minimization.plugin.psi.FunctionModificationLens
import org.plan.research.minimization.plugin.services.ProjectCloningService
import kotlin.io.path.relativeTo
import kotlin.test.assertIs

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
                        elementsA.find { it is PsiWithBodyDDItem.NamedFunctionWithoutBlock && it.underlyingObject.element!!.name == "f" }!!,
                        elementsB.findLast { it is PsiWithBodyDDItem.LambdaExpression }!!,
                        *elementsC.filter { it is PsiWithBodyDDItem.PropertyAccessor }.toTypedArray(),
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
            val savedElements = readAction {
                listOf(
                    elementsA.find { it is PsiWithBodyDDItem.NamedFunctionWithBlock && it.underlyingObject.element!!.name == "stage2" }!!,
                    elementsB.find { it is PsiWithBodyDDItem.NamedFunctionWithBlock && it.underlyingObject.element!!.name == "stage2" }!!
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
        val cloned = projectCloningService.clone(project)
        kotlin.test.assertNotNull(cloned)
        try {
            runBlocking {
                val lens = FunctionModificationLens()
                val context = IJDDContext(cloned, originalProject = project)
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

    private suspend fun getAllElements(psiFile: KtFile): List<PsiWithBodyDDItem> {
        val visitor = BodyElementAcquiringKtVisitor(project)
        readAction {
            psiFile.accept(visitor)
        }
        return visitor.collectedElements
    }

    private suspend fun getAllElements(vfs: VirtualFile, project: Project): List<PsiWithBodyDDItem> {
        val psiFile = readAction { vfs.toPsiFile(project) }
        assertIs<KtFile>(psiFile)
        return getAllElements(psiFile)
    }
}