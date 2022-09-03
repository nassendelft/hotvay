import kotlinx.coroutines.flow.mapNotNull

interface Action {
    fun execute(): String?
}

private val registrationMap = mutableMapOf<KeyAction, Registration>()
val registrations: Collection<Registration> get() = registrationMap.values

suspend fun getDeviceActions() = hidScope(5) {
    readDeviceInput(0x0483u, 0x5752u)
        ?.mapNotNull(::getAction)
        ?: error("could not connect to device")
}

fun registerAction(registration: Registration) {
    if (registrationMap.containsKey(registration.keyAction)) {
        println("Warning: overwriting key: $registration.keyAction")
    }

    registrationMap[registration.keyAction] = registration
}

fun registerActions(registrations: List<Registration>) {
    if (registrations.isNotEmpty()) println("Warning: replacing registrations")
    registrationMap.clear()
    registrationMap.putAll(registrations.associateBy { it.keyAction })
}

private fun getAction(keyAction: KeyAction) = registrationMap[keyAction]
    ?.takeIf { it.keyAction.state == keyAction.state }
    ?.action

data class Registration(
    val keyAction: KeyAction,
    val description: String,
    val action: Action
)