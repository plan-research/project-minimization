import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.state.*
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings
import org.plan.research.minimization.plugin.settings.AppSettingsComponent

class SettingsUITest : BasePlatformTestCase() {

    private lateinit var component: AppSettingsComponent

    override fun setUp() {
        super.setUp()
        component = AppSettingsComponent(project)
    }

    fun testUpdateSettings() {
        val settings = project.service<MinimizationPluginSettings>().stateObservable
        var compilationStrategy by settings.compilationStrategy.mutable()
        var temporaryProjectLocation by settings.temporaryProjectLocation.mutable()
        var snapshotStrategy by settings.snapshotStrategy.mutable()
        var stages by settings.stages.mutable()
        var minimizationTransformations by settings.minimizationTransformations.mutable()


        compilationStrategy = CompilationStrategy.DUMB
        temporaryProjectLocation = "new-project-location"
        snapshotStrategy = SnapshotStrategy.PROJECT_CLONING
        stages = mutableListOf(
            FileLevelStage(HierarchyCollectionStrategy.FILE_TREE, DDStrategy.DD_MIN)
        )
        minimizationTransformations = mutableListOf(TransformationDescriptors.PATH_RELATIVIZATION)

        val updatedSettings = project.service<MinimizationPluginSettings>().state
        assertEquals(CompilationStrategy.DUMB, updatedSettings.compilationStrategy)
        assertEquals("new-project-location", updatedSettings.temporaryProjectLocation)
        assertEquals(SnapshotStrategy.PROJECT_CLONING, updatedSettings.snapshotStrategy)
        assertEquals(
            listOf(FileLevelStage(HierarchyCollectionStrategy.FILE_TREE, DDStrategy.DD_MIN)),
            updatedSettings.stages
        )
        assertEquals(listOf(TransformationDescriptors.PATH_RELATIVIZATION), updatedSettings.minimizationTransformations)
    }

    fun testSetAndGetCompilationStrategy() {
        component.compilationStrategy = CompilationStrategy.GRADLE_IDEA
        assertEquals(CompilationStrategy.GRADLE_IDEA, component.compilationStrategy)

        component.compilationStrategy = CompilationStrategy.DUMB
        assertEquals(CompilationStrategy.DUMB, component.compilationStrategy)
    }

    fun testAddAndRemoveStages() {
        val stage = FileLevelStage(HierarchyCollectionStrategy.FILE_TREE, DDStrategy.DD_MIN)
        component.stages = listOf(stage)
        assertEquals(1, component.stages.size)
        assertEquals(stage, component.stages[0])

        // Test remove
        component.stages = listOf()
        assertTrue(component.stages.isEmpty())
    }

    fun testAddAndRemoveTransformations() {
        component.transformations = listOf(TransformationDescriptors.PATH_RELATIVIZATION)
        assertEquals(1, component.transformations.size)
        assertEquals(TransformationDescriptors.PATH_RELATIVIZATION, component.transformations[0])

        // Test remove
        component.transformations = listOf()
        assertTrue(component.transformations.isEmpty())
    }

    fun testUpdateTemporaryProjectLocation() {
        component.temporaryProjectLocation = "new-location"
        assertEquals("new-location", component.temporaryProjectLocation)
    }

    fun testIgnoreList() {
        component.ignorePaths = listOf(project.guessProjectDir()!!.toString())
        assertEquals(1, component.ignorePaths.size)
        assertEquals(project.guessProjectDir()!!.toString(), component.ignorePaths[0])
    }
}
