import kotlinx.cinterop.*
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import platform.posix.*

fun main(): Unit = runBlocking {
    val cancelFunc = staticCFunction<Int, Unit> {
        println("stopping...")
    }
    signal(SIGINT, cancelFunc)
    signal(SIGTERM, cancelFunc)

    val config = readConfiguration()
    val registrations = configToRegistrations(config)
    registerActions(registrations)

    launch(SupervisorJob()) {
        registrations.first().action.execute()?.let { println(it) }
//        getDeviceActions()
//            .onEach { it.execute() }
//            .collect()
    }
}

private fun configToRegistrations(config: Config) = config.registrations
    .map { Registration(KeyAction(it.key), it.description, ExecuteAction(it.command)) }

private const val FILE_CONFIG = ".hotvay"

private fun readConfiguration(): Config {
    

    val configFile = userHomeDir?.let { "$it/$FILE_CONFIG" }
        ?: error("could not determine home dir")
    val bytes = readFileToString(configFile)
    return Json.decodeFromString(bytes)
}


@Serializable
private data class Config(
    val registrations: List<ConfigRegistration>
)

@Serializable
private data class ConfigRegistration(
    val key: Key,
    val description: String,
    val command: String,
)

private class ExecuteAction(private val command: String) : Action {
    override fun execute(): String {
        val output = StringBuilder()
        memScoped {
            println("Executing '$command'")

            val filePointer = popen("/usr/bin/env $command 2>&1", "r")
                ?: error("Could not execute command")
            try {
                val path = allocArray<ByteVar>(4096)
                val bufSize = sizeOf<CPointerVarOf<CArrayPointer<ByteVar>>>() * 4096
                while (fgets(path, bufSize.toInt(), filePointer) != null) {
                    output.append(path.toKStringFromUtf8())
                }
            } finally {
                val exitCode = pclose(filePointer)
                if (exitCode != 0) println("'$command' execution failed (exit code $exitCode)")
            }
        }
        return output.toString()
    }
}