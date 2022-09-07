import kotlinx.cinterop.*
import platform.Foundation.*
import platform.Foundation.NSURL.Companion.fileURLWithPath


actual fun configToRegistrations(config: ConfigFile) = config.registrations
    .map { Registration(KeyEvent(it.key), it.description, ExecuteAction(it.command)) }

private class ExecuteAction(private val command: Command) : Action {
    override fun execute() = try {
        val pipe = NSPipe()
        val process = NSTask().apply {
            standardOutput = pipe
            standardError = pipe
            standardInput = null
            executableURL = fileURLWithPath(command.executable)
            arguments = command.arguments
            command.workingDir?.let { currentDirectoryURL = fileURLWithPath(it) }
        }

        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            if (!process.launchAndReturnError(error.ptr)) {
                return "error: ${error.value?.localizedDescription}"
            }
        }

        process.waitUntilExit()

        pipe.fileHandleForReading
            .readDataToEndOfFile()
            .let { NSString.create(data = it, NSUTF8StringEncoding) }
            ?.toString()
            ?: "<< no output >>"
    } catch (e: Exception) {
        "Failed executing command: $command"
    }
}