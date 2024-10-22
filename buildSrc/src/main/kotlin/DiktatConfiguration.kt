/**
 * Configuration for diktat static analysis
 */

package org.jetbrains.research.code.submissions.clustering.buildutils

import com.saveourtool.diktat.plugin.gradle.DiktatExtension
import com.saveourtool.diktat.plugin.gradle.DiktatGradlePlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

/**
 * Applies diktat gradle plugin and configures diktat for [this] project
 */
fun Project.configureDiktat() {
    apply<DiktatGradlePlugin>()
    configure<DiktatExtension> {
        githubActions = true
        reporters {
            plain()
            sarif()
        }
        inputs {
            include("src/main/**/*.kt")
        }
    }
}

/**
 * Creates unified tasks to run diktat on all projects
 */
fun Project.createDiktatTask() {
    if (this == rootProject) {
        apply<DiktatGradlePlugin>()
        configure<DiktatExtension> {
            diktatConfigFile = rootProject.file("diktat-analysis.yml")
            githubActions = true
            reporters {
                plain()
                sarif()
            }
            inputs {
                include("./*.kts")
                include("./buildSrc/**/*.kt")
            }
        }
    }
}
