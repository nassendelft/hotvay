import kotlinx.coroutines.*
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import platform.AppKit.NSRunningApplication
import platform.AppKit.NSWorkspace
import platform.AppKit.NSWorkspaceApplicationKey
import platform.AppKit.NSWorkspaceDidActivateApplicationNotification
import platform.CoreFoundation.CFRunLoopRun
import platform.Foundation.NSRunLoop
import platform.Foundation.run

var focusedApplication: NSRunningApplication? = NSWorkspace.sharedWorkspace.frontmostApplication
    private set

@OptIn(DelicateCoroutinesApi::class)
fun main(): Unit = runBlocking {
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }
    GlobalScope.launch(Dispatchers.Main + handler) { runApplication() }

    NSWorkspace.sharedWorkspace
        .notificationCenter
        .addObserverForName(
            NSWorkspaceDidActivateApplicationNotification,
            null,
            null
        ) {
            println("[DEBUG] Front most application changed")
            focusedApplication = it?.userInfo?.get(NSWorkspaceApplicationKey) as NSRunningApplication?
        }

    NSRunLoop.currentRunLoop.run()
}
