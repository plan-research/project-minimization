package org.plan.research.minimization.plugin.services

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import org.jetbrains.kotlin.idea.configuration.GRADLE_SYSTEM_ID
import java.nio.file.Path

@Service(Service.Level.APP)
class ProjectOpeningService {
    // TODO: JBRes-1977
    var isTest: Boolean = false

    suspend fun openProject(projectPath: Path): Project? {
        val project = ProjectUtil.openOrImportAsync(projectPath, OpenProjectTask {
            forceOpenInNewFrame = true
            runConversionBeforeOpen = false
            isRefreshVfsNeeded = !isTest
        }) ?: return null

        LocalFileSystem.getInstance().refreshAndFindFileByNioFile(projectPath)?.refresh(false, true)

        forceImportGradleProject(project)

        return project
    }

    private fun forceImportGradleProject(project: Project) {
        if (!isTest) {
            ExternalSystemUtil.refreshProject(
                project.guessProjectDir()!!.path,
                ImportSpecBuilder(project, GRADLE_SYSTEM_ID)
                    .use(ProgressExecutionMode.MODAL_SYNC)
                    .build(),
            )
        }
    }
}
