import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem
import org.plan.research.minimization.plugin.psi.PsiItemStorage
import org.plan.research.minimization.plugin.services.PsiAndRootManagerService
import kotlin.test.assertIs

abstract class PsiTrieTestBase : JavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/kotlin-psi"
    }

    override fun runInDispatchThread(): Boolean = false


    protected suspend fun getAllElements(): List<PsiWithBodyDDItem> {
        val service = project.service<PsiAndRootManagerService>()
        return service.findAllPsiWithBodyItems()
    }

    protected suspend fun selectElements(
        filter: (PsiWithBodyDDItem) -> Boolean
    ) = getAllElements().filter(filter)

    protected open suspend fun doTest(
        psiFile: KtFile,
        selectedPsi: List<PsiWithBodyDDItem>,
        psiProcessor: suspend (PsiElement) -> Unit
    ) {
        val allPsi = getAllElements()
        val psiTrie = PsiItemStorage.create(allPsi, selectedPsi.toSet(), project)
        psiTrie.processMarkedElements(psiFile, psiProcessor)
        withContext(Dispatchers.EDT) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        }
//        val expectedPsi = myFixture.configureByFile(expectedFile)
//        readAction {
//            kotlin.test.assertEquals(expectedPsi.text, psiFile.text)
//        }
    }

    protected val PsiWithBodyDDItem.psi: KtExpression?
        get() = runBlocking {
            project
                .service<PsiAndRootManagerService>()
                .getPsiElementFromItem(this@psi)
        }

    protected fun loadPsiFile(sourcePath: String, targetPath: String): KtFile {
        val vfsFile = myFixture.copyFileToProject(sourcePath, targetPath)
        val psiFile = runBlocking { readAction { vfsFile.toPsiFile(project) } }
        assertIs<KtFile>(psiFile)
        return psiFile
    }
}