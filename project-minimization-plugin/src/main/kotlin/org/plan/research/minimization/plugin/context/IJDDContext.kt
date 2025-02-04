package org.plan.research.minimization.plugin.context

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile

@Suppress("KDOC_EXTRA_PROPERTY", "KDOC_NO_CLASS_BODY_PROPERTIES_IN_HEADER")
/**
 * Represents a context for the minimization process containing the current project and derived properties.
 *
 * @property originalProject The original project before any minimization stages.
 * @property indexProject The project that can be used for indexes or for progress reporting purposes
 * @constructor projectDir The directory of the current project to be minimized.
 */
interface IJDDContext {
    val originalProject: Project
    val projectDir: VirtualFile
    val indexProject: Project
    val indexProjectDir: VirtualFile
}

sealed class IJDDContextBase<C : IJDDContextBase<C>>(
    override val originalProject: Project,
) : IJDDContext {
    override val indexProjectDir by lazy { indexProject.guessProjectDir()!! }

    abstract suspend fun <T> transform(transformer: IJDDContextTransformer<T>): T

    abstract suspend fun clone(cloner: IJDDContextCloner): C?
}

/**
 * This context represents a project as an opened IntelliJ IDEA project.
 */
abstract class HeavyIJDDContext<C : HeavyIJDDContext<C>>(
    val project: Project,
    originalProject: Project,
) : IJDDContextBase<C>(originalProject) {
    override val projectDir: VirtualFile by lazy { project.guessProjectDir()!! }
    override val indexProject: Project = project

    abstract fun copy(project: Project): C

    override suspend fun <T> transform(transformer: IJDDContextTransformer<T>): T =
        transformer.transformHeavy(this)

    override fun toString(): String = "HeavyIJDDContext(project=$projectDir)"
}

/**
 * This context represents a project as a usual project in the file system.
 */
abstract class LightIJDDContext<C : LightIJDDContext<C>>(
    override val projectDir: VirtualFile,
    override val indexProject: Project,
    originalProject: Project,
) : IJDDContextBase<C>(originalProject) {
    abstract fun copy(projectDir: VirtualFile): C

    override suspend fun <T> transform(transformer: IJDDContextTransformer<T>): T =
        transformer.transformLight(this)

    override fun toString(): String = "LightIJDDContext(project=$projectDir, indexProject=$indexProjectDir)"
}

interface IJDDContextTransformer<T> {
    suspend fun transformLight(context: LightIJDDContext<*>): T
    suspend fun transformHeavy(context: HeavyIJDDContext<*>): T
}

interface IJDDContextCloner {
    suspend fun <C : LightIJDDContext<C>> cloneLight(context: C): C?
    suspend fun <C : HeavyIJDDContext<C>> cloneHeavy(context: C): C?
}
