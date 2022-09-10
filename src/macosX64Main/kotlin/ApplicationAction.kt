import platform.AppKit.*
import platform.CoreFoundation.CFRelease
import platform.CoreGraphics.*
import platform.GameController.GCKeyCode
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ApplicationAction(private val bundleId: String) : Action {

    private var runningApp = NSWorkspace.sharedWorkspace.frontmostApplication?.bundleIdentifier

    init {
        NSWorkspace.sharedWorkspace
            .notificationCenter
            .addObserverForName(
                NSWorkspaceDidActivateApplicationNotification,
                null,
                null
            ) {
                runningApp = (it?.userInfo?.get(NSWorkspaceApplicationKey) as NSRunningApplication?)?.bundleIdentifier
            }
    }

    override suspend fun execute(): String? {
        println("Active app: $runningApp")
        if (runningApp == bundleId) {
            toggleWindow()
        } else {
            val runningApp = findRunningApp(bundleId)
            if (runningApp != null) {
                println("Activating app")
                runningApp.activateWithOptions(NSApplicationActivateIgnoringOtherApps)
            } else {
                openApp(bundleId)
            }
        }
        return null
    }

    private suspend fun openApp(bundleId: String) {
        println("opening app")
        suspendCoroutine { continuation ->
            NSWorkspace.sharedWorkspace.openApplicationAtURL(
                NSWorkspace.sharedWorkspace.URLForApplicationWithBundleIdentifier(bundleId)!!,
                NSWorkspaceOpenConfiguration.configuration()
            ) { _, nsError ->
                if (nsError == null) {
                    continuation.resumeWith(Result.success(Unit))
                } else {
                    continuation.resumeWithException(Exception(nsError.localizedDescription))
                }
            }
        }
    }

    private fun findRunningApp(bundleId: String) = NSWorkspace.sharedWorkspace
        .runningApplications
        .filterIsInstance<NSRunningApplication>()
        .find { it.bundleIdentifier == bundleId }

    private fun toggleWindow() {
        println("toggling window")
        // TODO read shortcut keys from system: https://stackoverflow.com/a/879437/571088
        val source = CGEventSourceCreate(kCGEventSourceStateHIDSystemState)
        val commandUp = CGEventCreateKeyboardEvent(source, commandKey, false)
        val commandDown = CGEventCreateKeyboardEvent(source, commandKey, true)
        val backTickUp = CGEventCreateKeyboardEvent(source, backTickKey, false)
        val backTickDown = CGEventCreateKeyboardEvent(source, backTickKey, true)
        CGEventSetFlags(backTickDown, kCGEventFlagMaskCommand)
        CGEventSetFlags(backTickUp, kCGEventFlagMaskCommand)
        CGEventPost(kCGHIDEventTap, commandDown)
        CGEventPost(kCGHIDEventTap, backTickDown)
        CGEventPost(kCGHIDEventTap, backTickUp)
        CGEventPost(kCGHIDEventTap, commandUp)
        CFRelease(backTickDown)
        CFRelease(backTickUp)
        CFRelease(commandDown)
        CFRelease(commandUp)
        CFRelease(source)
    }

    companion object {
        private const val commandKey: UShort = 0x37u
        private const val backTickKey: UShort = 0x32u
    }
}
