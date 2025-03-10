import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.plan.research.minimization.plugin.settings.data.FileLevelStageData
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings
import org.plan.research.minimization.plugin.settings.MinimizationPluginState
import org.plan.research.minimization.plugin.settings.loadStateFromFile
import org.plan.research.minimization.plugin.settings.saveStateToFile
import org.plan.research.minimization.plugin.settings.data.CompilationStrategy
import org.plan.research.minimization.plugin.settings.data.DDStrategy
import org.plan.research.minimization.plugin.settings.data.ExceptionComparingStrategy
import org.plan.research.minimization.plugin.settings.data.SnapshotStrategy
import org.plan.research.minimization.plugin.settings.data.TransformationDescriptor
import org.w3c.dom.Document
import java.io.File
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertEquals

class MinimizationPluginStateTest : BasePlatformTestCase() {

    fun testSerialization() {
        val resourcePath = getFilePathFromResources("testData/MinimizationPluginState/baseState.xml")

        loadStateFromFile(project, resourcePath)
        val baseState = project.service<MinimizationPluginSettings>().state
        assertEquals(CompilationStrategy.GRADLE_IDEA, baseState.compilationStrategy)
        assertEquals("build", baseState.gradleTask)
        assertEquals(emptyList<String>(), baseState.gradleOptions)
        assertNull(baseState.temporaryProjectLocation)
        assertEquals(SnapshotStrategy.PROJECT_CLONING, baseState.snapshotStrategy)
        assertEquals(ExceptionComparingStrategy.STACKTRACE, baseState.exceptionComparingStrategy)
        assertEquals(
            MinimizationPluginState.defaultTransformations,
            baseState.minimizationTransformations
        )
        assertEquals(
            MinimizationPluginState.defaultStages,
            baseState.stages
        )

        saveStateToFile(project, "baseState1.xml")
        val file1 = File(getFilePathFromResources("testData/MinimizationPluginState/baseState.xml"))
        val file2 = File("baseState1.xml")
        assertEquals(true, areXmlFilesEqual(file1, file2), "XML files are not identical")
        file2.delete()
    }

    fun testSerialization2() {
        val resourcePath = getFilePathFromResources("testData/MinimizationPluginState/changedState.xml")
        loadStateFromFile(project, resourcePath)
        val changedState = project.service<MinimizationPluginSettings>().state
        assertEquals(CompilationStrategy.DUMB, changedState.compilationStrategy)
        assertEquals("user_build", changedState.gradleTask)
        assertEquals(listOf("--info"), changedState.gradleOptions)
        assertEquals("new-project-location", changedState.temporaryProjectLocation)
        assertEquals(SnapshotStrategy.PROJECT_CLONING, changedState.snapshotStrategy)
        assertEquals(ExceptionComparingStrategy.STACKTRACE, changedState.exceptionComparingStrategy)
        assertEquals(emptyList<TransformationDescriptor>(), changedState.minimizationTransformations)
        assertEquals(
            listOf(FileLevelStageData(DDStrategy.DD_MIN)),
            changedState.stages
        )

        saveStateToFile(project, "changedState1.xml")
        val file1 = File(getFilePathFromResources("testData/MinimizationPluginState/changedState.xml"))
        val file2 = File("changedState1.xml")
        assertEquals(true, areXmlFilesEqual(file1, file2), "XML files are not identical")
        file2.delete()
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

    private fun getFilePathFromResources(resourcePath: String): String {
        val resourceUrl = this::class.java.classLoader.getResource(resourcePath)
            ?: throw IllegalArgumentException("Resource not found: $resourcePath")
        return Paths.get(resourceUrl.toURI()).toString()
    }
}