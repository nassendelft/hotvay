import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

interface Action {
    suspend fun execute(): String?
}

private val registrationMap = mutableMapOf<KeyEvent, Registration>()
val registrations: Collection<Registration> get() = registrationMap.values

/*
Vaydeer - 9-key Smart Keypad
  vendorId:      0x0483
  productId:     0x5752
*/
fun getDeviceActions() = connectDevice(0x0483, 0x5752)
    .flowOn(Dispatchers.Default)
    .onEach { println(it) }
    .filterIsInstance<DeviceEvent.Key>()
    .mapNotNull(::getAction)

fun registerAction(registration: Registration) {
    if (registrationMap.containsKey(registration.keyEvent)) {
        println("Warning: overwriting key: $registration.keyAction")
    }

    registrationMap[registration.keyEvent] = registration
}

fun registerActions(registrations: List<Registration>) {
    if (registrations.isNotEmpty()) println("Warning: replacing registrations")
    registrationMap.clear()
    registrationMap.putAll(registrations.associateBy { it.keyEvent })
}

private fun getAction(keyEvent: DeviceEvent.Key) = registrationMap[KeyEvent(keyEvent.key, keyEvent.state)]
    ?.takeIf { it.keyEvent.state == keyEvent.state }
    ?.action

data class Registration(
    val keyEvent: KeyEvent,
    val description: String,
    val action: Action
)

data class KeyEvent(val key: KeyType, val state: KeyState = KeyState.UP)