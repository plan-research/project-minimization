package psi

import AbstractAnalysisKotlinTest
import arrow.core.compareTo
import arrow.core.some
import com.intellij.openapi.components.service
import kotlinx.coroutines.runBlocking
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.context.impl.DefaultProjectContext
import org.plan.research.minimization.plugin.model.item.PsiStubDDItem.CallablePsiStubDDItem
import org.plan.research.minimization.plugin.psi.CallTraceParameterCache
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService

class CallTraceParameterCacheTest : AbstractAnalysisKotlinTest() {
    override fun getTestDataPath(): String = "src/test/resources/testData/kotlin-psi/parameter-cache"
    fun getCallableItems(context: IJDDContext) = runBlocking {
        service<MinimizationPsiManagerService>().buildDeletablePsiGraph(
            context,
            true
        ).vertexSet().filterIsInstance<CallablePsiStubDDItem>()
    }

    fun testFunctionSimple() {
        myFixture.copyFileToProject("simple-function.kt")
        val context = DefaultProjectContext(project)
        val callableItems = getCallableItems(context)
        assertSize(6, callableItems)
        val sortedCallTraces =
            callableItems.flatMap { it.callTraces }.distinct()
                .sortedWith { a, b -> a.childrenPath.compareTo(b.childrenPath) }
        val cache = runBlocking { CallTraceParameterCache.create(context, callableItems) }
        val (entryF, entryH) = sortedCallTraces
        val parametersF = listOf("x", "y", "lambda")
        parametersF.forEachIndexed { index, parameter ->
            assertEquals(index.some(), cache.getIndexOf(parameter, entryF))
        }
        val parametersH = listOf("a", "b", "c")
        parametersH.forEachIndexed { index, parameter ->
            assertEquals(index.some(), cache.getIndexOf(parameter, entryH))
        }
    }

    fun testConstructorSimple() {
        myFixture.copyFileToProject("simple-constructor.kt")
        val context = DefaultProjectContext(project)
        val callableItems = getCallableItems(context)
        assertSize(5, callableItems)
        val sortedCallTraces =
            callableItems.flatMap { it.callTraces }.distinct()
                .sortedWith { a, b -> a.childrenPath.compareTo(b.childrenPath) }
        val cache = runBlocking { CallTraceParameterCache.create(context, callableItems) }
        val (entryA, entryB, entryC) = sortedCallTraces
        val parametersA = listOf(
            "x",
            "y"
        )
        val parametersB = listOf(
            "y",
            "z",
            "f"
        )
        val parametersC = listOf(
            "y",
            "z",
            "f"
        )
        parametersA.forEachIndexed { index, parameter ->
            assertEquals(index.some(), cache.getIndexOf(parameter, entryA))
        }
        parametersB.forEachIndexed { index, parameter ->
            assertEquals(index.some(), cache.getIndexOf(parameter, entryB))
        }
        parametersC.forEachIndexed { index, parameter ->
            assertEquals(index.some(), cache.getIndexOf(parameter, entryC))
        }
    }

    fun testFunctionParametersNamed() {
        myFixture.copyFileToProject("named-function.kt")
        val context = DefaultProjectContext(project)
        val callableItems = getCallableItems(context)
        assertSize(5, callableItems)
        val sortedCallTraces =
            callableItems.flatMap { it.callTraces }.distinct()
                .sortedWith { a, b -> a.childrenPath.compareTo(b.childrenPath) }
        val cache = runBlocking { CallTraceParameterCache.create(context, callableItems) }
        val (callTraceNormal, callTraceLambdaReversed, callTraceReversedLambda) = sortedCallTraces
        val parametersNormal = listOf(
            "x",
            "y"
        )
        val parametersLambdaReversed = listOf(
            "lambda",
            "y",
            "x"
        )
        val parametersReversedLambda = listOf(
            "y",
            "x",
            "lambda"
        )
        parametersNormal.forEachIndexed { index, parameter ->
            assertEquals(index.some(), cache.getIndexOf(parameter, callTraceNormal))
        }
        parametersLambdaReversed.forEachIndexed { index, parameter ->
            assertEquals(index.some(), cache.getIndexOf(parameter, callTraceLambdaReversed))
        }
        parametersReversedLambda.forEachIndexed { index, parameter ->
            assertEquals(index.some(), cache.getIndexOf(parameter, callTraceReversedLambda))
        }
    }

    fun testDefaultParameter() {
        myFixture.copyFileToProject("default-function.kt")
        val context = DefaultProjectContext(project)
        val callableItems = getCallableItems(context)
        assertSize(3, callableItems)
        val sortedCallTraces =
            callableItems.flatMap { it.callTraces }.distinct()
                .sortedWith { a, b -> a.childrenPath.compareTo(b.childrenPath) }
        val cache = runBlocking { CallTraceParameterCache.create(context, callableItems) }
        val (callTraceNormal, callTraceEmpty, callTraceOnlyLambda, callTraceOnlyFirst, callTraceFirstTwo) = sortedCallTraces
        val parametersNormal = listOf("x", "y", "lambda")
        val parametersEmpty = emptyList<String>()
        val parametersOnlyLambda = listOf("lambda")
        val parametersOnlyFirst = listOf("x")
        val parametersFirstTwo = listOf("x", "y")

        parametersNormal.forEachIndexed { index, parameter ->
            assertEquals(index.some(), cache.getIndexOf(parameter, callTraceNormal))
        }
        parametersEmpty.forEachIndexed { index, parameter ->
            assertEquals(index.some(), cache.getIndexOf(parameter, callTraceEmpty))
        }
        parametersOnlyLambda.forEachIndexed { index, parameter ->
            assertEquals(index.some(), cache.getIndexOf(parameter, callTraceOnlyLambda))
        }
        parametersOnlyFirst.forEachIndexed { index, parameter ->
            assertEquals(index.some(), cache.getIndexOf(parameter, callTraceOnlyFirst))
        }
        parametersFirstTwo.forEachIndexed { index, parameter ->
            assertEquals(index.some(), cache.getIndexOf(parameter, callTraceFirstTwo))
        }
    }
}