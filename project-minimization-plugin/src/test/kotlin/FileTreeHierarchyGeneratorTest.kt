import arrow.core.Either
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runWithModalProgressBlocking
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.openapi.vfs.isFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.plugin.hierarchy.FileTreeHierarchicalDDGenerator
import org.plan.research.minimization.plugin.hierarchy.FileTreeHierarchyGenerator
import org.plan.research.minimization.plugin.model.dd.IJDDContext
import org.plan.research.minimization.plugin.model.snapshot.SnapshotDecision
import org.plan.research.minimization.plugin.model.strategies.CompilationStrategy
import org.plan.research.minimization.plugin.services.SnapshottingService
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings
import org.plan.research.minimization.plugin.snapshot.CloneSnapshot
import kotlin.io.path.relativeTo
import kotlin.test.assertIs

class FileTreeHierarchyGeneratorTest : JavaCodeInsightFixtureTestCase() {
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

        val firstLevel = runWithModalProgressBlocking(project, "") { generator.generateFirstLevel() }
        UsefulTestCase.assertSize(1, firstLevel.items)

        val firstElement = firstLevel.items.first().vfs
        assertIs<VirtualFile>(firstElement)
        assertNull(runWithModalProgressBlocking(project, "") {
            generator.generateNextLevel(
                DDAlgorithmResult(
                    firstLevel.context,
                    firstLevel.items
                )
            )
        })
    }

    fun testSingleFileProject() {
        val psiFile = myFixture.configureByFile("singleFileProject")
        val projectRoot = project.guessProjectDir()!!
        val psiManager = PsiManager.getInstance(project)
        val projectRootPsi = psiManager.findDirectory(projectRoot)!!
        assertEquals(1, getPsiDepth(psiFile) - getPsiDepth(projectRootPsi))
        val project = myFixture.project
        val generator = generateHierarchicalDDGenerator(project)

        val firstLevel = runWithModalProgressBlocking(project, "") { generator.generateFirstLevel() }
        UsefulTestCase.assertSize(1, firstLevel.items)
        val firstElement = firstLevel.items.first().vfs
        assertIs<VirtualFile>(firstElement)
        assertEquals("", firstElement.toNioPath().relativeTo(projectRoot.toNioPath()).toString())

        val secondLevel = runWithModalProgressBlocking(project, "") {
            generator.generateNextLevel(
                DDAlgorithmResult(
                    firstLevel.context, firstLevel.items
                )
            )
        }
        assertNotNull(secondLevel)
        assertSize(1, secondLevel!!.items)
        val secondElement = secondLevel.items.first().vfs
        assertIs<VirtualFile>(secondElement)
        assertEquals("singleFileProject", secondElement.presentableName)
        assertNull(runWithModalProgressBlocking(project, "") {
            generator.generateNextLevel(
                DDAlgorithmResult(
                    secondLevel.context,
                    secondLevel.items
                )
            )
        })
        runWithModalProgressBlocking(project, "") { assertNotNull(secondLevel.context.snapshot.rollback().getOrNull()) }
    }

    fun testComplicatedFileStructure() {
        val snapshottingService = project.service<SnapshottingService>()
        myFixture.copyDirectoryToProject("complicated-project", "complicated-project")
        val deepestFile = FilenameIndex.getVirtualFilesByName(
            "10.txt", GlobalSearchScope.projectScope(project)
        ).first()
        val psiFile = deepestFile.findPsiFile(project)!!
        val projectRoot = project.guessProjectDir()!!
        val psiManager = PsiManager.getInstance(project)
        val projectRootPsi = psiManager.findDirectory(projectRoot)!!

        assertEquals(12, getPsiDepth(psiFile) - getPsiDepth(projectRootPsi))

        val project = myFixture.project
        val generator = generateHierarchicalDDGenerator(project)
        var currentLevel = runWithModalProgressBlocking(project, "") { generator.generateFirstLevel() }
        assertSize(1, currentLevel.items)
        val firstElement = currentLevel.items.first().vfs
        assertIs<VirtualFile>(firstElement)
        currentLevel = runWithModalProgressBlocking(project, "") {
            generator.generateNextLevel(
                DDAlgorithmResult(
                    currentLevel.context, currentLevel.items
                )
            )
        }!!
        assertSize(1, currentLevel.items)
        val secondElement = currentLevel.items.first().vfs
        assertIs<VirtualFile>(secondElement)
        assertEquals("complicated-project", secondElement.presentableName)
        currentLevel = runWithModalProgressBlocking(project, "") {
            generator.generateNextLevel(
                DDAlgorithmResult(
                    currentLevel.context, currentLevel.items
                )
            )
        }!!

        for (i in 0..9) {
            assertSize(2, currentLevel.items)
            val directory = currentLevel.items.map { it.vfs }.filter { it.isDirectory }
            val file = currentLevel.items.map { it.vfs }.filter { it.isFile }
            assertSize(1, directory)
            assertSize(1, file)
            assertEquals("$i.txt", file.first().name)
            assertEquals("$i", directory.first().name)
            runWithModalProgressBlocking(
                project,
                ""
            ) {
                snapshottingService.makeTransaction(currentLevel.context.snapshot) {
                    val nextElements =
                        DDAlgorithmResult(
                            IJDDContext(CloneSnapshot(project, null)),
                            currentLevel.items.filterNot { it.vfs.isDirectory })
                    val nextLevelWithoutDirectory = generator.generateNextLevel(nextElements)
                    assertNull(nextLevelWithoutDirectory)
                    SnapshotDecision.Rollback
                }
            }

            currentLevel = runWithModalProgressBlocking(project, "") {
                generator.generateNextLevel(
                    DDAlgorithmResult(
                        currentLevel.context,
                        currentLevel.items
                    )
                )
            }!!
        }
        assertSize(1, currentLevel.items)
        val lastElement = currentLevel.items.first().vfs
        assertIs<VirtualFile>(lastElement)
        assertEquals("10.txt", lastElement.presentableName)
        assertNull(runWithModalProgressBlocking(project, "") {
            generator.generateNextLevel(
                DDAlgorithmResult(
                    currentLevel.context,
                    currentLevel.items
                )
            )
        })
        runWithModalProgressBlocking(project, "") {
            assertNotNull(
                currentLevel.context.snapshot.rollback().getOrNull()
            )
        }
    }

    private fun getPsiDepth(element: PsiElement?): Int = if (element == null) 0 else getPsiDepth(element.parent) + 1
    private fun generateHierarchicalDDGenerator(project: Project): FileTreeHierarchicalDDGenerator {
        val snapshottingService = project.service<SnapshottingService>()
        val snapshot = runWithModalProgressBlocking(project, "") { snapshottingService.initialSnapshot().getOrNull() }
        kotlin.test.assertNotNull(snapshot)
        val ddGenerator =
            runWithModalProgressBlocking(project, "") { fileTreeHierarchyGenerator.produce(IJDDContext(snapshot)) }
        assertIs<Either.Right<FileTreeHierarchicalDDGenerator>>(ddGenerator)
        val generator = ddGenerator.value
        return generator
    }
}