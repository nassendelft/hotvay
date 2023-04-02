import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import platform.AppKit.NSRunningApplication
import platform.AppKit.NSWorkspace
import platform.AppKit.NSWorkspaceApplicationKey
import platform.AppKit.NSWorkspaceDidActivateApplicationNotification
import platform.Foundation.NSRunLoop
import platform.Foundation.run

var focusedApplication: NSRunningApplication? = NSWorkspace.sharedWorkspace.frontmostApplication
    private set

var moveFocusToNextWindowShortcut: KeyboardShortcut = readKeyboardShortcut(kSHKMoveFocusToNextWindow)
    private set

@OptIn(DelicateCoroutinesApi::class)
fun main(): Unit = runBlocking {
    val context = Dispatchers.Main + CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }

    GlobalScope.launch(context) { runApplication() }

    GlobalScope.launch(context) {
        readKeyboardShortcutChanges(kSHKMoveFocusToNextWindow)
            .onEach { println("[INFO] shortcut config change detected") }
            .onEach { moveFocusToNextWindowShortcut = it }
            .collect()
    }

    NSWorkspace.sharedWorkspace
        .notificationCenter
        .addObserverForName(NSWorkspaceDidActivateApplicationNotification, null, null) {
            println("[DEBUG] Front most application changed")
            focusedApplication = it?.userInfo?.get(NSWorkspaceApplicationKey) as NSRunningApplication?
        }

    NSRunLoop.currentRunLoop.run()
}
