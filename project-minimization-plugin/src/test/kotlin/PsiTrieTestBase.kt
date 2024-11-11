import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtFile
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.LightIJDDContext
import org.plan.research.minimization.plugin.model.PsiDDItem
import org.plan.research.minimization.plugin.psi.PsiItemStorage
import org.plan.research.minimization.plugin.psi.PsiUtils
import org.plan.research.minimization.plugin.services.MinimizationPsiManager
import kotlin.test.assertIs

abstract class PsiTrieTestBase : JavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/kotlin-psi"
    }

    override fun runInDispatchThread(): Boolean = false


    protected open suspend fun getAllElements(context: IJDDContext): List<PsiDDItem> {
        val service = service<MinimizationPsiManager>()
        return service.findAllPsiWithBodyItems(context)
    }

    protected suspend inline fun selectElements(
        context: IJDDContext,
        filter: (PsiDDItem) -> Boolean
    ) = getAllElements(context).filter { filter(it) }

    protected open suspend fun doTest(
        psiFile: KtFile,
        selectedPsi: List<PsiDDItem>,
        psiProcessor: (PsiElement) -> Unit
    ) {
        val context = LightIJDDContext(project)
        val allPsi = getAllElements(context)
        val psiTrie = PsiItemStorage.create(allPsi, selectedPsi.toSet(), context)
        PsiUtils.performPsiChangesAndSave(context, psiFile) {
            psiTrie.processMarkedElements(psiFile, psiProcessor)
        }
        withContext(Dispatchers.EDT) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        }
    }


    protected fun loadPsiFile(sourcePath: String, targetPath: String): KtFile {
        val vfsFile = myFixture.copyFileToProject(sourcePath, targetPath)
        val psiFile = runBlocking { smartReadAction(project) { vfsFile.toPsiFile(project) } }
        assertIs<KtFile>(psiFile)
        return psiFile
    }
}