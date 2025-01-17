package psi.graph

import AbstractAnalysisKotlinTest
import com.intellij.openapi.components.service
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.context.impl.DefaultProjectContext
import org.plan.research.minimization.plugin.model.item.PsiStubDDItem
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

            assertSize(7, graph.edges)
            graph.assertConnection<PsiIJEdge.Overload>(classB, classA)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(classB, classA)
            graph.assertConnection<PsiIJEdge.PSITreeEdge>(funF, classB)
            graph.assertConnection<PsiIJEdge.PSITreeEdge>(valX, classB)
            graph.assertConnection<PsiIJEdge.PSITreeEdge>(funG, classB)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(funG, funF)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(funG, valX)
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

            assertSize(24, graph.edges)
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
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(classGG, interfaceA)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(classE, classD)
        }
    }

    fun testComplexTypeAlias() {
        doTest("complex-typealias.kt") { graph, context ->
            val classI = graph.findByClassAndName<KtClass>(context, "I").single()
            val classA = graph.findByClassAndName<KtClass>(context, "A").single()
            val typeAliasB = graph.findByClassAndName<KtTypeAlias>(context, "B").single()
            val typeAliasC = graph.findByClassAndName<KtTypeAlias>(context, "C").single()
            val typeAliasD = graph.findByClassAndName<KtTypeAlias>(context, "D").single()

            assertSize(6, graph.edges)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(classA, classI)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(typeAliasB, classA)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(typeAliasC, classA)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(typeAliasC, classI)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(typeAliasD, typeAliasB)
            graph.assertConnection<PsiIJEdge.UsageInPSIElement>(typeAliasD, typeAliasC)
        }
    }


    private fun doTest(fileName: String, graphCheckFunction: (InstanceLevelGraph, IJDDContext) -> Unit) =
        runBlocking {
            val psiFile = myFixture.configureByFile(fileName)
            assertIs<KtFile>(psiFile)
            configureModules(myFixture.project)
            val context = DefaultProjectContext(project)
            val graph = service<MinimizationPsiManagerService>().buildDeletablePsiGraph(context)
            graphCheckFunction(graph, context)
        }

    private inline fun <reified T : KtExpression> InstanceLevelGraph.findByClassAndName(
        context: IJDDContext,
        name: String
    ) =
        vertices.filterByPsi(context) { it is T && it.name == name }

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