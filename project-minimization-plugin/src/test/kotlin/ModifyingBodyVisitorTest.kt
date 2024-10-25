import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtFile
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem
import org.plan.research.minimization.plugin.psi.BodyElementAcquiringKtVisitor
import org.plan.research.minimization.plugin.psi.ModifyingBodyKtVisitor
import kotlin.test.assertIs

class ModifyingBodyVisitorTest : JavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/kotlin-psi"
    }

    override fun runInDispatchThread(): Boolean = false
    fun testFunctionsWithBody() {
        val vfsFile = myFixture.copyFileToProject("functions.kt", "functions_1.kt")
        val psiFile = runBlocking { readAction { vfsFile.toPsiFile(project) } }
        assertIs<KtFile>(psiFile)
        val elements = runBlocking { getAllElements(psiFile) }
        runBlocking {
            markElements(elements) {
                it is PsiWithBodyDDItem.NamedFunctionWithBlock
            }
        }

        runBlocking { doTest(psiFile, "modification-results/functions_1.kt") }
    }

    fun testFunctionsWithoutBody() {
        val vfsFile = myFixture.copyFileToProject("functions.kt", "functions_2.kt")
        val psiFile = runBlocking { readAction { vfsFile.toPsiFile(project) } }
        assertIs<KtFile>(psiFile)
        val elements = runBlocking { getAllElements(psiFile) }
        runBlocking {
            markElements(elements) {
                it is PsiWithBodyDDItem.NamedFunctionWithoutBlock
            }
        }

        runBlocking { doTest(psiFile, "modification-results/functions_2.kt") }
    }

    fun testSimpleClassWithBody() {
        val vfsFile = myFixture.copyFileToProject("simple-class.kt", "simple-class_1.kt")
        val psiFile = runBlocking { readAction { vfsFile.toPsiFile(project) } }
        assertIs<KtFile>(psiFile)
        val elements = runBlocking { getAllElements(psiFile) }
        runBlocking {
            markElements(elements) {
                it is PsiWithBodyDDItem.NamedFunctionWithBlock
            }
        }

        runBlocking { doTest(psiFile, "modification-results/simple-class_1.kt") }
    }
    fun testSimpleClassWithoutBody() {
        val vfsFile = myFixture.copyFileToProject("simple-class.kt", "simple-class_2.kt")
        val psiFile = runBlocking { readAction { vfsFile.toPsiFile(project) } }
        assertIs<KtFile>(psiFile)
        val elements = runBlocking { getAllElements(psiFile) }
        runBlocking {
            markElements(elements) {
                it is PsiWithBodyDDItem.NamedFunctionWithoutBlock
            }
        }

        runBlocking { doTest(psiFile, "modification-results/simple-class_2.kt") }
    }


    private suspend fun doTest(psiFile: KtFile, expectedFile: String) {
        val visitor = readAction { ModifyingBodyKtVisitor(project, project) }
        readAction {
            psiFile.accept(visitor)
        }
        withContext(Dispatchers.EDT) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        }
        val expectedPsi = myFixture.configureByFile(expectedFile)
        readAction {
            kotlin.test.assertEquals(expectedPsi.text, psiFile.text)
        }
    }

    private suspend fun getAllElements(psiFile: KtFile): List<PsiWithBodyDDItem> {
        val visitor = BodyElementAcquiringKtVisitor(project)
        readAction {
            psiFile.accept(visitor)
        }
        return visitor.collectedElements
    }

    private suspend fun markElements(
        elements: List<PsiWithBodyDDItem>,
        filter: suspend (PsiWithBodyDDItem) -> Boolean
    ) {
        elements.forEach { element ->
            if (filter(element)) {
                writeAction {
                    element.underlyingObject.element?.putUserData(
                        ModifyingBodyKtVisitor.MAPPED_FOR_DELETION_KEY,
                        true
                    )
                }
            }
        }
    }
}