import arrow.core.compareTo
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiElement
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import com.intellij.util.concurrency.annotations.RequiresReadLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.base.plugin.artifacts.KotlinArtifacts
import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.modification.item.index.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.modification.item.PsiDDItem
import org.plan.research.minimization.plugin.modification.psi.PsiUtils
import kotlin.collections.filter

abstract class AbstractAnalysisKotlinTest : JavaCodeInsightFixtureTestCase() {
    override fun runInDispatchThread(): Boolean = false
    protected fun configureModules(project: Project) = runBlocking {
        writeAction {
            val moduleManager = ModuleManager.getInstance(project)
            moduleManager.modules.forEach { module ->
                ModuleRootModificationUtil.updateModel(module) { model ->
                    val libraryTable = model.moduleLibraryTable
                    if (libraryTable.getLibraryByName("stdlib") != null) return@updateModel
                    model.moduleLibraryTable.modifiableModel.apply {
                        val library = createLibrary("stdlib")

                        library.modifiableModel.apply {
                            addRoot(
                                VfsUtil.getUrlForLibraryRoot(KotlinArtifacts.kotlinStdlib),
                                OrderRootType.CLASSES
                            )
                            commit()
                        }
                        commit()
                    }
                }
            }
        }
        withContext(Dispatchers.EDT) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        }
    }
}