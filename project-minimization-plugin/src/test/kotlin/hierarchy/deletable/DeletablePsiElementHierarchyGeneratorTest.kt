package hierarchy.deletable

import AbstractAnalysisKotlinTest
import arrow.core.None
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import kotlinx.coroutines.runBlocking
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.plugin.hierarchy.DeletablePsiElementHierarchyGenerator
import org.plan.research.minimization.plugin.model.LightIJDDContext
import org.plan.research.minimization.plugin.model.PsiStubDDItem.NonOverriddenPsiStubDDItem
import org.plan.research.minimization.plugin.model.state.CompilationStrategy
import org.plan.research.minimization.plugin.psi.stub.KtClassBodyStub
import org.plan.research.minimization.plugin.psi.stub.KtClassStub
import org.plan.research.minimization.plugin.psi.stub.KtFunctionStub
import org.plan.research.minimization.plugin.psi.stub.KtPropertyStub
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService
import kotlin.test.assertIs

class DeletablePsiElementHierarchyGeneratorTest : AbstractAnalysisKotlinTest() {
    override fun getTestDataPath() = "src/test/resources/testData/deletablePsiHierarchy"

    override fun setUp() {
        super.setUp()
        var compilationStrategy by project.service<MinimizationPluginSettings>().stateObservable.compilationStrategy.mutable()
        compilationStrategy = CompilationStrategy.DUMB
    }

    fun testSingleLevel() {
        myFixture.copyDirectoryToProject("single-level", ".")
        val context = LightIJDDContext(project)
        val items = runBlocking { service<MinimizationPsiManagerService>().findDeletablePsiItems(context) }
        assertSize(2, items)
        val hierarchy = runBlocking { DeletablePsiElementHierarchyGenerator(10).produce(context).getOrNull() }
        kotlin.test.assertNotNull(hierarchy)
        val level = runBlocking { hierarchy.generateFirstLevel(context).getOrNull()!! }
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
        val nextLevel = runBlocking { hierarchy.generateNextLevel(DDAlgorithmResult(context, level.items)) }
        assert(nextLevel.isNone())
    }

    fun testMultipleLevels() {
        myFixture.copyDirectoryToProject("two-levels", ".")

        val context = LightIJDDContext(project)
        val items = runBlocking { service<MinimizationPsiManagerService>().findDeletablePsiItems(context) }
        assertSize(4, items)
        val hierarchy = runBlocking { DeletablePsiElementHierarchyGenerator(10).produce(context).getOrNull() }
        kotlin.test.assertNotNull(hierarchy)
        val level = runBlocking { hierarchy.generateFirstLevel(context).getOrNull()!! }
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
        val nextLevel = runBlocking { hierarchy.generateNextLevel(DDAlgorithmResult(context, level.items)).getOrNull() }
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
            runBlocking { hierarchy.generateNextLevel(DDAlgorithmResult(context, nextLevel.items)).isNone() }
        )
    }
    fun testTwoLevelsWithThreshold() {
        myFixture.copyDirectoryToProject("two-levels", ".")

        val context = LightIJDDContext(project)
        val items = runBlocking { service<MinimizationPsiManagerService>().findDeletablePsiItems(context) }
        assertSize(4, items)
        val hierarchy = runBlocking { DeletablePsiElementHierarchyGenerator(1).produce(context).getOrNull() }
        kotlin.test.assertNotNull(hierarchy)
        val level = runBlocking { hierarchy.generateFirstLevel(context).getOrNull()!! }
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
        assertIs<None>(runBlocking { hierarchy.generateNextLevel(DDAlgorithmResult(context, level.items)) })
    }
}