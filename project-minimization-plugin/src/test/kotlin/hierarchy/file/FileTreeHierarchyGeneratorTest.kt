package hierarchy.file

import LightTestContext
import arrow.core.Either
import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.openapi.vfs.isFile
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.model.lift
import org.plan.research.minimization.plugin.hierarchy.FileTreeHierarchicalDDGenerator
import org.plan.research.minimization.plugin.hierarchy.FileTreeHierarchyGenerator
import org.plan.research.minimization.plugin.model.state.CompilationStrategy
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings
import runMonadWithEmptyProgress
import kotlin.io.path.name
import kotlin.test.assertIs

class FileTreeHierarchyGeneratorTest : JavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/fileTreeHierarchy"
    }

    override fun setUp() {
        super.setUp()
        project.service<MinimizationPluginSettings>().stateObservable.compilationStrategy.set(CompilationStrategy.DUMB)
    }

    private val fileTreeHierarchyGenerator = FileTreeHierarchyGenerator<LightTestContext>()

    fun testWithEmptyProject() {
        val project = myFixture.project
        LightTestContext(project).runMonadWithEmptyProgress {
            val generator = generateHierarchicalDDGenerator(lift { context })

            val firstLevel = runWithModalProgressBlocking(project, "") {
                generator.generateFirstLevel().getOrNull()
            }

            UsefulTestCase.assertNotNull(firstLevel)
            UsefulTestCase.assertSize(1, firstLevel!!.items)

            assertNull(runWithModalProgressBlocking(project, "") {
                generator.generateNextLevel(
                    DDAlgorithmResult(firstLevel.items, emptyList())
                ).getOrNull()
            })
        }
    }

    fun testSingleFileProject() {
        val psiFile = myFixture.configureByFile("singleFileProject")
        val project = myFixture.project
        val projectRoot = project.guessProjectDir()!!
        val psiManager = PsiManager.getInstance(project)
        val projectRootPsi = psiManager.findDirectory(projectRoot)!!
        assertEquals(1, getPsiDepth(psiFile) - getPsiDepth(projectRootPsi))
        LightTestContext(project).runMonadWithEmptyProgress {
            val generator = generateHierarchicalDDGenerator(lift { context })

            val firstLevel = runWithModalProgressBlocking(project, "") {
                generator.generateFirstLevel().getOrNull()
            }
            UsefulTestCase.assertNotNull(firstLevel)
            UsefulTestCase.assertSize(1, firstLevel!!.items)

            val secondLevel = runWithModalProgressBlocking(project, "") {
                generator.generateNextLevel(
                    DDAlgorithmResult(firstLevel.items, emptyList())
                ).getOrNull()
            }
            assertNotNull(secondLevel)
            assertSize(1, secondLevel!!.items)
            val secondElement = secondLevel.items.first()
            assertEquals("singleFileProject", secondElement.localPath.name)
            assertNull(runWithModalProgressBlocking(project, "") {
                generator.generateNextLevel(
                    DDAlgorithmResult(secondLevel.items, emptyList())
                ).getOrNull()
            })
        }
    }

    fun testComplicatedFileStructure() {
        myFixture.copyDirectoryToProject("complicated-project", "complicated-project")
        val deepestFile = FilenameIndex.getVirtualFilesByName(
            "10.txt", GlobalSearchScope.projectScope(project)
        ).first()
        val project = myFixture.project

        val psiFile = deepestFile.findPsiFile(project)!!
        val projectRoot = project.guessProjectDir()!!
        val psiManager = PsiManager.getInstance(project)
        val projectRootPsi = psiManager.findDirectory(projectRoot)!!
        LightTestContext(project).runMonadWithEmptyProgress {
            assertEquals(12, getPsiDepth(psiFile) - getPsiDepth(projectRootPsi))

            val generator = generateHierarchicalDDGenerator(lift { context })
            var currentLevel =
                runWithModalProgressBlocking(project, "") { generator.generateFirstLevel().getOrNull() }!!
            assertSize(1, currentLevel.items)
            currentLevel = runWithModalProgressBlocking(project, "") {
                generator.generateNextLevel(
                    DDAlgorithmResult(currentLevel.items, emptyList())
                ).getOrNull()
            }!!
            assertSize(1, currentLevel.items)
            val secondElement = currentLevel.items.first()
            assertEquals("complicated-project", secondElement.localPath.name)
            currentLevel = runWithModalProgressBlocking(project, "") {
                generator.generateNextLevel(
                    DDAlgorithmResult(currentLevel.items, emptyList())
                ).getOrNull()
            }!!
            for (i in 0..9) {
                assertSize(2, currentLevel.items)
                val directory =
                    currentLevel.items.map { it.getVirtualFile(lift { context })!! }.filter { it.isDirectory }
                val file = currentLevel.items.map { it.getVirtualFile(lift { context })!! }.filter { it.isFile }
                assertSize(1, directory)
                assertSize(1, file)
                assertEquals("$i.txt", file.first().name)
                assertEquals("$i", directory.first().name)
                val nextElements =
                    currentLevel.items.filterNot { it.getVirtualFile(lift { context })!!.isDirectory }
                val nextLevelWithoutDirectory =
                    runWithModalProgressBlocking(project, "") { generator.generateNextLevel(DDAlgorithmResult(nextElements, emptyList())).getOrNull() }
                assertNull(nextLevelWithoutDirectory)
                currentLevel = runWithModalProgressBlocking(project, "") {
                    generator.generateNextLevel(
                        DDAlgorithmResult(currentLevel.items, emptyList())
                    ).getOrNull()
                }!!
            }
            assertSize(1, currentLevel.items)
            val lastElement = currentLevel.items.first()
            assertEquals("10.txt", lastElement.localPath.name)
            assertNull(runWithModalProgressBlocking(project, "") {
                generator.generateNextLevel(
                    DDAlgorithmResult(currentLevel.items, emptyList())
                ).getOrNull()
            })
        }
    }

    private fun getPsiDepth(element: PsiElement?): Int = if (element == null) 0 else getPsiDepth(element.parent) + 1

    private fun generateHierarchicalDDGenerator(context: LightTestContext): FileTreeHierarchicalDDGenerator<LightTestContext> {
        val ddGenerator = runWithModalProgressBlocking(project, "") { fileTreeHierarchyGenerator.produce(context) }
        assertIs<Either.Right<FileTreeHierarchicalDDGenerator<LightTestContext>>>(ddGenerator)
        val generator = ddGenerator.value
        return generator
    }
}