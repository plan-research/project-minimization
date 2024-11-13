import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.XmlSerializer
import junit.framework.TestCase
import mu.KotlinLogging
import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.state.CompilationStrategy
import org.plan.research.minimization.plugin.model.state.DDStrategy
import org.plan.research.minimization.plugin.settings.MinimizationPluginState

class MinimizationPluginStateTest : TestCase() {

    fun testDefault() {
        val state = MinimizationPluginState()
        doTest(state)
    }
    
    fun testModified() {
        val state = MinimizationPluginState()
        state.compilationStrategy = CompilationStrategy.DUMB
        state.gradleTask = "compileKotlin"
        state.temporaryProjectLocation = "custom-location"
        state.gradleOptions = listOf("--option1", "--option2")
        state.stages = listOf(FileLevelStage())
        state.minimizationTransformations = listOf()
        doTest(state)
    }

    fun testStages() {
        val state = MinimizationPluginState()
        state.stages = listOf(
            FileLevelStage(
                ddAlgorithm = DDStrategy.DD_MIN
            )
        )
        doTest(state)
    }

    private fun doTest(state: MinimizationPluginState) {
        val serialized = XmlSerializer.serialize(state)
        logger.info { JDOMUtil.write(serialized) }
        val deserialized = XmlSerializer.deserialize(serialized, MinimizationPluginState::class.java)
        assertEquals(state.minimizationTransformations, deserialized.minimizationTransformations)
        assertEquals(state.stages, deserialized.stages)
        assertEquals(state.gradleTask, deserialized.gradleTask)
        assertEquals(state.compilationStrategy, deserialized.compilationStrategy)
        assertEquals(state.temporaryProjectLocation, deserialized.temporaryProjectLocation)
        assertEquals(state.snapshotStrategy, deserialized.snapshotStrategy)
        assertEquals(state.exceptionComparingStrategy, deserialized.exceptionComparingStrategy)
        assertEquals(state.gradleOptions, deserialized.gradleOptions)
    }

    private val logger = KotlinLogging.logger("org.plan.research.minimization.plugin.MinimizationPluginStateTest")
}