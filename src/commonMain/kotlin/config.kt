import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val FILE_CONFIG = ".hotvay"

fun readConfiguration(): Config {
    val configFile = userHomeDir?.let { "$it/$FILE_CONFIG" }
        ?: error("could not determine home dir")
    val bytes = readFileToString(configFile)
    val config: ConfigFile = Json.decodeFromString(bytes)
    return Config(configToRegistrations(config))
}

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
    val command: Command,
)

@Serializable
data class Command(
    val executable: String,
    val arguments: List<String> = emptyList(),
    val workingDir: String? = null
)

expect fun configToRegistrations(config: ConfigFile): List<Registration>