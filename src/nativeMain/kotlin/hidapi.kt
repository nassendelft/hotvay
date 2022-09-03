import hidapi.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import platform.posix.*

/*
https://github.com/libusb/hidapi
https://usb.org/sites/default/files/hut1_3_0.pdf
http://www.softelectro.ru/usb30.pdf

Vaydeer - 9-key Smart Keypad
  vendorId:      0x0483
  productId:     0x5752
*/

suspend fun <T> hidScope(
    initializationRetryCount: Int = 0,
    initializationRetryDelay: Long = 500,
    block: suspend () -> T
): T {
    initialize(initializationRetryCount, initializationRetryDelay)
    println("hid initialized")

    val value = block()

    println("hid exit")
    hid_exit()

    return value
}

private suspend fun initialize(maxRetry: Int, retryDelay: Long) {
    var retried = 0
    while (hid_init() != 0) {
        if (maxRetry == 0 || ++retried == maxRetry) error("Could not init")
        println("Could not init")
        delay(retryDelay)
    }
}

fun readDeviceInput(vendorId: UShort, productId: UShort): Flow<KeyAction>? {
    val device = hid_open(vendorId, productId, null) ?: return null
    hid_set_nonblocking(device, 1)
    return readInput(device).onCompletion { hid_close(device) }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun readInput(
    device: CPointer<hid_device>,
    delayMillis: Long = 200
): Flow<KeyAction> = channelFlow {
    memScoped {
        val data = allocArray<UByteVar>(16)
        while (!isClosedForSend) {
            val bytesRead = hid_read(device, data, 16)
            if (bytesRead == -1) {
                println("could not read")
                break
            }
            if (bytesRead > 0) {
                send(keyData2Key(data))
            }
            delay(delayMillis)
        }
    }
}

private fun keyData2Key(data: CArrayPointer<UByteVar>) = KeyAction(
    data[3],
    if (data[4] == 0u.toUByte()) KeyState.DOWN else KeyState.UP
)

typealias Key = UByte

data class KeyAction(
    val key: Key,
    val state: KeyState = KeyState.UP
)

enum class KeyState { UP, DOWN }