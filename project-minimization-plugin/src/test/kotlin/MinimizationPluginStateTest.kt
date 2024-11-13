import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.FunctionLevelStage
import org.plan.research.minimization.plugin.model.state.*
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings
import org.plan.research.minimization.plugin.settings.MinimizationPluginState
import org.plan.research.minimization.plugin.settings.loadStateFromFile
import org.plan.research.minimization.plugin.settings.saveStateToFile
import org.w3c.dom.Document
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertEquals

class MinimizationPluginStateTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        project.service<MinimizationPluginSettings>().updateState(MinimizationPluginState())

        saveStateToFile(project, "baseState.xml")

        val newState = MinimizationPluginState()
        newState.compilationStrategy = CompilationStrategy.DUMB
        newState.gradleTask = "user_build"
        newState.gradleOptions = listOf("--info")
        newState.temporaryProjectLocation = "new-project-location"
        newState.snapshotStrategy = SnapshotStrategy.PROJECT_CLONING
        newState.exceptionComparingStrategy = ExceptionComparingStrategy.SIMPLE
        newState.stages = listOf(
            FileLevelStage(
                hierarchyCollectionStrategy = HierarchyCollectionStrategy.FILE_TREE,
                ddAlgorithm = DDStrategy.DD_MIN,
            )
        )
        newState.minimizationTransformations = emptyList()

        project.service<MinimizationPluginSettings>().updateState(newState)

        saveStateToFile(project, "changedState.xml")
    }

    fun testSerialization() {
        loadStateFromFile(project, "baseState.xml")
        val baseState = project.service<MinimizationPluginSettings>().state
        assertEquals(CompilationStrategy.GRADLE_IDEA, baseState.compilationStrategy)
        assertEquals("build", baseState.gradleTask)
        assertEquals(emptyList<String>(), baseState.gradleOptions)
        assertEquals("minimization-project-snapshots", baseState.temporaryProjectLocation)
        assertEquals(SnapshotStrategy.PROJECT_CLONING, baseState.snapshotStrategy)
        assertEquals(ExceptionComparingStrategy.SIMPLE, baseState.exceptionComparingStrategy)
        assertEquals(listOf(
            TransformationDescriptors.PATH_RELATIVIZATION,
        ),
            baseState.minimizationTransformations
        )
        assertEquals(
            listOf(
                FunctionLevelStage(
                    ddAlgorithm = DDStrategy.PROBABILISTIC_DD,
                ),
                FileLevelStage(
                    hierarchyCollectionStrategy = HierarchyCollectionStrategy.FILE_TREE,
                    ddAlgorithm = DDStrategy.PROBABILISTIC_DD,
                ),
            ),
            baseState.stages
        )

        saveStateToFile(project, "baseState1.xml")
        val file1 = File("baseState.xml")
        val file2 = File("baseState1.xml")
        assertEquals(true, areXmlFilesEqual(file1, file2), "XML files are not identical")
    }

    fun testSerialization2() {
        loadStateFromFile(project, "changedState.xml")
        val changedState = project.service<MinimizationPluginSettings>().state
        assertEquals(CompilationStrategy.DUMB, changedState.compilationStrategy)
        assertEquals("user_build", changedState.gradleTask)
        assertEquals(listOf("--info"), changedState.gradleOptions)
        assertEquals("new-project-location", changedState.temporaryProjectLocation)
        assertEquals(SnapshotStrategy.PROJECT_CLONING, changedState.snapshotStrategy)
        assertEquals(ExceptionComparingStrategy.SIMPLE, changedState.exceptionComparingStrategy)
        assertEquals(emptyList<TransformationDescriptors>(), changedState.minimizationTransformations)
        assertEquals(
            listOf(FileLevelStage(HierarchyCollectionStrategy.FILE_TREE, DDStrategy.DD_MIN)),
            changedState.stages
        )

        saveStateToFile(project, "changedState1.xml")
        val file1 = File("changedState.xml")
        val file2 = File("changedState1.xml")
        assertEquals(true, areXmlFilesEqual(file1, file2), "XML files are not identical")
    }

    private fun areXmlFilesEqual(file1: File, file2: File): Boolean {
        val doc1 = parseXml(file1)
        val doc2 = parseXml(file2)
        doc1.documentElement.normalize()
        doc2.documentElement.normalize()
        return doc1.isEqualNode(doc2)
    }

    private fun parseXml(file: File): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isIgnoringElementContentWhitespace = true  // Игнорируем пробелы
        return factory.newDocumentBuilder().parse(file)
    }
}