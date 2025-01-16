package lens

import LightTestContext
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.PsiElement
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.plan.research.minimization.plugin.lenses.FunctionModificationLens
import org.plan.research.minimization.plugin.model.item.PsiChildrenIndexDDItem
import org.plan.research.minimization.plugin.model.item.index.IntChildrenIndex
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService
import org.plan.research.minimization.plugin.services.ProjectCloningService
import runMonad
import kotlin.io.path.relativeTo

class FunctionModificationLensTest : PsiLensTestBase<LightTestContext, PsiChildrenIndexDDItem, IntChildrenIndex>() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/function-modification"
    }

    override fun getLens() = FunctionModificationLens<LightTestContext>()
    override suspend fun getAllItems(context: LightTestContext): List<PsiChildrenIndexDDItem> {
        configureModules(context.indexProject)
        return service<MinimizationPsiManagerService>().findAllPsiWithBodyItems(context)
    }

    fun testProjectSimple() {
        myFixture.copyDirectoryToProject("project-simple", ".")
        configureModules(project)
        val context = LightTestContext(project)
        runBlocking { doTest(context, emptyList(), "project-simple-modified") }
    }

    fun testMultipleFilesProject() {
        val root = myFixture.copyDirectoryToProject("project-multiple-files", ".")
        val context = LightTestContext(project)
        configureModules(project)
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
        configureModules(project)
        val context = LightTestContext(project)
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

    override suspend fun doTest(
        initialContext: LightTestContext,
        elements: List<PsiChildrenIndexDDItem>,
        expectedFolder: String
    ): LightTestContext {
        val projectCloningService = project.service<ProjectCloningService>()
        val psiGetterService = service<MinimizationPsiManagerService>()
        val cloned = projectCloningService.clone(initialContext) as LightTestContext
        kotlin.test.assertNotNull(cloned)
        configureModules(cloned.indexProject)
        val lens = getLens()
        val items = psiGetterService.findAllPsiWithBodyItems(cloned)
        return cloned.runMonad {
            lens.focusOn(items - elements.toSet())

            val files = smartReadAction(context.indexProject) {
                val fileIndex = ProjectRootManager.getInstance(context.indexProject).fileIndex
                buildList { fileIndex.iterateContentUnderDirectory(context.projectDir) { fileOrDir -> add(fileOrDir); true } }
            }
            val projectRoot = context.projectDir.toNioPath()

            files.mapNotNull { smartReadAction(context.indexProject) { it.toPsiFile(context.indexProject) } }
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
    }

    override suspend fun getAllElements(context: LightTestContext, vfs: VirtualFile): List<PsiChildrenIndexDDItem> {
        configureModules(context.indexProject)
        val service = service<MinimizationPsiManagerService>()
        val elements = service.findAllPsiWithBodyItems(context)
        val vfsRelativePath = context.projectDir.toNioPath().relativize(vfs.toNioPath())
        return elements.filter { it.localPath == vfsRelativePath }
    }
}