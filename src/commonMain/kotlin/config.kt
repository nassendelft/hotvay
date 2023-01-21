import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val FILE_CONFIG = userHomeDir?.let { "$it/.hotvay" }
    ?: error("could not determine home dir")

fun readConfiguration() = watchFile(FILE_CONFIG)
    .onEach { println("[INFO] config file change detected") }
    .onStart { emit(Unit) } // forces first read
    .mapNotNull { readFileToString(FILE_CONFIG) }
    .distinctUntilChanged()
    .mapNotNull {
        try {
            Json.decodeFromString<ConfigFile>(it)
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
data class ConfigRegistration(
    val key: KeyType,
    val description: String,
    val appBundleId: String? = null,
    val command: Command? = null
)

@Serializable
data class Command(
    val executable: String,
    val arguments: List<String> = emptyList(),
    val workingDir: String? = null
)

expect fun configToRegistrations(config: ConfigFile): List<Registration>
expect fun watchFile(vararg filePath: String): Flow<Unit>
