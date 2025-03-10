package gradle

import AbstractAnalysisKotlinTest
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.platform.backend.observation.ActivityKey
import com.intellij.platform.backend.observation.trackActivity
import com.intellij.testFramework.TestObservation
import com.intellij.testFramework.common.runAll
import mu.KotlinLogging
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.plan.research.minimization.plugin.settings.data.TransformationDescriptor
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings
import kotlin.test.assertNotEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private object TestGradleProjectConfigurationActivityKey : ActivityKey {
    override val presentableName: String
        get() = "The test Gradle project configuration"
}

private val DEFAULT_SYNC_TIMEOUT: Duration = 10.minutes


suspend fun <R> awaitGradleProjectConfiguration(project: Project, action: suspend () -> R): R {
    return project.trackActivity(TestGradleProjectConfigurationActivityKey, action)
        .also { TestObservation.awaitConfiguration(DEFAULT_SYNC_TIMEOUT, project) }
}

abstract class GradleProjectBaseTest : AbstractAnalysisKotlinTest() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/gradle"
    }

    override fun runInDispatchThread(): Boolean = false
    private val logger = KotlinLogging.logger {}

    override fun setUp() {
        super.setUp()
//        configureModules(project)
    }

    override fun tearDown() {
        runAll({ removeJdk() }, { super.tearDown() })
    }

    private fun removeJdk() {
        val jdkTable = ProjectJdkTable.getInstance()
        val jdks = jdkTable.allJdks
        invokeAndWaitIfNeeded {
            runWriteAction {
                jdks.forEach { jdkTable.removeJdk(it) }

            }
        }
    }

    protected suspend fun importGradleProject(project: Project) {
        val projectDir = project.guessProjectDir()!!
        val importSpec = ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
            .use(ProgressExecutionMode.IN_BACKGROUND_ASYNC)
            .build()
        awaitGradleProjectConfiguration(project) {
            ExternalSystemUtil.refreshProject(
                projectDir.path,
                importSpec
            )
        }
    }

    protected suspend fun assertGradleLoaded(project: Project) {
        val data = smartReadAction(project) {
            ProjectDataManager
                .getInstance()
                .getExternalProjectsData(project, GradleConstants.SYSTEM_ID)
                .firstOrNull()
                ?.externalProjectStructure
                ?.data
        }
        kotlin.test.assertNotNull(data, message = "Gradle project is not loaded")
        assertNotEquals("unspecified", data.version, message = "Gradle project is not loaded")
    }

    protected fun copyGradle(useK2: Boolean = false, useBuildKts: Boolean = true, copyProperties: Boolean = true) {
        myFixture.copyDirectoryToProject("core/gradle", "gradle")
        if (copyProperties)
            myFixture.copyFileToProject("core/gradle.properties", "gradle.properties")
        myFixture.copyFileToProject("core/settings.gradle.kts", "settings.gradle.kts")
        if (useBuildKts) {
            if (!useK2)
                myFixture.copyFileToProject("core/build.gradle.kts", "build.gradle.kts")
            else
                myFixture.copyFileToProject("core/build.gradle.kts.2", "build.gradle.kts")
        }
        myFixture.copyFileToProject("core/gradlew", "gradlew")
        myFixture.copyFileToProject("core/gradlew.bat", "gradlew.bat")
    }

    protected fun enableDeduplication() {
        project.service<MinimizationPluginSettings>().stateObservable.minimizationTransformations.mutate {
            if (!it.contains(TransformationDescriptor.PATH_RELATIVIZATION)) {
                it + TransformationDescriptor.PATH_RELATIVIZATION
            } else {
                it
            }
        }
    }

    protected fun disableDeduplication() {
        project.service<MinimizationPluginSettings>().stateObservable.minimizationTransformations.mutate {
            it - TransformationDescriptor.PATH_RELATIVIZATION
        }
    }
}