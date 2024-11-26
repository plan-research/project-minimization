import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.EdtTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.base.plugin.artifacts.KotlinArtifacts

abstract class AbstractAnalysisKotlinTest : JavaCodeInsightFixtureTestCase() {
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