import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.psi.KtFile
import org.plan.research.minimization.plugin.psi.BodyElementAcquiringKtVisitor
import org.plan.research.minimization.plugin.services.PsiWithBodiesCollectorService
import kotlin.io.path.Path
import kotlin.io.path.relativeTo
import kotlin.test.assertIs

class PsiWithBodyCollectorServiceTest : JavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/kotlin-psi"
    }

    override fun runInDispatchThread(): Boolean = false

    private val testDataRoot = Path(testDataPath)

    private val allFiles = testDataRoot
        .toFile()
        .listFiles { file -> file.extension == "kt" }
        .map { it.toPath().relativeTo(testDataRoot).toString() }

    fun testSingleFiles() {
        allFiles.forEach { file ->
            doTest(listOf(file))
        }
    }
    
    fun testOnlyFunctions() {
        doTest(listOf(
            "functions.kt",
            "lambda.kt",
            "lambda-as-default.kt",
        ))
    }
    fun testOnlyLambdas() {
        doTest(listOf(
            "lambda.kt",
            "lambda-as-default.kt"
        ))
    }
    fun testAll() {
        doTest(allFiles)
    }

    private fun doTest(fileNames: List<String>) = runBlocking {
        myFixture.project.guessProjectDir()!!.children.forEach { file ->
            writeAction {
                file.delete(this)
            }
        }
        fileNames.forEach {
            myFixture.copyFileToProject(it)
        }
        val collectorService = myFixture.project.service<PsiWithBodiesCollectorService>()
        val collected = collectorService.getElementsWithBody()

        val visitor = BodyElementAcquiringKtVisitor(project)
        fileNames.forEach {
            val psiFile = myFixture.configureByFile(it)
            assertIs<KtFile>(psiFile)
            readAction {
                psiFile.accept(visitor)
            }
        }

        val psiManager = PsiManagerEx.getInstance(project)
        assertEquals(visitor.collectedElements.size, collected.size)
        readAction {
            assert(visitor.collectedElements.all { first ->
                collected.any { second ->
                    psiManager.areElementsEquivalent(
                        first.underlyingObject.element!!,
                        second.underlyingObject.element!!
                    )
                }
            })
            assert(collected.all { first ->
                visitor.collectedElements.any { second ->
                    psiManager.areElementsEquivalent(
                        first.underlyingObject.element!!,
                        second.underlyingObject.element!!
                    )
                }
            })
        }
        val projectRoot = myFixture.project.guessProjectDir()!!
        fileNames.forEach { name ->
            val vf = projectRoot.findChild(name)!!
            withContext(Dispatchers.EDT) {
                writeAction {
                    vf.delete(this)
                }
            }
        }
    }
}