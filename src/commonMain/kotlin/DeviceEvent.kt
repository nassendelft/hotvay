typealias KeyType = UByte

enum class KeyState { UP, DOWN }

abstract class DeviceEvent(open val serial: String?) {
    data class Connected(override val serial: String?) : DeviceEvent(serial)
    data class Disconnected(override val serial: String?) : DeviceEvent(serial)
    data class Key(override val serial: String?, val key: KeyType, val state: KeyState) : DeviceEvent(serial)
}