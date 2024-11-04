import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.plan.research.minimization.plugin.settings.AppSettingsComponent
import org.plan.research.minimization.plugin.model.state.*
import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.settings.AppSettings

class SettingsUITest : BasePlatformTestCase() {

    private lateinit var component: AppSettingsComponent

    override fun setUp() {
        super.setUp()
        component = AppSettingsComponent()
    }

    fun testDefaultSettings() {
        val settings = AppSettings.getInstance().state
        assertEquals(CompilationStrategy.GRADLE_IDEA, settings.compilationStrategy)
        assertEquals("minimization-project-snapshots", settings.temporaryProjectLocation)
        assertEquals(SnapshotStrategy.PROJECT_CLONING, settings.snapshotStrategy)
        assertEquals(ExceptionComparingStrategy.SIMPLE, settings.exceptionComparingStrategy)
        assertEquals(listOf(FileLevelStage(HierarchyCollectionStrategy.FILE_TREE, DDStrategy.PROBABILISTIC_DD)), settings.stages)
        assertEquals(listOf(TransformationDescriptors.PATH_RELATIVIZATION), settings.minimizationTransformations)
    }

    fun testUpdateSettings() {
        val settings = AppSettings.getInstance().state

        settings.compilationStrategy = CompilationStrategy.DUMB
        settings.temporaryProjectLocation = "new-project-location"
        settings.snapshotStrategy = SnapshotStrategy.PROJECT_CLONING
        settings.stages = mutableListOf(
            FileLevelStage(HierarchyCollectionStrategy.FILE_TREE, DDStrategy.DD_MIN)
        )
        settings.minimizationTransformations = mutableListOf(TransformationDescriptors.PATH_RELATIVIZATION)

        AppSettings.getInstance().loadState(settings)

        val updatedSettings = AppSettings.getInstance().state
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
}
