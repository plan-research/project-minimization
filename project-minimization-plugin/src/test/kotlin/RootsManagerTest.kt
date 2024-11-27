import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.runBlocking
import org.plan.research.minimization.plugin.model.HeavyIJDDContext
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.LightIJDDContext
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings
import org.plan.research.minimization.plugin.services.RootsManagerService
import kotlin.io.path.div
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

abstract class RootsManagerTest<C : IJDDContext> : JavaCodeInsightFixtureTestCase(), TestWithContext<C> {

    override fun runInDispatchThread(): Boolean = false

    fun testSimple() {
        runBlocking {
            val contentRoot = createTestDirectory("")
            val contentRoot1 = createTestDirectory("contentRoot1")
            val contentRoot2 = createTestDirectory("contentRoot2")
            val sourceRoot1 = createTestDirectory("contentRoot1/src/main")
            val sourceRoot2 = createTestDirectory("contentRoot2/src/test")

            setupModuleRoots(
                mapOf(
                    contentRoot to listOf(),
                    contentRoot1 to listOf(sourceRoot1),
                    contentRoot2 to listOf(sourceRoot2),
                ),
            )

            val context = createContext(project)
            val roots = smartReadAction(project) {
                service<RootsManagerService>().findPossibleRoots(context)
            }

            val expectedRoots = readAction {
                listOf(
                    VfsUtil.findRelativeFile(contentRoot1, "src")!!.toNioPath(),
                    VfsUtil.findRelativeFile(contentRoot2, "src")!!.toNioPath()
                ).map { it.relativeTo(context.projectDir.toNioPath()) }
            }

            assertEquals("The roots do not match.", expectedRoots.toSet(), roots.toSet())
        }
    }

    fun testSimpleWithIgnore1() {
        runBlocking {
            val contentRoot = createTestDirectory("")
            val contentRoot1 = createTestDirectory("contentRoot1")
            val contentRoot2 = createTestDirectory("contentRoot2")
            val sourceRoot1 = createTestDirectory("contentRoot1/src/main")
            val sourceRoot2 = createTestDirectory("contentRoot2/src/test")

            setupModuleRoots(
                mapOf(
                    contentRoot to listOf(),
                    contentRoot1 to listOf(sourceRoot1),
                    contentRoot2 to listOf(sourceRoot2),
                ),
            )

            val context = createContext(project)
            var ignorePaths by context.originalProject.service<MinimizationPluginSettings>().stateObservable.ignorePaths.mutable()
            ignorePaths = listOf(
                "contentRoot1/src/main"
            )

            val roots = smartReadAction(project) {
                service<RootsManagerService>().findPossibleRoots(context)
            }

            val expectedRoots = readAction {
                listOf(
                    VfsUtil.findRelativeFile(contentRoot2, "src")!!.toNioPath()
                ).map { it.relativeTo(context.projectDir.toNioPath()) }
            }

            assertEquals("The roots do not match.", expectedRoots.toSet(), roots.toSet())
        }
    }

    fun testSimpleWithIgnore2() {
        runBlocking {
            val contentRoot = createTestDirectory("")
            val contentRoot1 = createTestDirectory("contentRoot1")
            val contentRoot2 = createTestDirectory("contentRoot2")
            val sourceRoot1 = createTestDirectory("contentRoot1/src/main")
            val sourceRoot2 = createTestDirectory("contentRoot2/src/test")

            setupModuleRoots(
                mapOf(
                    contentRoot to listOf(),
                    contentRoot1 to listOf(sourceRoot1),
                    contentRoot2 to listOf(sourceRoot2),
                ),
            )

            val context = createContext(project)
            var ignorePaths by context.originalProject.service<MinimizationPluginSettings>().stateObservable.ignorePaths.mutable()
            ignorePaths = listOf(
                "contentRoot1"
            )

            val roots = smartReadAction(project) {
                service<RootsManagerService>().findPossibleRoots(context)
            }

            val expectedRoots = readAction {
                listOf(
                    VfsUtil.findRelativeFile(contentRoot2, "src")!!.toNioPath()
                ).map { it.relativeTo(context.projectDir.toNioPath()) }
            }

            assertEquals("The roots do not match.", expectedRoots.toSet(), roots.toSet())
        }
    }

    fun testComplex() {
        runBlocking {
            val contentRoot = createTestDirectory("")
            val contentRoot1 = createTestDirectory("contentRoot1")
            val contentRoot2 = createTestDirectory("contentRoot1/contentRoot2")
            val contentRoot3 = createTestDirectory("module/contentRoot3")

            val sourceRoot1 = createTestDirectory("contentRoot1/src")
            createTestDirectory("contentRoot1/src/main")
            createTestDirectory("contentRoot1/src/test")
            val sourceRoot2 = createTestDirectory("contentRoot1/contentRoot2/src/main")
            val sourceRoot3 = createTestDirectory("contentRoot1/contentRoot2/src/test")
            val sourceRoot4 = createTestDirectory("module/contentRoot3/src")

            setupModuleRoots(
                mapOf(
                    contentRoot to listOf(),
                    contentRoot1 to listOf(sourceRoot1),
                    contentRoot2 to listOf(sourceRoot2, sourceRoot3),
                    contentRoot3 to listOf(sourceRoot4),
                ),
            )

            val context = createContext(project)
            val roots = smartReadAction(project) {
                service<RootsManagerService>().findPossibleRoots(context)
            }

            val expectedRoots = readAction {
                listOf(
                    VfsUtil.findRelativeFile(sourceRoot1)!!.toNioPath(),
                    VfsUtil.findRelativeFile(contentRoot2, "src")!!.toNioPath(),
                    VfsUtil.findRelativeFile(sourceRoot4)!!.toNioPath(),
                ).map { it.relativeTo(context.projectDir.toNioPath()) }
            }

            assertEquals("The roots do not match.", expectedRoots.toSet(), roots.toSet())
        }
    }

    fun testComplexWithIgnore() {
        runBlocking {
            val contentRoot = createTestDirectory("")
            val contentRoot1 = createTestDirectory("contentRoot1")
            val contentRoot2 = createTestDirectory("contentRoot1/contentRoot2")
            val contentRoot3 = createTestDirectory("module/contentRoot3")

            val sourceRoot1 = createTestDirectory("contentRoot1/src")
            createTestDirectory("contentRoot1/src/main")
            createTestDirectory("contentRoot1/src/test")
            val sourceRoot2 = createTestDirectory("contentRoot1/contentRoot2/src/main")
            val sourceRoot3 = createTestDirectory("contentRoot1/contentRoot2/src/test")
            val sourceRoot4 = createTestDirectory("module/contentRoot3/src")

            setupModuleRoots(
                mapOf(
                    contentRoot to listOf(),
                    contentRoot1 to listOf(sourceRoot1),
                    contentRoot2 to listOf(sourceRoot2, sourceRoot3),
                    contentRoot3 to listOf(sourceRoot4),
                ),
            )

            val context = createContext(project)
            var ignorePaths by context.originalProject.service<MinimizationPluginSettings>().stateObservable.ignorePaths.mutable()
            ignorePaths = listOf(
                "contentRoot1"
            )

            val roots = smartReadAction(project) {
                service<RootsManagerService>().findPossibleRoots(context)
            }

            val expectedRoots = readAction {
                listOf(
                    VfsUtil.findRelativeFile(sourceRoot4)!!.toNioPath(),
                ).map { it.relativeTo(context.projectDir.toNioPath()) }
            }

            assertEquals("The roots do not match.", expectedRoots.toSet(), roots.toSet())
        }
    }

    fun testComplexWithIgnore1() {
        runBlocking {
            val contentRoot = createTestDirectory("")
            val contentRoot1 = createTestDirectory("contentRoot1")
            val contentRoot2 = createTestDirectory("contentRoot1/contentRoot2")
            val contentRoot3 = createTestDirectory("module/contentRoot3")

            val sourceRoot1 = createTestDirectory("contentRoot1/src")
            createTestDirectory("contentRoot1/src/main")
            createTestDirectory("contentRoot1/src/test")
            val sourceRoot2 = createTestDirectory("contentRoot1/contentRoot2/src/main")
            val sourceRoot3 = createTestDirectory("contentRoot1/contentRoot2/src/test")
            val sourceRoot4 = createTestDirectory("module/contentRoot3/src")

            setupModuleRoots(
                mapOf(
                    contentRoot to listOf(),
                    contentRoot1 to listOf(sourceRoot1),
                    contentRoot2 to listOf(sourceRoot2, sourceRoot3),
                    contentRoot3 to listOf(sourceRoot4),
                ),
            )

            val context = createContext(project)
            var ignorePaths by context.originalProject.service<MinimizationPluginSettings>().stateObservable.ignorePaths.mutable()
            ignorePaths = listOf(
                "contentRoot1/src/main"
            )

            val roots = smartReadAction(project) {
                service<RootsManagerService>().findPossibleRoots(context)
            }

            val expectedRoots = readAction {
                listOf(
                    VfsUtil.findRelativeFile(sourceRoot1, "test")!!.toNioPath(),
                    VfsUtil.findRelativeFile(contentRoot2, "src")!!.toNioPath(),
                    VfsUtil.findRelativeFile(sourceRoot4)!!.toNioPath(),
                ).map { it.relativeTo(context.projectDir.toNioPath()) }
            }

            assertEquals("The roots do not match.", expectedRoots.toSet(), roots.toSet())
        }
    }

    fun testComplexWithIgnore2() {
        runBlocking {
            val contentRoot = createTestDirectory("")
            val contentRoot1 = createTestDirectory("contentRoot1")
            val contentRoot2 = createTestDirectory("contentRoot1/contentRoot2")
            val contentRoot3 = createTestDirectory("module/contentRoot3")

            val sourceRoot1 = createTestDirectory("contentRoot1/src")
            createTestDirectory("contentRoot1/src/main")
            createTestDirectory("contentRoot1/src/test")
            val sourceRoot2 = createTestDirectory("contentRoot1/contentRoot2/src/main")
            val sourceRoot3 = createTestDirectory("contentRoot1/contentRoot2/src/test")
            val sourceRoot4 = createTestDirectory("module/contentRoot3/src")

            setupModuleRoots(
                mapOf(
                    contentRoot to listOf(),
                    contentRoot1 to listOf(sourceRoot1),
                    contentRoot2 to listOf(sourceRoot2, sourceRoot3),
                    contentRoot3 to listOf(sourceRoot4),
                ),
            )

            val context = createContext(project)
            var ignorePaths by context.originalProject.service<MinimizationPluginSettings>().stateObservable.ignorePaths.mutable()
            ignorePaths = listOf(
                "contentRoot1/contentRoot2/src/main"
            )

            val roots = smartReadAction(project) {
                service<RootsManagerService>().findPossibleRoots(context)
            }

            val expectedRoots = readAction {
                listOf(
                    VfsUtil.findRelativeFile(sourceRoot1)!!.toNioPath(),
                    VfsUtil.findRelativeFile(sourceRoot3)!!.toNioPath(),
                    VfsUtil.findRelativeFile(sourceRoot4)!!.toNioPath(),
                ).map { it.relativeTo(context.projectDir.toNioPath()) }
            }

            assertEquals("The roots do not match.", expectedRoots.toSet(), roots.toSet())
        }
    }

    fun testComplexWithIgnore3() {
        runBlocking {
            val contentRoot = createTestDirectory("")
            val contentRoot1 = createTestDirectory("contentRoot1")
            val contentRoot2 = createTestDirectory("contentRoot1/contentRoot2")
            val contentRoot3 = createTestDirectory("module/contentRoot3")

            val sourceRoot1 = createTestDirectory("contentRoot1/src")
            createTestDirectory("contentRoot1/src/main")
            createTestDirectory("contentRoot1/src/test")
            val sourceRoot2 = createTestDirectory("contentRoot1/contentRoot2/src/main")
            val sourceRoot3 = createTestDirectory("contentRoot1/contentRoot2/src/test")
            val sourceRoot4 = createTestDirectory("module/contentRoot3/src")

            setupModuleRoots(
                mapOf(
                    contentRoot to listOf(),
                    contentRoot1 to listOf(sourceRoot1),
                    contentRoot2 to listOf(sourceRoot2, sourceRoot3),
                    contentRoot3 to listOf(sourceRoot4),
                ),
            )

            val context = createContext(project)
            var ignorePaths by context.originalProject.service<MinimizationPluginSettings>().stateObservable.ignorePaths.mutable()
            ignorePaths = listOf(
                "contentRoot1", "contentRoot1/contentRoot2/src/main", "contentRoot1/src/main"
            )

            val roots = smartReadAction(project) {
                service<RootsManagerService>().findPossibleRoots(context)
            }

            val expectedRoots = readAction {
                listOf(
                    VfsUtil.findRelativeFile(sourceRoot4)!!.toNioPath(),
                ).map { it.relativeTo(context.projectDir.toNioPath()) }
            }

            assertEquals("The roots do not match.", expectedRoots.toSet(), roots.toSet())
        }
    }

    fun testComplexWithIgnore4() {
        runBlocking {
            val contentRoot = createTestDirectory("")
            val contentRoot1 = createTestDirectory("contentRoot1")
            val contentRoot2 = createTestDirectory("contentRoot1/contentRoot2")
            val contentRoot3 = createTestDirectory("module/contentRoot3")

            val sourceRoot1 = createTestDirectory("contentRoot1/src")
            createTestDirectory("contentRoot1/src/main")
            createTestDirectory("contentRoot1/src/test")
            val sourceRoot2 = createTestDirectory("contentRoot1/contentRoot2/src/main")
            val sourceRoot3 = createTestDirectory("contentRoot1/contentRoot2/src/test")
            val sourceRoot4 = createTestDirectory("module/contentRoot3/src")

            setupModuleRoots(
                mapOf(
                    contentRoot to listOf(),
                    contentRoot1 to listOf(sourceRoot1),
                    contentRoot2 to listOf(sourceRoot2, sourceRoot3),
                    contentRoot3 to listOf(sourceRoot4),
                ),
            )

            val context = createContext(project)
            var ignorePaths by context.originalProject.service<MinimizationPluginSettings>().stateObservable.ignorePaths.mutable()
            ignorePaths = listOf(
                "module"
            )

            val roots = smartReadAction(project) {
                service<RootsManagerService>().findPossibleRoots(context)
            }

            val expectedRoots = readAction {
                listOf(
                    VfsUtil.findRelativeFile(sourceRoot1)!!.toNioPath(),
                    VfsUtil.findRelativeFile(contentRoot2, "src")!!.toNioPath(),
                ).map { it.relativeTo(context.projectDir.toNioPath()) }
            }

            assertEquals("The roots do not match.", expectedRoots.toSet(), roots.toSet())
        }
    }

    private suspend fun createTestDirectory(name: String): VirtualFile =
        writeAction {
            val result = VfsUtil.createDirectories(project.guessProjectDir()!!.toNioPath().div(name).pathString)
                ?: error("Failed to create directory: $name")
            result
        }

    private suspend fun setupModuleRoots(contentRoots: Map<VirtualFile, List<VirtualFile>>) {
        writeAction {
            PsiTestUtil.removeAllRoots(module, null)
            contentRoots.forEach { (root, sources) ->
                PsiTestUtil.addContentRoot(module, root)
                sources.forEach { srcRoot ->
                    PsiTestUtil.addSourceRoot(module, srcRoot)
                }
            }
        }
    }
}

class RootsManagerLightTest : RootsManagerTest<LightIJDDContext>(), TestWithContext<LightIJDDContext> by TestWithLightContext()
class RootsManagerHeavyTest : RootsManagerTest<HeavyIJDDContext>(), TestWithContext<HeavyIJDDContext> by TestWithHeavyContext()
