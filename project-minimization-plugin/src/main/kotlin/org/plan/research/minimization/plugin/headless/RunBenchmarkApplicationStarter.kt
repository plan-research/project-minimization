package org.plan.research.minimization.plugin.headless

import com.github.ajalt.clikt.command.SuspendingCliktCommand

class RunBenchmarkApplicationStarter : AbstractApplicationStarterCliktAdapter() {
    override val cliktRunner: SuspendingCliktCommand = RunBenchmarkCliCommand()
}
