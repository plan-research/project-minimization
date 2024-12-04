package org.plan.research.minimization.plugin.services

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.platform.backend.observation.Observation
import mu.KotlinLogging
import java.nio.file.Path

@Service(Service.Level.APP)
class ProjectOpeningService {
    // TODO: JBRes-1977
    var isTest: Boolean = false
    private val logger = KotlinLogging.logger {}

    suspend fun openProject(projectPath: Path): Project? {
        val project = ProjectUtil.openOrImportAsync(projectPath, OpenProjectTask {
            forceOpenInNewFrame = true
            runConversionBeforeOpen = false
            isRefreshVfsNeeded = !isTest
        }) ?: return null

        LocalFileSystem.getInstance().refreshAndFindFileByNioFile(projectPath)?.refresh(false, true)

        Observation.awaitConfiguration(project) {
            logger.info { "Awaiting ${project.name} configuration: $it" }
        }
        return project
    }
}
