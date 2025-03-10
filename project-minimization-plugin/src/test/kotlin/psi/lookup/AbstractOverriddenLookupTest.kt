package psi.lookup

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.plan.research.minimization.plugin.modification.psi.lookup.AbstractOverriddenLookup
import kotlin.test.assertIs

class AbstractOverriddenLookupTest : AbstractLookupTestBase() {
    override fun getTestDataPath() = "${super.getTestDataPath()}/abstract-overridden"
    override fun lookupFunction(element: PsiElement) = AbstractOverriddenLookup.lookupDirectlyOverridden(element)

    fun testNoObligatory() {
        val vfsFile = myFixture.configureByFile("NoObligatory.kt")
        assertIs<KtFile>(vfsFile)
        doTest(vfsFile, checkFunction =  List<*>::isEmpty)
    }

    fun testSimpleObligatory() {
        val vfsFile = myFixture.configureByFile("SimpleObligatory.kt")
        assertIs<KtFile>(vfsFile)
        doTest(vfsFile) { collectedElements ->
            assertSize(1, collectedElements)
            val (from, to) = collectedElements.single()
            assertIs<KtClass>(from)
            assertEquals("B", from.name)
            assertIs<KtNamedFunction>(to)
            assertEquals("foo", to.name)
        }
    }
    fun testInterface() {
        val vfsFile = myFixture.configureByFile("Interface.kt")
        assertIs<KtFile>(vfsFile)
        doTest(vfsFile) { collectedElements ->
            assertSize(2, collectedElements)
            val (fromVal, toVal) = collectedElements.first()
            assertIs<KtClass>(fromVal)
            assertEquals("A", fromVal.name)
            assertIs<KtProperty>(toVal)
            assertEquals("x", toVal.name)
            val (fromFun, toFun) = collectedElements.last()
            assertIs<KtClass>(fromFun)
            assertEquals("A", fromFun.name)
            assertIs<KtNamedFunction>(toFun)
            assertEquals("foo", toFun.name)
        }
    }
    fun testImplicitObligatory() {
        val vfsFile = myFixture.configureByFile("ImplicitObligatory.kt")
        assertIs<KtFile>(vfsFile)
        doTest(vfsFile) { collectedElements ->
            assertSize(2, collectedElements)
            val (fromVal, toVal) = collectedElements.first()
            assertIs<KtClass>(fromVal)
            assertEquals("B", fromVal.name)
            assertIs<KtProperty>(toVal)
            assertEquals("a", toVal.name)
            val (fromFun, toFun) = collectedElements.last()
            assertIs<KtClass>(fromFun)
            assertEquals("B", fromFun.name)
            assertIs<KtNamedFunction>(toFun)
            assertEquals("kek", toFun.name)

        }
    }
    fun testPartialImplicitObligatory() {
        val vfsFile = myFixture.configureByFile("PartialImplicitObligatory.kt")
        assertIs<KtFile>(vfsFile)
        doTest(vfsFile) { collectedElements ->
            assertSize(2, collectedElements)
            val (fromVal, toVal) = collectedElements.first()
            assertIs<KtClass>(fromVal)
            assertEquals("A", fromVal.name)
            assertIs<KtProperty>(toVal)
            assertEquals("a", toVal.name)
            val (fromFun, toFun) = collectedElements.last()
            assertIs<KtClass>(fromFun)
            assertEquals("B", fromFun.name)
            assertIs<KtNamedFunction>(toFun)
            assertEquals("kek", toFun.name)
        }
    }
    fun testMultiOverridden() {
        val vfsFile = myFixture.configureByFile("MultiOverridden.kt")
        assertIs<KtFile>(vfsFile)
        doTest(vfsFile) { collectedElements ->
            assertSize(2, collectedElements)
            val (fromFoo, toFoo) = collectedElements.first()
            assertIs<KtClass>(fromFoo)
            assertEquals("A", fromFoo.name)
            assertIs<KtNamedFunction>(toFoo)
            assertEquals("foo", toFoo.name)
            val (fromBar, toBar) = collectedElements.last()
            assertIs<KtClass>(fromBar)
            assertEquals("B", fromBar.name)
            assertIs<KtNamedFunction>(toBar)
            assertEquals("bar", toBar.name)
        }
    }
}