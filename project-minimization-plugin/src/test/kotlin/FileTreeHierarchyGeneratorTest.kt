import arrow.core.Either
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import kotlinx.coroutines.runBlocking
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.plugin.hierarchy.FileTreeHierarchyGenerator
import org.plan.research.minimization.plugin.model.CompilationStrategy
import org.plan.research.minimization.plugin.model.PsiDDItem
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings
import kotlin.test.assertIs

class FileTreeHierarchyGeneratorTest : LightJavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/fileTreeHierarchy"
    }

    override fun setUp() {
        super.setUp()
        project.service<MinimizationPluginSettings>().state.currentCompilationStrategy = CompilationStrategy.DUMB
    }

    private val fileTreeHierarchyGenerator = FileTreeHierarchyGenerator()

    fun testWithEmptyProject() {
        val project = myFixture.project
        val generator = generateHierarchicalDDGenerator(project)
        val firstLevel = runBlocking { generator.generateFirstLevel() }
        UsefulTestCase.assertSize(1, firstLevel.items)
        val firstElement = firstLevel.items.first().psi
        assertIs<PsiDirectory>(firstElement)
        assertEquals("src", firstElement.name)
        assertNull(runBlocking { generator.generateNextLevel(firstLevel.items) })
    }

    fun testSingleFileProject() {
        val psiFile = myFixture.configureByFile("singleFileProject")
        assertEquals(3, getPsiDepth(psiFile))
        val project = myFixture.project
        val generator = generateHierarchicalDDGenerator(project)
        val firstLevel = runBlocking { generator.generateFirstLevel() }
        UsefulTestCase.assertSize(1, firstLevel.items)
        val firstElement = firstLevel.items.first().psi
        assertIs<PsiDirectory>(firstElement)
        assertEquals("src", firstElement.name)
        val secondLevel = runBlocking { generator.generateNextLevel(firstLevel.items) }
        assertNotNull(secondLevel)
        assertSize(1, secondLevel!!.items)
        val secondElement = secondLevel.items.first().psi
        assertIs<PsiFile>(secondElement)
        assertEquals("singleFileProject", secondElement.name)
        assertNull(runBlocking { generator.generateNextLevel(secondLevel.items) })
    }

    fun testComplicatedFileStructure() {
        myFixture.copyDirectoryToProject("complicated-project", "complicated-project")
        val deepestFile = FilenameIndex.getVirtualFilesByName("10.txt", GlobalSearchScope.projectScope(project)).first()
        val psiFile = deepestFile.findPsiFile(project)!!
        assertEquals(14, getPsiDepth(psiFile))

        val project = myFixture.project
        val generator = generateHierarchicalDDGenerator(project)
        var currentLevel = runBlocking { generator.generateFirstLevel() }
        assertSize(1, currentLevel.items)
        val firstElement = currentLevel.items.first().psi
        assertIs<PsiDirectory>(firstElement)
        assertEquals("src", firstElement.name)
        currentLevel = runBlocking { generator.generateNextLevel(currentLevel.items) }!!
        assertSize(1, currentLevel.items)
        val secondElement = currentLevel.items.first().psi
        assertIs<PsiDirectory>(secondElement)
        assertEquals("complicated-project", secondElement.name)
        currentLevel = runBlocking { generator.generateNextLevel(currentLevel.items) }!!
        for (i in 0..9) {
            assertSize(2, currentLevel.items)
            val directory = currentLevel.items.map { it.psi }.filterIsInstance<PsiDirectory>()
            val file = currentLevel.items.map { it.psi }.filterIsInstance<PsiFile>()
            assertSize(1, directory)
            assertSize(1, file)
            assertEquals("$i.txt", file.first().name)
            assertEquals("$i", directory.first().name)
            val nextLevelWithoutDirectory =
                runBlocking { generator.generateNextLevel(currentLevel.items.filterNot { it.psi is PsiDirectory }) }
            assertNull(nextLevelWithoutDirectory)
            currentLevel = runBlocking { generator.generateNextLevel(currentLevel.items) }!!
        }
        assertSize(1, currentLevel.items)
        val lastElement = currentLevel.items.first().psi
        assertIs<PsiFile>(lastElement)
        assertEquals("10.txt", lastElement.name)
        assertNull(runBlocking { generator.generateNextLevel(currentLevel.items) })    }

    private fun getPsiDepth(element: PsiElement?): Int = if (element == null) 0 else getPsiDepth(element.parent) + 1
    private fun generateHierarchicalDDGenerator(project: Project): HierarchicalDDGenerator<PsiDDItem> {
        val ddGenerator = runBlocking { fileTreeHierarchyGenerator.produce(project) }
        assertIs<Either.Right<HierarchicalDDGenerator<PsiDDItem>>>(ddGenerator)
        val generator = ddGenerator.value
        return generator
    }
}