import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.findFile
import com.intellij.psi.PsiElement
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.plan.research.minimization.plugin.lenses.FunctionModificationLens
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.LightIJDDContext
import org.plan.research.minimization.plugin.model.PsiChildrenPathDDItem
import org.plan.research.minimization.plugin.model.IntWrapper
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService

class FunctionModificationLensTest : PsiLensTestBase<PsiChildrenPathDDItem, IntWrapper>() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/function-modification"
    }

    override fun getLens() = FunctionModificationLens()
    override suspend fun getAllItems(context: IJDDContext) =
        service<MinimizationPsiManagerService>().findAllPsiWithBodyItems(context)

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
}