package hierarchy.deletable

import AbstractAnalysisKotlinTest
import arrow.core.None
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.runBlocking
import org.plan.research.minimization.core.model.lift
import org.plan.research.minimization.plugin.hierarchy.DeletablePsiElementHierarchyGenerator
import org.plan.research.minimization.plugin.model.context.LightIJDDContext
import org.plan.research.minimization.plugin.model.context.WithImportRefCounterContext
import org.plan.research.minimization.plugin.model.item.PsiStubDDItem.NonOverriddenPsiStubDDItem
import org.plan.research.minimization.plugin.model.state.CompilationStrategy
import org.plan.research.minimization.plugin.psi.KtSourceImportRefCounter
import org.plan.research.minimization.plugin.psi.stub.KtClassBodyStub
import org.plan.research.minimization.plugin.psi.stub.KtClassStub
import org.plan.research.minimization.plugin.psi.stub.KtFunctionStub
import org.plan.research.minimization.plugin.psi.stub.KtPropertyStub
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService
import runMonadWithEmptyProgress
import kotlin.test.assertIs

class TestContext(
    projectDir: VirtualFile,
    indexProject: Project,
    originalProject: Project,
) : LightIJDDContext<TestContext>(projectDir, indexProject, originalProject),
    WithImportRefCounterContext<TestContext> {

    override val importRefCounter: KtSourceImportRefCounter
        get() = TODO()

    constructor(project: Project) : this(project.guessProjectDir()!!, project, project)

    override fun copy(projectDir: VirtualFile): TestContext =
        TestContext(projectDir, indexProject, originalProject)

    override fun copy(importRefCounter: KtSourceImportRefCounter): TestContext =
        TestContext(projectDir, indexProject, originalProject)
}

class DeletablePsiElementHierarchyGeneratorTest : AbstractAnalysisKotlinTest() {
    override fun getTestDataPath() = "src/test/resources/testData/deletablePsiHierarchy"

    override fun setUp() {
        super.setUp()
        project.service<MinimizationPluginSettings>().stateObservable.compilationStrategy.set(CompilationStrategy.DUMB)
    }

    fun testSingleLevel() {
        myFixture.copyDirectoryToProject("single-level", ".")
        TestContext(project).runMonadWithEmptyProgress {
            val items = runBlocking { service<MinimizationPsiManagerService>().findDeletablePsiItems(lift { context }) }
            assertSize(2, items)
            val hierarchy = runBlocking { DeletablePsiElementHierarchyGenerator<TestContext>(10).produce(lift { context }).getOrNull() }
            kotlin.test.assertNotNull(hierarchy)
            val level = runBlocking { hierarchy.generateFirstLevel().getOrNull()!! }
            assertSize(2, level.items)
            val (itemA, itemB) = level.items.find { it.localPath.endsWith("A.kt") } to
                    level.items.find { it.localPath.endsWith("B.kt") }
            kotlin.test.assertNotNull(itemA)
            runBlocking {
                readAction {
                    assertIs<NonOverriddenPsiStubDDItem>(itemA)
                    assertEmpty(itemA.childrenElements)
                    val stubs = itemA.childrenPath
                    assertEquals(listOf(KtClassStub("A")), stubs)
                }
            }
            kotlin.test.assertNotNull(itemB)
            runBlocking {
                readAction {
                    assertIs<NonOverriddenPsiStubDDItem>(itemB)
                    assertEmpty(itemB.childrenElements)
                    val stubs = itemB.childrenPath
                    assertEquals(listOf(KtClassStub("B")), stubs)
                }
            }
            val nextLevel = runBlocking { hierarchy.generateNextLevel(level.items) }
            assert(nextLevel.isNone())
        }
    }

    fun testMultipleLevels() {
        myFixture.copyDirectoryToProject("two-levels", ".")

        TestContext(project).runMonadWithEmptyProgress {
            val items = runBlocking { service<MinimizationPsiManagerService>().findDeletablePsiItems(lift { context }) }
            assertSize(4, items)
            val hierarchy = runBlocking { DeletablePsiElementHierarchyGenerator<TestContext>(10).produce(lift { context }).getOrNull() }
            kotlin.test.assertNotNull(hierarchy)
            val level = runBlocking { hierarchy.generateFirstLevel().getOrNull()!! }
            assertSize(2, level.items)
            val (itemA, itemB) = level.items.find { it.localPath.endsWith("A.kt") } to
                    level.items.find { it.localPath.endsWith("B.kt") }
            kotlin.test.assertNotNull(itemA)
            runBlocking {
                readAction {
                    assertIs<NonOverriddenPsiStubDDItem>(itemA)
                    assertEmpty(itemA.childrenElements)
                    val stubs = itemA.childrenPath
                    assertEquals(listOf(KtClassStub("A")), stubs)
                }
            }
            kotlin.test.assertNotNull(itemB)
            runBlocking {
                readAction {
                    assertIs<NonOverriddenPsiStubDDItem>(itemB)
                    assertEmpty(itemB.childrenElements)
                    val stubs = itemB.childrenPath
                    assertEquals(listOf(KtClassStub("B")), stubs)
                }
            }
            val nextLevel =
                runBlocking { hierarchy.generateNextLevel(level.items).getOrNull() }
            assertNotNull(nextLevel)

            assertSize(2, nextLevel!!.items)
            val itemA2 = nextLevel.items.find { it.localPath.endsWith("A.kt") }!!
            val itemB2 = nextLevel.items.find { it.localPath.endsWith("B.kt") }!!

            kotlin.test.assertNotNull(itemA2)
            runBlocking {
                readAction {
                    assertIs<NonOverriddenPsiStubDDItem>(itemA2)
                    assertEmpty(itemA2.childrenElements)
                    val stubs = itemA2.childrenPath
                    assertEquals(
                        listOf(KtClassStub("A"), KtClassBodyStub, KtFunctionStub("method", emptyList(), null, "")),
                        stubs
                    )
                }
            }
            kotlin.test.assertNotNull(itemB)
            runBlocking {
                readAction {
                    assertIs<NonOverriddenPsiStubDDItem>(itemB2)
                    assertEmpty(itemB2.childrenElements)
                    val stubs = itemB2.childrenPath
                    assertEquals(listOf(KtClassStub("B"), KtClassBodyStub, KtPropertyStub("x", null, "")), stubs)
                }
            }
            assertTrue(
                runBlocking { hierarchy.generateNextLevel(nextLevel.items).isNone() }
            )
        }
    }
    fun testTwoLevelsWithThreshold() {
        myFixture.copyDirectoryToProject("two-levels", ".")

        TestContext(project).runMonadWithEmptyProgress {
            val items = runBlocking { service<MinimizationPsiManagerService>().findDeletablePsiItems(lift { context }) }
            assertSize(4, items)
            val hierarchy = runBlocking { DeletablePsiElementHierarchyGenerator<TestContext>(1).produce(lift { context }).getOrNull() }
            kotlin.test.assertNotNull(hierarchy)
            val level = runBlocking { hierarchy.generateFirstLevel().getOrNull()!! }
            assertSize(2, level.items)
            val (itemA, itemB) = level.items.find { it.localPath.endsWith("A.kt") } to
                    level.items.find { it.localPath.endsWith("B.kt") }
            kotlin.test.assertNotNull(itemA)
            runBlocking {
                readAction {
                    assertIs<NonOverriddenPsiStubDDItem>(itemA)
                    assertEmpty(itemA.childrenElements)
                    val stubs = itemA.childrenPath
                    assertEquals(listOf(KtClassStub("A")), stubs)
                }
            }
            kotlin.test.assertNotNull(itemB)
            runBlocking {
                readAction {
                    assertIs<NonOverriddenPsiStubDDItem>(itemB)
                    assertEmpty(itemB.childrenElements)
                    val stubs = itemB.childrenPath
                    assertEquals(listOf(KtClassStub("B")), stubs)
                }
            }
            assertIs<None>(runBlocking { hierarchy.generateNextLevel(level.items) })
        }
    }
}