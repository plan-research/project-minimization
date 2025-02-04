package preprocessing

import AbstractAnalysisKotlinTest
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.FileBasedIndex
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.idea.KotlinFileType
import org.plan.research.minimization.plugin.context.impl.DefaultProjectContext
import org.plan.research.minimization.plugin.modification.psi.KDocRemover
import kotlin.io.path.Path
import kotlin.io.path.relativeTo

class KDocRemoverTest : AbstractAnalysisKotlinTest() {
    override fun getTestDataPath() = "src/test/resources/testData/kdoc-remover"
    fun testSample() {
        myFixture.copyDirectoryToProject("sample", ".")
        doTest("result")
    }

    @Suppress("SameParameterValue")
    private fun doTest(finalPath: String) = runBlocking {
        val context = DefaultProjectContext(myFixture.project)
        val kDocRemover = KDocRemover()
        kDocRemover.removeKDocs(context)
        val projectFiles = buildList {
            FileBasedIndex.getInstance().iterateIndexableFiles(object : ContentIterator {
                private val root = project.guessProjectDir()!!
                override fun processFile(fileOrDir: VirtualFile): Boolean {
                    if (VfsUtilCore.isAncestor(
                            root,
                            fileOrDir,
                            true
                        ) && fileOrDir.extension == KotlinFileType.EXTENSION
                    ) add(fileOrDir)
                    return true
                }
            }, project, null)
        }
        val projectRoot = project.guessProjectDir()!!.toNioPath()
        projectFiles.forEach {
            val path = it.toNioPath().relativeTo(projectRoot)
            myFixture.checkResultByFile(path.toString(), Path(finalPath).resolve(path).toString(), true)
        }
    }
}