import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    val config = readConfiguration()
    registerActions(config.registrations)

    getDeviceActions().collect {
        launch(Dispatchers.Default) {
            println(it.execute())
        }
    }
}

expect fun connectDevice(vendorId: Int, productId: Int): Flow<DeviceEvent>
