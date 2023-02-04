import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSNumber

fun mutableCFDictionary(capacity: Int = 0) = CFDictionaryCreateMutable(null, capacity.toLong(), null, null)

fun cfDictionary(vararg items: Pair<String, CValuesRef<*>>): CFDictionaryRef? {
    val dict = CFDictionaryCreateMutable(null, items.size.toLong(), null, null)
    items.forEach { (key, value) -> dict?.setValue(key, value) }
    return dict
}

fun CFMutableDictionaryRef.setValue(key: String, value: CValuesRef<*>) {
    CFDictionarySetValue(this, key.toCFString(), value)
}

fun CFStringRef.toKStringFromUtf8() = CFStringGetCStringPtr(this, kCFStringEncodingUTF8)?.toKStringFromUtf8()

fun CFDictionaryRef.getValue(key: String): CPointer<out CPointed> = memScoped {
    val cfKey = key.toCFString()
    val value: COpaquePointerVar = alloc()
    val hasValue: Boolean = CFDictionaryGetValueIfPresent(this@getValue, cfKey, value.ptr)
    CFRelease(cfKey)
    if (!hasValue) error("Key '$key' not found")
    return value.value ?: error("value for key '$key' not found")
}

fun CFDictionaryRef.getCFDictionary(key: String): CFDictionaryRef = memScoped { getValue(key).reinterpret() }

fun CFDictionaryRef.getCFArray(key: String): CFArrayRef = memScoped { getValue(key).reinterpret() }

fun CFDictionaryRef.getBoolean(key: String): Boolean = memScoped {
    val boolValue: CFBooleanRef = getValue(key).reinterpret()
    return CFBooleanGetValue(boolValue)
}

fun CFArrayRef.getLong(index: Int): Long = memScoped {
    val value = CFArrayGetValueAtIndex(this@getLong, index.toLong())
    val longValue: LongVar = alloc()
    val hasValue = CFNumberGetValue(value?.reinterpret(), kCFNumberSInt64Type, longValue.ptr)
    if (!hasValue) error("value for index '$index' not found")
    return longValue.value
}

fun String.toCFString() = CFStringCreateWithCString(null, this, kCFStringEncodingUTF8)
    ?: error("Could not create CFString")

fun Int.toCFNumber(): CFNumberRef = CFBridgingRetain(NSNumber(int = this))?.reinterpret()
    ?: error("Could not create CFNumber")
