package lens

import AbstractAnalysisKotlinTest
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.plan.research.minimization.plugin.lenses.BasePsiLens
import org.plan.research.minimization.plugin.model.context.LightIJDDContext
import org.plan.research.minimization.plugin.model.item.PsiDDItem
import org.plan.research.minimization.plugin.model.item.index.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.services.ProjectCloningService
import runMonad
import kotlin.io.path.relativeTo

abstract class PsiLensTestBase<C : LightIJDDContext<C>, ITEM, T> :
    AbstractAnalysisKotlinTest() where ITEM : PsiDDItem<T>, T : PsiChildrenPathIndex, T : Comparable<T> {
    override fun runInDispatchThread(): Boolean = false

    protected abstract fun getLens(): BasePsiLens<C, ITEM, T>
    protected abstract suspend fun getAllItems(context: C): List<ITEM>

    protected open suspend fun doTest(
        initialContext: C,
        elements: List<ITEM>,
        expectedFolder: String
    ): C {
        val projectCloningService = project.service<ProjectCloningService>()
        val cloned = projectCloningService.clone(initialContext) as C
        kotlin.test.assertNotNull(cloned)
        val lens = getLens()
        val items = getAllItems(cloned)

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

    protected open suspend fun getAllElements(context: C, vfs: VirtualFile): List<ITEM> {
        val elements = getAllItems(context)
        val vfsRelativePath = context.projectDir.toNioPath().relativize(vfs.toNioPath())
        return elements.filter { it.localPath == vfsRelativePath }
    }
}