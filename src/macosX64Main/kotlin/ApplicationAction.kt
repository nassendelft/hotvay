import platform.AppKit.*
import platform.ApplicationServices.AXIsProcessTrusted
import platform.CoreFoundation.CFRelease
import platform.CoreGraphics.*
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ApplicationAction(private val bundleId: String) : Action {

    override suspend fun execute(): Boolean {
        println("[INFO] executing 'ApplicationAction' with bundleId '$bundleId'")
        val focusedAppBundleId = focusedApplication?.bundleIdentifier
        println("[DEBUG] Current focused app: $focusedAppBundleId")

        if (focusedAppBundleId == bundleId) {
            if (!AXIsProcessTrusted()) {
                println("[WARN] Accessibility permission is not granted")
                return true
            }
            println("[DEBUG] Toggling window for app: $bundleId")
            toggleWindow()
            return true
        }

        val runningApp = findRunningApp(bundleId)
        if (runningApp != null) {
            println("[DEBUG] Focusing app: $bundleId")
            runningApp.activateWithOptions(NSApplicationActivateIgnoringOtherApps)
        } else {
            println("[DEBUG] Opening app: $bundleId")
            openApp(bundleId)
        }
        return true
    }

    private suspend fun openApp(bundleId: String) {
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
        val (enabled, modifiers, virtualKeyCode) = toggleKeyShortcut
        println("[DEBUG] Toggle keyboard shortcut: $toggleKeyShortcut]")
        if (!enabled) return

        val source = CGEventSourceCreate(kCGEventSourceStateHIDSystemState)

        val modifiersUp = modifiers.map { CGEventCreateKeyboardEvent(source, it.virtualKeyCode, false) }
        val modifiersDown = modifiers.map { CGEventCreateKeyboardEvent(source, it.virtualKeyCode, true) }
        val virtualKeyUp = CGEventCreateKeyboardEvent(source, virtualKeyCode.toUShort(), false)
        val virtualKeyDown = CGEventCreateKeyboardEvent(source, virtualKeyCode.toUShort(), true)

        CGEventSetFlags(virtualKeyDown, kCGEventFlagMaskCommand)
        CGEventSetFlags(virtualKeyUp, kCGEventFlagMaskCommand)
        modifiersDown.forEach { CGEventPost(kCGHIDEventTap, it) }
        CGEventPost(kCGHIDEventTap, virtualKeyDown)
        CGEventPost(kCGHIDEventTap, virtualKeyUp)
        modifiersUp.forEach { CGEventPost(kCGHIDEventTap, it) }

        CFRelease(virtualKeyDown)
        CFRelease(virtualKeyUp)
        modifiersDown.forEach { CFRelease(it) }
        modifiersUp.forEach { CFRelease(it) }
        CFRelease(source)
    }

    companion object {
        // TODO re-read when shortcut changes
        private val toggleKeyShortcut by lazy { getKeyboardShortcut(kSHKMoveFocusToNextWindow) }
    }
}
