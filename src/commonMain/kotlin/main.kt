import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSRunLoop
import platform.Foundation.run


fun main(): Unit = runBlocking {
    launch(Dispatchers.Default) {
        readConfiguration().collect { registerActions(it.registrations) }
    }

    launch(Dispatchers.Default) {
        getDeviceActions().collect {
            launch(Dispatchers.Default) {
                println("Result: ${it.execute()}")
            }
        }
    }

    // TODO move this to macosX64Main
    NSRunLoop.currentRunLoop.run()
}

expect fun connectDevice(vendorId: Int, productId: Int): Flow<DeviceEvent>
