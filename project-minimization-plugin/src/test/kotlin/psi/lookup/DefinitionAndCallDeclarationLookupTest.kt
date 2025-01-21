package psi.lookup

import com.intellij.openapi.application.readAction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtUserType
import org.plan.research.minimization.plugin.psi.lookup.DefinitionAndCallDeclarationLookup
import kotlin.test.assertIs

class DefinitionAndCallDeclarationLookupTest : AbstractLookupTestBase() {
    override fun getTestDataPath(): String {
        return "${super.getTestDataPath()}/definition-and-call"
    }

    override fun lookupFunction(element: PsiElement) =
        DefinitionAndCallDeclarationLookup.getReferenceDeclaration(element)

    fun testSimple() {
        val vfs = myFixture.configureByFile("simple.kt")
        assertIs<KtFile>(vfs)
        doTest(
            vfs,
            filterFunction = { it.filter { it.second.containingFile == vfs } }
        ) { elements ->
            assertSize(1, elements)
            val (from, to) = elements.single()
            assertEquals("a", from.text)
            assertIs<KtNamedFunction>(to)
            kotlin.test.assertEquals("a", to.name)
        }
    }

    fun testConstructorInvocation() {
        val vfs = myFixture.configureByFile("constructor-invocation.kt")
        assertIs<KtFile>(vfs)
        doTest(
            vfs,
            filterFunction = { it.filter { it.second.containingFile == vfs } }
        ) { elements ->
            assertSize(4, elements)
            val constructors = elements.filter { it.second is KtPrimaryConstructor }
            assertSize(3, constructors)
            assertTrue(constructors.all { ((it.second as? KtPrimaryConstructor)?.parent as? KtClass)?.name == "A" })
            val (from, to) = elements.single { it.second is KtClass }
            assertIs<KtClass>(to)
            assertIs<KtUserType>(from.parent)
        }
    }

    fun testRecursive() {
        val vfs = myFixture.configureByFile("recursive.kt")
        assertIs<KtFile>(vfs)
        doTest(
            vfs,
            filterFunction = { it.filter { it.second.containingFile == vfs } }
        ) { elements ->
            assertSize(1, elements)
            val (from, to) = elements.first()
            assertEquals("a", from.text)
            assertIs<KtNamedFunction>(to)
            assertEquals("a", to.name)
            assertTrue(PsiTreeUtil.isAncestor(to, from, false))
        }
    }

    fun testVariables() {
        val vfs = myFixture.configureByFile("variables.kt")
        assertIs<KtFile>(vfs)
        doTest(
            vfs,
            filterFunction = { it.filter { it.second.containingFile == vfs } }
        ) { elements ->
            assertSize(5, elements)
            val toX = elements.filter { it.first.text == "x" }
            assertSize(2, toX)
            assertTrue(toX.all { (it.second as? KtProperty)?.name == "x" })

            val toY = elements.filter { it.first.text == "y" }
            assertSize(2, toY)
            assertTrue(toY.all { (it.second as? KtProperty)?.name == "y" })

            val toZ = elements.filter { it.first.text == "z" }
            assertSize(1, toZ)
            assertTrue(toZ.all { (it.second as? KtProperty)?.name == "z" })
        }
    }

    fun testGenericConstraints() {
        val vfs = myFixture.configureByFile("generic-constraints.kt")
        assertIs<KtFile>(vfs)
        doTest(
            vfs,
            filterFunction = { it.filter { it.second.containingFile == vfs } }
        ) { elements ->
            assertSize(4, elements)
            val (fromTypeConstraint, toTypeConstraint) = elements[0]
            assertIs<KtUserType>(fromTypeConstraint.parent)
            assertEquals("I", fromTypeConstraint.text)
            assertIs<KtClass>(toTypeConstraint)
            assertEquals("I", toTypeConstraint.name)

            val (fromXType, toXType) = elements[1]
            assertIs<KtUserType>(fromXType.parent)
            assertEquals("T", fromXType.text)
            assertIs<KtTypeParameter>(toXType)
            assertEquals("T", toXType.name)

            val (fromVal, toVal) = elements[2]
            assertIs<KtDotQualifiedExpression>(fromVal.parent)
            assertEquals("x", fromVal.text)
            assertIs<KtParameter>(toVal)
            assertEquals("x", toVal.name)

            val (fromFoo, toFoo) = elements[3]
            assertEquals("foo", fromFoo.text)
            assertIs<KtNamedFunction>(toFoo)
            assertEquals("foo", toFoo.name)
        }
    }
    fun testFunctionalTypes() {
        val vfs = myFixture.configureByFile("functional-types.kt")
        assertIs<KtFile>(vfs)
        doTest(
            vfs,
            filterFunction = { it.filter { it.second.containingFile == vfs } }
        ) { elements ->
            val (fromFf, toFf) = elements[0]
            assertEquals("ff", fromFf.text)
            assertIs<KtParameter>(toFf)
            assertEquals("ff", toFf.name)
            val (fromF, toF) = elements[1]
            assertEquals("f", fromF.text)
            assertIs<KtNamedFunction>(toF)
            assertEquals("f", toF.name)
            val (fromGg, toGg) = elements[2]
            assertEquals("gg", fromGg.text)
            assertIs<KtNamedFunction>(toGg)
            assertEquals("gg", toGg.name)


        }
    }
}