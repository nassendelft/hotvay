import kotlinx.cinterop.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import platform.CoreFoundation.*
import platform.Foundation.NSLibraryDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

const val kSHKMoveFocusToNextWindow = "27"

private const val symbolicHotKeys = "com.apple.symbolichotkeys"

fun readKeyboardShortcutChanges(shortcutId: String): Flow<KeyboardShortcut> {
    val libraryDir = NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, true).first()
    return watchFile("$libraryDir/Preferences/$symbolicHotKeys.plist")
        .onStart { emit(Unit) } // forces first read
        .map { readKeyboardShortcut(shortcutId) }
}

fun readKeyboardShortcut(shortcutId: String): KeyboardShortcut = cfMemScoped {
    val shortcutDict = readAppPreference("AppleSymbolicHotKeys", symbolicHotKeys)
        .asCFDictionary()
        .getCFDictionary(shortcutId)

    val enabled = shortcutDict.getBoolean("enabled")

    val parametersDict = shortcutDict.getCFDictionary("value").getCFArray("parameters")

    val virtualKeyCode = parametersDict.getLong(1)

    val mask = parametersDict.getLong(2)

    val modifiers = getModifierVirtualKeyCodes(mask)

    return KeyboardShortcut(enabled, modifiers, virtualKeyCode)
}

private fun getModifierVirtualKeyCodes(mask: Long): List<Modifiers> = memScoped {
    println("[DEBUG] shortcut modifier mask: $mask")
    val modifiers = mutableListOf<Modifiers>()
    if (mask and 0x100000L == 0x100000L) modifiers.add(Modifiers.COMMAND)
    if (mask and 0x80000L == 0x80000L) modifiers.add(Modifiers.OPTION)
    if (mask and 0x40000L == 0x40000L) modifiers.add(Modifiers.CONTROL)
    if (mask and 0x20000L == 0x20000L) modifiers.add(Modifiers.SHIFT)
    return modifiers
}

enum class Modifiers(val virtualKeyCode: UShort) {
    SHIFT(0x38u),
    COMMAND(0x37u),
    OPTION(0x4Au),
    CONTROL(0x4Bu),
}

data class KeyboardShortcut(
    val enabled: Boolean,
    val modifiers: List<Modifiers>,
    val virtualKeyCode: Long
)

private fun CFMemScope.readAppPreference(key: String, appId: String) =
    CFPreferencesCopyAppValue(cfString(key), cfString(appId))
        ?.let(::scoped)
        ?: error("Could not read '$appId.plist'")