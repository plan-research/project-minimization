package psi.lookup

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.plan.research.minimization.plugin.psi.lookup.TypeDeclarationLookup
import kotlin.test.assertIs

class TypeDeclarationLookupTest : AbstractLookupTestBase() {
    override fun getTestDataPath() = "${super.getTestDataPath()}/type-declaration"
    override fun lookupFunction(element: PsiElement) = TypeDeclarationLookup.getSymbolTypeDeclarations(element)
    fun testSimple() {
        val vfs = myFixture.configureByFile("simple.kt")
        assertIs<KtFile>(vfs)
        doTest(vfs) { elements ->
            assertSize(1, elements)
            val (from, to) = elements.single()
            assertIs<KtNamedFunction>(from)
            assertEquals("f", from.name)
            assertIs<KtObjectDeclaration>(to)
            assertEquals("kotlin.Unit", to.fqName?.asString())
        }
    }

    private inline fun<reified T1: KtElement, reified T2: KtElement> checkPair(pair: Pair<PsiElement, PsiElement>, fromName: String? = null, toName: String? = null) {
        val (from, to) = pair
        assertIs<T1>(from)
        fromName?.let { assertEquals(it, from.name) }
        assertIs<T2>(to)
        toName?.let { assertEquals(it, to.name) }
    }

    fun testVariables() {
        val vfs = myFixture.configureByFile("variable.kt")
        assertIs<KtFile>(vfs)
        doTest(vfs) { elements ->
            assertSize(9, elements)
            val (pairX, pairY, pairXX, pairYY, pairZZ) = elements
            checkPair<KtProperty, KtClass>(pairX, fromName = "x", toName = "Int")
            checkPair<KtProperty, KtClass>(pairY, fromName = "y", toName = "String")
            checkPair<KtParameter, KtClass>(pairXX, fromName = "xx", toName = "String")
            checkPair<KtParameter, KtClass>(pairYY, fromName = "yy", toName = "Int")
            checkPair<KtParameter, KtClass>(pairZZ, fromName = "zz", toName = "A")
            val constructor = elements[5]
            checkPair<KtPrimaryConstructor, KtClass>(constructor, toName = "A")
            val pairA = elements[6]
            checkPair<KtClass, KtClass>(pairA, fromName = "A", toName = "A")
            kotlin.test.assertEquals(pairA.first, pairA.second)
            val z = elements[7]
            checkPair<KtProperty, KtClass>(z, fromName = "z", toName = "A")
            val pairMain = elements[8]
            checkPair<KtNamedFunction, KtClass>(pairMain, fromName = "main", toName = "A")
        }
    }
    fun testGenericConstraints() {
        val vfs = myFixture.configureByFile("generic-constraints.kt")
        assertIs<KtFile>(vfs)
        doTest(vfs) { elements ->
            assertSize(5, elements)
            val (fooPair, iPair, xPair ,constructorPair, xClassPair) = elements
            checkPair<KtNamedFunction, KtObjectDeclaration>(fooPair, fromName = "foo", toName = "Unit")
            checkPair<KtClass, KtClass>(iPair, fromName = "I", toName = "I")
            assertEquals(iPair.first, iPair.second)
            checkPair<KtParameter, KtTypeParameter>(xPair, fromName = "x", toName = "T")
            checkPair<KtPrimaryConstructor, KtClass>(constructorPair, toName = "X")
            checkPair<KtClass, KtClass>(xClassPair, fromName = "X", toName = "X")
            kotlin.test.assertEquals(xClassPair.first, xClassPair.second)
        }
    }
    fun testFunctionalTypes() {
        val vfs = myFixture.configureByFile("functional-types.kt")
        assertIs<KtFile>(vfs)
        doTest(vfs) { elements ->
            assertSize(4, elements)
            val (pairFf, pairF, pairGg, pairX) = elements
            checkPair<KtParameter, KtClass>(pairFf, fromName = "ff", toName = "Function1")
            checkPair<KtNamedFunction, KtClass>(pairF, fromName = "f", toName = "Function0")
            checkPair<KtNamedFunction, KtClass>(pairGg, fromName = "gg", toName = "Function0")
            checkPair<KtProperty, KtClass>(pairX, fromName = "x", toName = "Function0")
        }
    }
}