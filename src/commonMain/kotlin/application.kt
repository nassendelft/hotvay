import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface Action {
    suspend fun execute(): Boolean
}

private val registrationMap = mutableMapOf<KeyEvent, Registration>()

internal suspend fun runApplication() = withContext(Dispatchers.Main) {
    launch(Dispatchers.Default) {
        readConfiguration()
            .map { it.registrations }
            .onEach { println("[INFO] Loading ${it.size} action${if (it.size == 1) "" else "s"}") }
            .collect(::registerActions)
    }

    launch(Dispatchers.Default) {
        getDeviceActions().collect(::executeAction)
    }
}

private fun CoroutineScope.executeAction(it: Action) {
    launch(Dispatchers.Default) { println("[INFO] Action result: ${it.execute()}") }
}

/*
Vaydeer - 9-key Smart Keypad
  vendorId:      0x0483
  productId:     0x5752
*/
private fun getDeviceActions() = connectDevice(0x0483, 0x5752)
    .flowOn(Dispatchers.Default)
    .onEach { println("[INFO] Device state change: $it") }
    .filterIsInstance<DeviceEvent.Key>()
    .mapNotNull(::getAction)

private fun registerActions(registrations: List<Registration>) {
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

internal expect fun connectDevice(vendorId: Int, productId: Int): Flow<DeviceEvent>
