import com.intellij.openapi.application.writeAction
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.progress.runWithModalProgressBlocking
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import com.intellij.testFramework.runInEdtAndWait
import org.jetbrains.plugins.gradle.util.GradleConstants


open class GradleProjectBaseTest : JavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/gradle"
    }
    protected lateinit var sdk: Sdk

    override fun setUp() {
        super.setUp()
        sdk = ExternalSystemJdkUtil.getAvailableJdk(project).second
        SdkConfigurationUtil.addSdk(sdk)
    }

    override fun tearDown() {
        val jdkTable = ProjectJdkTable.getInstance()
        runWithModalProgressBlocking(project, "") {
            writeAction {
                jdkTable.removeJdk(sdk)
            }
        }
        super.tearDown()
    }

    protected fun importGradleProject(root: VirtualFile) {
        val projectPath = root.path

        runInEdtAndWait {
            ExternalSystemUtil.refreshProject(
                projectPath,
                ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
                    .use(ProgressExecutionMode.MODAL_SYNC)
                    .build()
            )
        }
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }

    protected fun assertGradleLoaded() {
        assert(
            ProjectDataManager.getInstance()
                .getExternalProjectsData(project, GradleConstants.SYSTEM_ID).isNotEmpty()
        ) { "Gradle project hasn't been loaded correctly"}
    }
}