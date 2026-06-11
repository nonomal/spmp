package plugin.shared

import org.gradle.api.Project
import org.gradle.kotlin.dsl.newInstance
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

val Project.Command: CommandClass get() = CommandClass(this)

val Project.execOperations: ExecOperations
    get() = objects.newInstance<InjectedExecOps>().getExecOps()

private interface InjectedExecOps {
    @Inject
    fun getExecOps(): ExecOperations
}

class CommandClass(project: Project): Project by project {
    fun cmd(vararg args: String): String {
        val out = ByteArrayOutputStream()
        execOperations.exec {
            commandLine(args.toList())
            standardOutput = out
        }
        return out.toString().trim()
    }

    fun getCurrentGitTag(): String? {
        try {
            val tags: List<String> = cmd("git", "tag", "--points-at", "HEAD").split('\n')
            return tags.lastOrNull()
        }
        catch (e: Throwable) {
            return null
        }
    }

    fun getCurrentGitCommitHash(): String? {
        try {
            return cmd("git", "rev-parse", "HEAD").ifBlank { null }
        }
        catch (e: Throwable) {
            return null
        }
    }
}
