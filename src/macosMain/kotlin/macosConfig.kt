import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

actual fun configToRegistrations(config: ConfigFile) = config.registrations
    .map { Registration(KeyEvent(it.key), it.description, it.action) }

@Serializable
@SerialName("app")
class AppConfigRegistration(
    override val key: KeyType,
    override val description: String,
    val appBundleId: String,
): ConfigRegistration()

private val ConfigRegistration.action: Action
    get() = when(this) {
        is AppConfigRegistration -> ApplicationAction(appBundleId)
        is CommandConfigRegistration -> ExecuteAction(command)
        else -> error("Could not find action")
    }

internal actual fun createModule() = SerializersModule {
    polymorphic(ConfigRegistration::class) {
        subclass(AppConfigRegistration::class)
        subclass(CommandConfigRegistration::class)
    }
}
