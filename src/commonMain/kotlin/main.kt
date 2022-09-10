import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import platform.AppKit.NSRunningApplication
import platform.AppKit.NSWorkspace
import platform.AppKit.NSWorkspaceDidActivateApplicationNotification
import platform.AppKit.runningApplications
import platform.CoreFoundation.CFRunLoopRun
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSRunLoop
import platform.Foundation.run
import platform.darwin.dispatch_main


fun main(): Unit = runBlocking {
    val config = readConfiguration()
    registerActions(config.registrations)

    launch(Dispatchers.Default) {
        getDeviceActions().collect {
            launch(Dispatchers.Default) {
                println("Result: ${it.execute()}")
            }
        }
    }

    NSRunLoop.currentRunLoop.run()
}

expect fun connectDevice(vendorId: Int, productId: Int): Flow<DeviceEvent>
