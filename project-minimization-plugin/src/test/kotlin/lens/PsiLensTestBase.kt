package lens

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.plan.research.minimization.plugin.lenses.BasePsiLens
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.LightIJDDContext
import org.plan.research.minimization.plugin.model.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.model.PsiDDItem
import org.plan.research.minimization.plugin.psi.PsiUtils
import org.plan.research.minimization.plugin.services.ProjectCloningService
import kotlin.io.path.relativeTo

abstract class PsiLensTestBase<ITEM, T> :
    JavaCodeInsightFixtureTestCase() where ITEM : PsiDDItem<T>, T : PsiChildrenPathIndex, T : Comparable<T> {
    override fun runInDispatchThread(): Boolean = false

    protected abstract fun getLens(): BasePsiLens<ITEM, T>
    protected abstract suspend fun getAllItems(context: IJDDContext): List<ITEM>

    protected open suspend fun doTest(
        context: LightIJDDContext,
        elements: List<ITEM>,
        expectedFolder: String
    ): LightIJDDContext {
        val projectCloningService = project.service<ProjectCloningService>()
        var cloned = projectCloningService.clone(context)
        kotlin.test.assertNotNull(cloned)
        val lens = getLens()
        val items = getAllItems(context)
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
        return cloned
    }

    @RequiresReadLock
    protected fun List<ITEM>.findByPsi(context: IJDDContext, filter: (PsiElement) -> Boolean) =
        find { filter(PsiUtils.getPsiElementFromItem(context, it)!!) }

    @RequiresReadLock
    protected fun List<ITEM>.findLastByPsi(context: IJDDContext, filter: (PsiElement) -> Boolean) =
        findLast { filter(PsiUtils.getPsiElementFromItem(context, it)!!) }

    @RequiresReadLock
    protected fun List<ITEM>.filterByPsi(context: IJDDContext, filter: (PsiElement) -> Boolean) =
        filter { filter(PsiUtils.getPsiElementFromItem(context, it)!!) }

    protected open suspend fun getAllElements(context: IJDDContext, vfs: VirtualFile): List<ITEM> {
        val elements = getAllItems(context)
        val vfsRelativePath = context.projectDir.toNioPath().relativize(vfs.toNioPath())
        return elements.filter { it.localPath == vfsRelativePath }
    }
}