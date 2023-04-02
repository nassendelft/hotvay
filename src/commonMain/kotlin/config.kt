import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

private val FILE_CONFIG = userHomeDir?.let { "$it/.hotvay" }
    ?: error("could not determine home dir")

internal expect fun createModule(): SerializersModule

private val json = Json { serializersModule = createModule() }

fun readConfiguration() = watchFile(FILE_CONFIG)
    .onEach { println("[INFO] config file change detected") }
    .onStart { emit(Unit) } // forces first read
    .mapNotNull { readFileToString(FILE_CONFIG) }
    .distinctUntilChanged()
    .mapNotNull {
        try {
            json.decodeFromString<ConfigFile>(it)
        } catch (e: Exception) {
            println("[ERROR] Failed parsing config file: ${e.message}")
            null
        }
    }
    .map { Config(configToRegistrations(it)) }

data class Config(
    val registrations: List<Registration>
)

@Serializable
data class ConfigFile(
    val registrations: List<ConfigRegistration>
)

@Serializable
abstract class ConfigRegistration {
    abstract val key: KeyType
    abstract val description: String
}

@Serializable
@SerialName("command")
class CommandConfigRegistration(
    override val key: KeyType,
    override val description: String,
    val command: Command
) : ConfigRegistration()

@Serializable
data class Command(
    val executable: String,
    val arguments: List<String> = emptyList(),
    val workingDir: String? = null
)

expect fun configToRegistrations(config: ConfigFile): List<Registration>
expect fun watchFile(vararg filePath: String): Flow<Unit>
