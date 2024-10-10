import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import kotlin.test.assertNotEquals


abstract class GradleProjectBaseTest : JavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/gradle"
    }

    override fun runInDispatchThread(): Boolean = false

    protected lateinit var sdk: Sdk

    override fun setUp() {
        super.setUp()
        sdk = ExternalSystemJdkUtil.getAvailableJdk(project).second
        ApplicationManager.getApplication().runWriteAction {
            val jdkTable = ProjectJdkTable.getInstance()
            jdkTable.addJdk(sdk, testRootDisposable)
        }
    }

    protected suspend fun importGradleProject(root: VirtualFile) {
        val projectPath = root.path
        val gradleSettings = GradleSettings.getInstance(project)
        val projectSettings = GradleProjectSettings().apply {
            externalProjectPath = projectPath
            gradleJvm = sdk.name
        }
        gradleSettings.linkProject(projectSettings)

        withContext(Dispatchers.EDT) {
            ExternalSystemUtil.refreshProject(
                projectPath,
                ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
                    .use(ProgressExecutionMode.MODAL_SYNC)
                    .build()
            )
        }
    }

    protected fun assertGradleLoaded() {
        val data = ProjectDataManager
            .getInstance()
            .getExternalProjectsData(project, GradleConstants.SYSTEM_ID)
            .firstOrNull()
            ?.externalProjectStructure
            ?.data
        kotlin.test.assertNotNull(data, message = "Gradle project is not loaded")
        assertNotEquals("unspecified", data.version, message = "Gradle project is not loaded")
    }
}