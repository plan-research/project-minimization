package psi.graph

import AbstractAnalysisKotlinTest
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.DumbService
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.testFramework.PlatformTestUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.context.impl.DefaultProjectContext
import org.plan.research.minimization.plugin.model.item.PsiStubDDItem
import org.plan.research.minimization.plugin.psi.PsiUtils
import org.plan.research.minimization.plugin.psi.graph.InstanceLevelGraph
import org.plan.research.minimization.plugin.psi.graph.PsiIJEdge
import org.plan.research.minimization.plugin.psi.stub.KtClassStub
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService
import kotlin.test.assertIs

class InstanceLevelGraphCollectionTest : AbstractAnalysisKotlinTest() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/graph/"
    }

    fun testSimple() {
        doTest("simple.kt") { graph, context ->
            val classA = graph.findByClassAndName<KtClass>(context, "A").single()
            val classB = graph.findByClassAndName<KtClass>(context, "B").single()
            val funF = graph.findByClassAndName<KtNamedFunction>(context, "f").single()
            val valX = graph.findByClassAndName<KtProperty>(context, "x").single()
            val funG = graph.findByClassAndName<KtNamedFunction>(context, "g").single()
            val file = graph.findByClassAndName<KtFile>(context, "simple.kt").single()
            val dir = graph.findByClassAndName<PsiDirectory>(context, null).single()

            assertSize(13, graph.edges)
            graph.assertConnection<PsiIJEdge.Overload>(classB, classA)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(classB, classA)
            graph.assertConnection<PsiIJEdge.PSITreeEdge>(funF, classB)
            graph.assertConnection<PsiIJEdge.PSITreeEdge>(valX, classB)
            graph.assertConnection<PsiIJEdge.PSITreeEdge>(funG, classB)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(funG, funF)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(funG, valX)
            listOf(classA, classB, funF, valX, funG).forEach {
                graph.assertConnection<PsiIJEdge.PSITreeEdge>(it, file)
            }
            graph.assertConnection<PsiIJEdge.FileTreeEdge>(file, dir)
        }
    }

    fun testComplexOverload() {
        doTest("complex-overload.kt") { graph, context ->
            val interfaceA = graph.findByClassAndName<KtClass>(context, "A").single()
            val funFA = graph.findByClassAndName<KtNamedFunction>(context, "f")
                .find { it.childrenPath.any { it is KtClassStub && it.name == "A" } }!!

            val interfaceB = graph.findByClassAndName<KtClass>(context, "B").single()
            val interfaceC = graph.findByClassAndName<KtClass>(context, "C").single()
            val funFC = graph.findByClassAndName<KtNamedFunction>(context, "f")
                .find { it.childrenPath.any { it is KtClassStub && it.name == "C" } }!!

            val classD = graph.findByClassAndName<KtClass>(context, "D").single()
            val overrideFunF = graph.findByClassAndName<KtNamedFunction>(context, "f")
                .find { it.childrenPath.any { it is KtClassStub && it.name == "D" } }!!
            val classE = graph.findByClassAndName<KtClass>(context, "E").single()
            val overrideOverrideFunF = graph.findByClassAndName<KtNamedFunction>(context, "f")
                .find { it.childrenPath.any { it is KtClassStub && it.name == "E" } }!!
            val classGG = graph.findByClassAndName<KtClass>(context, "GG").single()
            val funG = graph.findByClassAndName<KtNamedFunction>(context, "g").single()
            val valX = graph.findByClassAndName<KtProperty>(context, "x").single()
            val valY = graph.findByClassAndName<KtProperty>(context, "y").single()
            val parameterX = graph.findByClassAndName<KtParameter>(context, "x").single()

            val allElements = listOf(
                interfaceA,
                funFA,
                interfaceB,
                interfaceC,
                funFC,
                classD,
                overrideFunF,
                classE,
                overrideOverrideFunF,
                classGG,
                funG,
                valX,
                valY,
                parameterX
            )

            val file = graph.findByClassAndName<KtFile>(context, "complex-overload.kt").single()
            val dir = graph.findByClassAndName<PsiDirectory>(context, null).single()

            assertSize(26 + allElements.size, graph.edges)
            graph.assertConnection<PsiIJEdge.PSITreeEdge>(funFA, interfaceA)

            graph.assertConnection<PsiIJEdge.PSITreeEdge>(funFC, interfaceC)

            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(classD, interfaceA)
            graph.assertConnection<PsiIJEdge.Overload>(classD, interfaceA)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(classD, interfaceB)
            graph.assertConnection<PsiIJEdge.Overload>(classD, interfaceB)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(classD, interfaceC)
            graph.assertConnection<PsiIJEdge.Overload>(classD, interfaceC)

            graph.assertConnection<PsiIJEdge.PSITreeEdge>(overrideFunF, classD)
            graph.assertConnection<PsiIJEdge.ObligatoryOverride>(classD, overrideFunF)
            graph.assertConnection<PsiIJEdge.Overload>(overrideFunF, funFA)
            graph.assertConnection<PsiIJEdge.Overload>(overrideFunF, funFC)

            graph.assertConnection<PsiIJEdge.Overload>(classE, classD)

            graph.assertConnection<PsiIJEdge.PSITreeEdge>(overrideOverrideFunF, classE)
            graph.assertConnection<PsiIJEdge.Overload>(overrideOverrideFunF, overrideFunF)
            graph.assertConnection<PsiIJEdge.PSITreeEdge>(classGG, overrideOverrideFunF)
            graph.assertConnection<PsiIJEdge.PSITreeEdge>(funG, overrideOverrideFunF)
            graph.assertConnection<PsiIJEdge.PSITreeEdge>(valX, overrideOverrideFunF)
            graph.assertConnection<PsiIJEdge.PSITreeEdge>(valY, funG)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(funG, classD) // x2
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(overrideOverrideFunF, funG)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(classE, classD)

            graph.assertConnection<PsiIJEdge.PSITreeEdge>(parameterX, classGG)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(parameterX, interfaceA)

            allElements.forEach {
                graph.assertConnection<PsiIJEdge.PSITreeEdge>(it, file)
            }
            graph.assertConnection<PsiIJEdge.FileTreeEdge>(file, dir)
        }
    }

    fun testComplexTypeAlias() {
        doTest("complex-typealias.kt") { graph, context ->
            val classI = graph.findByClassAndName<KtClass>(context, "I").single()
            val classA = graph.findByClassAndName<KtClass>(context, "A").single()
            val typeAliasB = graph.findByClassAndName<KtTypeAlias>(context, "B").single()
            val typeAliasC = graph.findByClassAndName<KtTypeAlias>(context, "C").single()
            val typeAliasD = graph.findByClassAndName<KtTypeAlias>(context, "D").single()
            val allElements = listOf(classI, classA, typeAliasB, typeAliasC, typeAliasD)

            val file = graph.findByClassAndName<KtFile>(context, "complex-typealias.kt").single()
            val dir = graph.findByClassAndName<PsiDirectory>(context, null).single()

            assertSize(7 + allElements.size, graph.edges)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(classA, classI)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(typeAliasB, classA)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(typeAliasC, classA)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(typeAliasC, classI)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(typeAliasD, typeAliasB)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(typeAliasD, typeAliasC)

            allElements.forEach {
                graph.assertConnection<PsiIJEdge.PSITreeEdge>(it, file)
            }
            graph.assertConnection<PsiIJEdge.FileTreeEdge>(file, dir)
        }
    }


    private fun doTest(fileName: String, graphCheckFunction: suspend (InstanceLevelGraph, IJDDContext) -> Unit) =
        runBlocking {
            val psiFile = myFixture.configureByFile(fileName)
            assertIs<KtFile>(psiFile)
            configureModules(myFixture.project)
            val context = DefaultProjectContext(project)
            DumbService.getInstance(project).waitForSmartMode()
            val graph = service<MinimizationPsiManagerService>().buildDeletablePsiGraph(context)
            withContext(Dispatchers.EDT) {
                PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
            }
            graphCheckFunction(graph, context)
        }

    private suspend inline fun <reified T : PsiElement> InstanceLevelGraph.findByClassAndName(
        context: IJDDContext,
        name: String?
    ) =
        readAction {
            vertices.filterByPsi(context) { it is T && (name == null || it.name == name) }
        }

    private val PsiElement.name: String?
        get() = when (this) {
            is KtElement -> this.name
            is PsiDirectory -> this.name
            else -> "Not nameble"
        } ?: ""

    private inline fun <reified T : PsiIJEdge> InstanceLevelGraph.assertConnection(
        from: PsiStubDDItem,
        to: PsiStubDDItem
    ) {
        assertTrue(
            edgesFrom(from).isSome { it ->
                val filteredType = it.filterIsInstance<T>()
                assertTrue(filteredType.isNotEmpty())
                filteredType.any { it.to == to }
            }
        )
    }
}