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

fun readKeyboardShortcut(shortcutId: String): KeyboardShortcut {
    val key = "AppleSymbolicHotKeys".toCFStringRef()
    val appId = symbolicHotKeys.toCFStringRef()
    val hotkeyPrefList: CFDictionaryRef = CFPreferencesCopyAppValue(key, appId)
        ?.takeIf { CFGetTypeID(it) == CFDictionaryGetTypeID() }
        ?.reinterpret()
        ?: error("Could not read keyboard shortcuts")

    val shortcutDict: CFDictionaryRef = hotkeyPrefList.getCFDictionary(shortcutId)

    val enabled = shortcutDict.getBoolean("enabled")

    val parametersDict = shortcutDict.getCFDictionary("value").getCFArray("parameters")
    val virtualKeyCode = parametersDict.getLong(1)
    val modifiers = getModifierVirtualKeyCodes(parametersDict.getLong(2))

    CFRelease(appId)
    CFRelease(key)
    CFRelease(hotkeyPrefList)

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

private fun CFDictionaryRef.getValue(key: String): CPointer<out CPointed> = memScoped {
    val cfKey = key.toCFStringRef()
    val value: COpaquePointerVar = alloc()
    val hasValue: Boolean = CFDictionaryGetValueIfPresent(this@getValue, cfKey, value.ptr)
    CFRelease(cfKey)
    if (!hasValue) error("Key '$key' not found")
    return value.value ?: error("value for key '$key' not found")
}

private fun CFDictionaryRef.getCFDictionary(key: String): CFDictionaryRef = memScoped { getValue(key).reinterpret() }

private fun CFDictionaryRef.getCFArray(key: String): CFArrayRef = memScoped { getValue(key).reinterpret() }

private fun CFDictionaryRef.getBoolean(key: String): Boolean = memScoped {
    val boolValue: CFBooleanRef = getValue(key).reinterpret()
    return CFBooleanGetValue(boolValue)
}

private fun CFArrayRef.getLong(index: Int): Long = memScoped {
    val value = CFArrayGetValueAtIndex(this@getLong, index.toLong())
    val longValue: LongVar = alloc()
    val hasValue = CFNumberGetValue(value?.reinterpret(), kCFNumberSInt64Type, longValue.ptr)
    if (!hasValue) error("value for index '$index' not found")
    return longValue.value
}

private fun String.toCFStringRef() = CFStringCreateWithCString(null, this, kCFStringEncodingUTF8)

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