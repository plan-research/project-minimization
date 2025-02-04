package psi.trie

import LightTestContext
import AbstractAnalysisKotlinTest
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.smartReadAction
import com.intellij.psi.PsiElement
import com.intellij.testFramework.PlatformTestUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtFile
import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.modification.item.index.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.modification.item.PsiDDItem
import org.plan.research.minimization.plugin.modification.psi.PsiUtils
import org.plan.research.minimization.plugin.modification.psi.trie.PsiTrie
import kotlin.test.assertIs

abstract class PsiTrieTestBase<ITEM, T> : AbstractAnalysisKotlinTest() where ITEM : PsiDDItem<T>, T : PsiChildrenPathIndex, T : Comparable<T>{
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/kotlin-psi"
    }

    override fun runInDispatchThread(): Boolean = false


    protected abstract suspend fun getAllElements(context: IJDDContext): List<ITEM>

    protected suspend inline fun selectElements(
        context: IJDDContext,
        filter: (ITEM) -> Boolean
    ) = getAllElements(context).filter { filter(it) }

    protected open suspend fun doTest(
        psiFile: KtFile,
        selectedPsi: List<ITEM>,
        psiProcessor: (ITEM, PsiElement) -> Unit
    ) {
        val context = LightTestContext(project)
        val diffPsi = selectedPsi.groupBy { it.localPath }
        for ((_, elements) in diffPsi) {
            val trie = PsiTrie.create(elements)
            PsiUtils.performPsiChangesAndSave(context, psiFile) {
                trie.processMarkedElements(psiFile, psiProcessor)
            }
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