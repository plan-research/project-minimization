package lens

import HeavyTestContext
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.testFramework.PlatformTestUtil
import filterByPsi
import findByPsi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtParameter
import org.plan.research.minimization.plugin.modification.lenses.FunctionDeletingLens
import org.plan.research.minimization.plugin.context.IJDDContextCloner
import org.plan.research.minimization.plugin.context.LightIJDDContext
import org.plan.research.minimization.plugin.context.WithCallTraceParameterCacheContext
import org.plan.research.minimization.plugin.context.WithImportRefCounterContext
import org.plan.research.minimization.plugin.context.impl.DefaultProjectContext
import org.plan.research.minimization.plugin.modification.item.PsiStubDDItem
import org.plan.research.minimization.plugin.modification.psi.CallTraceParameterCache
import org.plan.research.minimization.plugin.modification.psi.KtSourceImportRefCounter
import org.plan.research.minimization.plugin.modification.psi.stub.KtFunctionStub
import org.plan.research.minimization.plugin.modification.psi.stub.KtPrimaryConstructorStub
import org.plan.research.minimization.plugin.modification.psi.stub.KtStub
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService
import org.plan.research.minimization.plugin.services.ProjectCloningService
import runMonad
import kotlin.io.path.exists

class TestContext(
    projectDir: VirtualFile,
    indexProject: Project,
    originalProject: Project,
    override val importRefCounter: KtSourceImportRefCounter,
    override val callTraceParameterCache: CallTraceParameterCache
) : LightIJDDContext<TestContext>(projectDir, indexProject, originalProject),
    WithImportRefCounterContext<TestContext>,
    WithCallTraceParameterCacheContext<TestContext> {

    constructor(
        project: Project,
        importRefCounter: KtSourceImportRefCounter,
        callTraceParameterCache: CallTraceParameterCache,
    ) : this(project.guessProjectDir()!!, project, project, importRefCounter, callTraceParameterCache)

    override fun copy(projectDir: VirtualFile): TestContext =
        TestContext(projectDir, indexProject, originalProject, importRefCounter, callTraceParameterCache)

    override suspend fun clone(cloner: IJDDContextCloner): TestContext? =
        cloner.cloneLight(this)

    override fun copy(importRefCounter: KtSourceImportRefCounter): TestContext =
        TestContext(projectDir, indexProject, originalProject, importRefCounter, callTraceParameterCache)

    override fun copy(callTraceParameterCache: CallTraceParameterCache): TestContext =
        TestContext(projectDir, indexProject, originalProject, importRefCounter, callTraceParameterCache)
}

class DeclarationDeletingLensTest : PsiLensTestBase<TestContext, PsiStubDDItem, KtStub>() {
    override fun getLens() = FunctionDeletingLens<TestContext>()
    override suspend fun getAllItems(context: TestContext): List<PsiStubDDItem> {
        configureModules(context.indexProject)
        return service<MinimizationPsiManagerService>()
            .findDeletablePsiItems(context, withFunctionParameters = true)
    }

    override fun getTestDataPath() = "src/test/resources/testData/function-deleting"
    private fun createContext(): TestContext {
        configureModules(project)
        val importRefCounter = runBlocking {
            KtSourceImportRefCounter.create(HeavyTestContext(project)).getOrNull()
        }
        val cache = runBlocking {
            val defaultContext = DefaultProjectContext(project)
            val allCallableItems = service<MinimizationPsiManagerService>()
                .buildDeletablePsiGraph(defaultContext, true)
                .vertexSet()
                .filterIsInstance<PsiStubDDItem.CallablePsiStubDDItem>()
            CallTraceParameterCache.create(defaultContext, allCallableItems)
        }

        kotlin.test.assertNotNull(importRefCounter)
        return TestContext(project, importRefCounter, cache)
    }

    fun testSimpleProject() {
        myFixture.copyDirectoryToProject("project-simple", ".")
        val context = createContext()

        runBlocking { doTest(context, getAllItems(context), "project-simple-modified-all") }
    }

    fun testSimpleProjectOdd() {
        myFixture.copyDirectoryToProject("project-simple", ".")
        val context = createContext()

        val allItems = runBlocking { getAllItems(context) }
        val items = allItems.filterIndexed { index, _ -> index % 2 != 0 }
        runBlocking { doTest(context, items, "project-simple-modified-odd") }
    }

    fun testSimpleProjectMultiStage() {
        myFixture.copyDirectoryToProject("project-simple", ".")
        val context = createContext()
        runBlocking {
            val firstStage =
                getAllItems(context).filter { (it.childrenPath.singleOrNull() as? KtFunctionStub)?.name == "g" }
            val afterTestContext = doTest(context, firstStage, "project-simple-multistage-1")
            val secondStage = getAllItems(context).let { readAction { it.filterByPsi(context) { it is KtClass } } }
            doTest(afterTestContext, secondStage, "project-simple-multistage-2")
        }
    }

    fun testImportOptimizingSingleReference() {
        myFixture.copyDirectoryToProject("project-import-optimizing", ".")
        val context = createContext()
        val allItems = runBlocking { getAllItems(context) }
        val items = allItems.filterNot { (it.childrenPath.singleOrNull() as? KtFunctionStub)?.name != "h" }
        runBlocking {
            doTest(context, items, "project-import-optimizing-modified-h")
        }
    }

    fun testImportOptimizingTwoReference() {
        myFixture.copyDirectoryToProject("project-import-optimizing", ".")
        val context = createContext()
        val allItems = runBlocking { getAllItems(context) }
        val items =
            runBlocking { readAction { allItems.filterByPsi(context) { it is KtNamedFunction && it.name != "h" } } }
        runBlocking {
            doTest(context, items, "project-import-optimizing-modified-f-g")
        }
    }

    fun testImportOptimizingMultiStage() {
        myFixture.copyDirectoryToProject("project-import-optimizing", ".")
        val context = createContext()
        runBlocking {
            val firstStage =
                getAllItems(context).filterNot { (it.childrenPath.singleOrNull() as? KtFunctionStub)?.name != "h" }
            val afterTestContext = doTest(context, firstStage, "project-import-optimizing-modified-h")
            val secondStage =
                getAllItems(context).let { readAction { it.filterByPsi(context) { it is KtNamedFunction } } }
            doTest(afterTestContext, secondStage, "project-import-optimizing-modified-h-stage-2")
        }
    }

    fun testStartImportMultiStage() {
        myFixture.copyDirectoryToProject("project-import-star", ".")
        configureModules(project)
        val context = createContext()
        runBlocking {
            val firstStage =
                getAllItems(context).filter { (it.childrenPath.singleOrNull() as? KtFunctionStub)?.name == "f" }
            val afterTestContext = doTest(context, firstStage, "project-import-star-stage-1")
            val secondStage =
                getAllItems(context).let { readAction { it.filterByPsi(context) { it is KtNamedFunction && it.name == "g" } } }
            doTest(afterTestContext, secondStage, "project-import-star-stage-2")
        }
    }

    fun testDeletingAll() {
        myFixture.copyDirectoryToProject("project-simple", ".")
        configureModules(project)
        val context = createContext()

        runBlocking {
            val projectCloningService = project.service<ProjectCloningService>()
            var cloned = projectCloningService.clone(context) as TestContext
            kotlin.test.assertNotNull(cloned)
            val psiFile = readAction { cloned.projectDir.findFile("a.kt")!!.toPsiFile(cloned.indexProject)!! }
            readAction { assertTrue(psiFile.isValid) }
            val lens = getLens()
            cloned = cloned.runMonad {
                lens.focusOn(emptyList())
            }
            withContext(Dispatchers.EDT) {
                PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
            }
            readAction { assertTrue(psiFile.isValid) }
            assertTrue(cloned.projectDir.toNioPath().resolve("a.kt").exists())
        }
    }

    fun testDeletingOverriddenFromMultipleFiles() {
        myFixture.copyDirectoryToProject("project-overridden-multiple-files", ".")
        val context = createContext()
        val allItems = runBlocking { getAllItems(context) }
        val items = allItems.filterNot { it.childrenPath.size == 1 }
        runBlocking {
            doTest(context, items, "project-overridden-multiple-files-result")
        }
    }

    fun testDeletingNonReceiver() {
        myFixture.copyDirectoryToProject("project-import-receiver", ".")
        val context = createContext()
        val allItems = runBlocking { getAllItems(context) }
        val items = allItems.filterNot {
            it.childrenPath.size == 1 && it.childrenPath.singleOrNull()
                ?.let { it is KtFunctionStub && it.name == "y" } == true
        }
        runBlocking {
            doTest(context, items, "project-import-receiver-result")
        }
    }

    fun testDeletingConstructorParameterSimple() {
        myFixture.copyDirectoryToProject("project-delete-constructor-parameter-simple", ".")
        val context = createContext()
        val allItems = runBlocking { getAllItems(context) }
        val items =
            allItems.filter { it.childrenPath.any { it is KtPrimaryConstructorStub } && it.childrenPath.last().name == "x" }
        runBlocking {
            doTest(context, items, "project-delete-constructor-parameter-simple-result")
        }
    }

    fun testDeletingFunctionParameters() {
        myFixture.copyDirectoryToProject("project-delete-function-parameter", ".")
        val context = createContext()
        val allItems = runBlocking { getAllItems(context) }
        val itemsToDelete = runBlocking {
            readAction {
                listOf(
                    allItems.findByPsi(context) { it is KtParameter && it.name == "b" }!!,
                    allItems.findByPsi(context) { it is KtParameter && it.name == "x" }!!
                )
            }
        }
        runBlocking {
            doTest(context, itemsToDelete, "project-delete-function-parameter-result")
        }
    }

    fun testDeletingConstructorCallWithImport() {
        myFixture.copyDirectoryToProject("project-call-deletion-import", ".")
        val context = createContext()
        val allItems = runBlocking { getAllItems(context) }
        val itemsToDelete = runBlocking {
            readAction {
                allItems.filterByPsi(context) { it is KtParameter && it.name == "x" }
            }
        }
        runBlocking {
            doTest(context, itemsToDelete, "project-call-deletion-import-result")
        }
    }

    fun testSecondaryConstructorsSimple() {
        myFixture.copyDirectoryToProject("project-secondary-constructor-simple", ".")
        val context = createContext()
        val allItems = runBlocking { getAllItems(context) }
        val itemsStage1 =
            runBlocking {
                readAction {
                    allItems.filterByPsi(context) { it is KtSecondaryConstructor && it.valueParameters.size == 1 }
                }
            }
        val itemsStage2 =
            runBlocking {
                readAction {
                    allItems.filterByPsi(context) { it is KtSecondaryConstructor && it.valueParameters.size == 2 }
                }
            }
        runBlocking {
            val test1 = doTest(context, itemsStage1, "project-secondary-constructor-simple-stage-1")
            doTest(test1, itemsStage2, "project-secondary-constructor-simple-stage-2")
        }
    }

    fun testSecondaryConstructorParameter() {
        myFixture.copyDirectoryToProject("project-secondary-constructor-parameter-simple", ".")
        val context = createContext()
        val allItems = runBlocking { getAllItems(context) }
        val item = runBlocking {
            readAction {
                allItems.filterByPsi(context) { it is KtParameter && it.name == "y" }.single()
            }
        }
        runBlocking {
            doTest(context, listOf(item), "project-secondary-constructor-parameter-simple-result")
        }
    }

    fun testSecondaryConstructorParameterLinked() {
        myFixture.copyDirectoryToProject("project-secondary-constructor-parameter-simple", ".")
        configureModules(project)
        DumbService.getInstance(project).waitForSmartMode()
        val context = createContext()
        val allItems = runBlocking { getAllItems(context) }
        val item = runBlocking {
            readAction {
                allItems.filterByPsi(context) { it is KtParameter && it.name == "x" }.single()
            }
        }
        runBlocking {
            doTest(context, listOf(item), "project-secondary-constructor-parameter-complicated-result")
        }
    }

    fun testDefaultFunctionParametersX() {
        myFixture.copyDirectoryToProject("project-default-function-parameters", ".")
        configureModules(project)
        DumbService.getInstance(project).waitForSmartMode()
        val context = createContext()
        val allItems = runBlocking { getAllItems(context) }
        val item = runBlocking {
            readAction {
                allItems.filterByPsi(context) { it is KtParameter && it.name == "x" }.single()
            }
        }
        runBlocking {
            doTest(context, listOf(item), "project-default-function-parameters-result-x")
        }
    }

    fun testDefaultFunctionParametersY() {
        myFixture.copyDirectoryToProject("project-default-function-parameters", ".")
        configureModules(project)
        DumbService.getInstance(project).waitForSmartMode()
        val context = createContext()
        val allItems = runBlocking { getAllItems(context) }
        val item = runBlocking {
            readAction {
                allItems.filterByPsi(context) { it is KtParameter && it.name == "y" }.single()
            }
        }
        runBlocking {
            doTest(context, listOf(item), "project-default-function-parameters-result-y")
        }
    }

    fun testDefaultFunctionParametersLambda() {
        myFixture.copyDirectoryToProject("project-default-function-parameters", ".")
        configureModules(project)
        DumbService.getInstance(project).waitForSmartMode()
        val context = createContext()
        val allItems = runBlocking { getAllItems(context) }
        val item = runBlocking {
            readAction {
                allItems.filterByPsi(context) { it is KtParameter && it.name == "lambda" }.single()
            }
        }
        runBlocking {
            doTest(context, listOf(item), "project-default-function-parameters-result-lambda")
        }
    }

    fun testNamedFunctionParametersSimple() {
        myFixture.copyDirectoryToProject("project-named-function-parameters", ".")
        configureModules(project)
        DumbService.getInstance(project).waitForSmartMode()
        val context = createContext()
        val allItems = runBlocking { getAllItems(context) }
        val item = runBlocking {
            readAction {
                allItems.filterByPsi(context) { it is KtParameter && it.name == "y" }.single()
            }
        }
        runBlocking {
            doTest(context, listOf(item), "project-named-function-parameters-result")
        }
    }

    fun testNamedFunctionParametersMultiStage() {
        return
        myFixture.copyDirectoryToProject("project-named-function-parameters", ".")
        configureModules(project)
        DumbService.getInstance(project).waitForSmartMode()
        val context = createContext()
        val allItems = runBlocking { getAllItems(context) }
        val item = runBlocking {
            readAction {
                allItems.filterByPsi(context) { it is KtParameter && it.name == "y" }.single()
            }
        }
        val contextAfterStage1 = runBlocking {
            doTest(context, listOf(item), "project-named-function-parameters-result")
        }
        val itemStage2 = runBlocking {
            readAction {
                allItems.filterByPsi(context) { it is KtParameter && it.name == "x" }.single()
            }
        }
        runBlocking {
            doTest(contextAfterStage1, listOf(itemStage2), "project-named-function-parameters-result-stage-2")
        }
    }
}