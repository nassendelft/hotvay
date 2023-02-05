import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*

fun Map<String, COpaquePointer?>.toCFDictionary(): CFDictionaryRef? =
    (this as NSDictionary).let(::CFBridgingRetain)?.reinterpret()

fun cfDictionaryOf(vararg items: Pair<String, COpaquePointer?>): CFDictionaryRef? = mapOf(*items).toCFDictionary()

operator fun CFDictionaryRef.get(key: String): COpaquePointer? = memScoped {
    val cfKey = key.toCFString()
    val value: COpaquePointerVar = alloc()
    val hasValue: Boolean = CFDictionaryGetValueIfPresent(this@get, cfKey, value.ptr)
    CFRelease(cfKey)
    if (!hasValue) return null
    return value.value ?: error("value for key '$key' not found")
}

@Suppress("UNCHECKED_CAST")
fun CFDictionaryRef.toMap() = CFBridgingRelease(this) as Map<String, COpaquePointer?>

fun CFDictionaryRef.getCFDictionary(key: String) = checkNotNull(getCFDictionaryOrNull(key))

fun CFDictionaryRef.getCFDictionaryOrNull(key: String): CFDictionaryRef? {
    val value = get(key)
    check(CFGetTypeID(value) == CFDictionaryGetTypeID()) {
        "value is not of type CFDictionary"
    }
    return value?.reinterpret()
}

fun CFDictionaryRef.getCFArray(key: String) = checkNotNull(getCFArrayOrNull(key))

fun CFDictionaryRef.getCFArrayOrNull(key: String): CFArrayRef? {
    val value = get(key)
    check(CFGetTypeID(value) == CFArrayGetTypeID()) {
        "value is not of type CFArray"
    }
    return value?.reinterpret()
}

fun CFDictionaryRef.getString(key: String) = checkNotNull(getStringOrNull(key))

fun CFDictionaryRef.getStringOrNull(key: String): String? {
    val value = get(key)
    check(CFGetTypeID(value) == CFStringGetTypeID()) {
        "value is not of type CFBoolean"
    }
    val stringValue: CFStringRef = value?.reinterpret() ?: return null
    return stringValue.getString()
}

fun CFDictionaryRef.getBoolean(key: String) = checkNotNull(getBooleanOrNull(key))

fun CFDictionaryRef.getBooleanOrNull(key: String): Boolean? {
    val value = get(key)
    check(CFGetTypeID(value) == CFBooleanGetTypeID()) {
        "value is not of type CFBoolean"
    }
    val boolValue: CFBooleanRef = value?.reinterpret() ?: return null
    return CFBooleanGetValue(boolValue)
}

fun CFDictionaryRef.getLong(key: String) = checkNotNull(getLongOrNull(key))

fun CFDictionaryRef.getLongOrNull(key: String): Long? {
    val value = get(key)
    check(CFGetTypeID(value) == CFNumberGetTypeID()) {
        "value is not of type CFNumber"
    }
    val numberValue: CFNumberRef? = value?.reinterpret()
    return numberValue?.getLong()
}

fun CFDictionaryRef.getInt(key: String) = checkNotNull(getIntOrNull(key))

fun CFDictionaryRef.getIntOrNull(key: String): Int? {
    val value = get(key)
    check(CFGetTypeID(value) == CFNumberGetTypeID()) {
        "value is not of type CFNumber"
    }
    val numberValue: CFNumberRef? = value?.reinterpret()
    return numberValue?.getInt()
}

fun CFDictionaryRef.getShort(key: String) = checkNotNull(getShortOrNull(key))

fun CFDictionaryRef.getShortOrNull(key: String): Short? {
    val value = get(key)
    check(CFGetTypeID(value) == CFNumberGetTypeID()) {
        "value is not of type CFNumber"
    }
    val numberValue: CFNumberRef? = value?.reinterpret()
    return numberValue?.getShort()
}

fun CFDictionaryRef.getByte(key: String) = checkNotNull(getByteOrNull(key))

fun CFDictionaryRef.getByteOrNull(key: String): Byte? {
    val value = get(key)
    check(CFGetTypeID(value) == CFNumberGetTypeID()) {
        "value is not of type CFNumber"
    }
    val numberValue: CFNumberRef? = value?.reinterpret()
    return numberValue?.getByte()
}

fun CFDictionaryRef.getFloat(key: String) = getFloatOrNull(key)

fun CFDictionaryRef.getFloatOrNull(key: String): Float? {
    val value = get(key)
    check(CFGetTypeID(value) == CFNumberGetTypeID()) {
        "value is not of type CFNumber"
    }
    val numberValue: CFNumberRef? = value?.reinterpret()
    return numberValue?.getFloat()
}

fun CFDictionaryRef.getDouble(key: String) = checkNotNull(getDoubleOrNull(key))

fun CFDictionaryRef.getDoubleOrNull(key: String): Double? {
    val value = get(key)
    check(CFGetTypeID(value) == CFNumberGetTypeID()) {
        "value is not of type CFNumber"
    }
    val numberValue: CFNumberRef? = value?.reinterpret()
    return numberValue?.getDouble()
}

fun MutableMap<String, *>.toMutableCFDictionary(): CFMutableDictionaryRef? =
    (this as NSMutableDictionary).let(::CFBridgingRetain)?.reinterpret()

@Suppress("FunctionName")
fun MutableCFDictionary(capacity: Int = 0) = CFDictionaryCreateMutable(null, capacity.toLong(), null, null)

fun mutableCFDictionaryOf(vararg items: Pair<String, COpaquePointer?>): CFMutableDictionaryRef? =
    mutableMapOf(*items).toMutableCFDictionary()

operator fun CFMutableDictionaryRef.set(key: String, value: CValuesRef<*>) {
    CFDictionarySetValue(this, key.toCFString(), value)
}

fun CFMutableDictionaryRef.remove(key: String) {
    CFDictionaryRemoveValue(this, key.toCFString())
}

@Suppress("UNCHECKED_CAST")
fun CFMutableDictionaryRef.toMutableMap() = CFBridgingRelease(this) as MutableMap<String, COpaquePointer?>

fun List<COpaquePointer?>.toCFArray(): CFArrayRef? =
    (this as NSArray).let(::CFBridgingRetain)?.reinterpret()

fun cfArrayOf(vararg items: COpaquePointer) = items.toList().toCFArray()

private class CFArrayIterator(private val array: CFArrayRef) : ListIterator<COpaquePointer?> {

    private val count = CFArrayGetCount(array).toInt()
    private var index = 0

    override fun hasNext() = index < count

    override fun hasPrevious() = index > 0

    override fun next() = CFArrayGetValueAtIndex(array, (index++).toLong())

    override fun nextIndex() = count + 1

    override fun previous() = CFArrayGetValueAtIndex(array, (index--).toLong())

    override fun previousIndex() = count - 1
}

operator fun CFArrayRef.iterator(): Iterator<COpaquePointer?> = CFArrayIterator(this)

operator fun CFArrayRef.get(index: Int): COpaquePointer? {
    val count = CFArrayGetCount(this)
    check(index <= count - 1) { "Index ($index) out of bounds ($count)" }
    return CFArrayGetValueAtIndex(this@get, index.toLong())
}

@Suppress("UNCHECKED_CAST")
fun CFArrayRef.asList() = CFBridgingRelease(this) as List<COpaquePointer?>

fun CFArrayRef.getCFDictionary(index: Int) = checkNotNull(getCFDictionaryOrNull(index))

fun CFArrayRef.getCFDictionaryOrNull(index: Int): CFDictionaryRef? {
    val value = get(index)
    check(CFGetTypeID(value) == CFDictionaryGetTypeID()) {
        "value is not of type CFDictionary"
    }
    return value?.reinterpret()
}

fun CFArrayRef.getCFArray(index: Int) = checkNotNull(getCFArrayOrNull(index))

fun CFArrayRef.getCFArrayOrNull(index: Int): CFArrayRef? {
    val value = get(index)
    check(CFGetTypeID(value) == CFArrayGetTypeID()) {
        "value is not of type CFArray"
    }
    return value?.reinterpret()
}

fun CFArrayRef.getString(index: Int) = checkNotNull(getStringOrNull(index))

fun CFArrayRef.getStringOrNull(index: Int): String? {
    val value = get(index)
    check(CFGetTypeID(value) == CFStringGetTypeID()) {
        "value is not of type CFBoolean"
    }
    val stringValue: CFStringRef = value?.reinterpret() ?: return null
    return stringValue.getString()
}

fun CFArrayRef.getBoolean(index: Int) = checkNotNull(getBooleanOrNull(index))

fun CFArrayRef.getBooleanOrNull(index: Int): Boolean? {
    val value = get(index)
    check(CFGetTypeID(value) == CFBooleanGetTypeID()) {
        "value is not of type CFBoolean"
    }
    val boolValue: CFBooleanRef = value?.reinterpret() ?: return null
    return CFBooleanGetValue(boolValue)
}

fun CFArrayRef.getLong(index: Int) = checkNotNull(getLongOrNull(index))

fun CFArrayRef.getLongOrNull(index: Int): Long? {
    val value = get(index)
    check(CFGetTypeID(value) == CFNumberGetTypeID()) {
        "value is not of type CFNumber"
    }
    val numberValue: CFNumberRef? = value?.reinterpret()
    return numberValue?.getLong()
}

fun CFArrayRef.getInt(index: Int) = checkNotNull(getIntOrNull(index))

fun CFArrayRef.getIntOrNull(index: Int): Int? {
    val value = get(index)
    check(CFGetTypeID(value) == CFNumberGetTypeID()) {
        "value is not of type CFNumber"
    }
    val numberValue: CFNumberRef? = value?.reinterpret()
    return numberValue?.getInt()
}

fun CFArrayRef.getShort(index: Int) = checkNotNull(getShortOrNull(index))

fun CFArrayRef.getShortOrNull(index: Int): Short? {
    val value = get(index)
    check(CFGetTypeID(value) == CFNumberGetTypeID()) {
        "value is not of type CFNumber"
    }
    val numberValue: CFNumberRef? = value?.reinterpret()
    return numberValue?.getShort()
}

fun CFArrayRef.getByte(index: Int) = checkNotNull(getByteOrNull(index))

fun CFArrayRef.getByteOrNull(index: Int): Byte? {
    val value = get(index)
    check(CFGetTypeID(value) == CFNumberGetTypeID()) {
        "value is not of type CFNumber"
    }
    val numberValue: CFNumberRef? = value?.reinterpret()
    return numberValue?.getByte()
}

fun CFArrayRef.getFloat(index: Int) = checkNotNull(getFloatOrNull(index))

fun CFArrayRef.getFloatOrNull(index: Int): Float? {
    val value = get(index)
    check(CFGetTypeID(value) == CFNumberGetTypeID()) {
        "value is not of type CFNumber"
    }
    val numberValue: CFNumberRef? = value?.reinterpret()
    return numberValue?.getFloat()
}

fun CFArrayRef.getDouble(index: Int) = checkNotNull(getDoubleOrNull(index))

fun CFArrayRef.getDoubleOrNull(index: Int): Double? {
    val value = get(index)
    check(CFGetTypeID(value) == CFNumberGetTypeID()) {
        "value is not of type CFNumber"
    }
    val numberValue: CFNumberRef? = value?.reinterpret()
    return numberValue?.getDouble()
}

fun MutableList<COpaquePointer?>.toMutableCFArray(): CFMutableArrayRef? =
    (this as NSMutableArray).let(::CFBridgingRetain)?.reinterpret()

@Suppress("FunctionName")
fun MutableCFArray(size: Int = 0) = CFArrayCreateMutable(null, size.toLong(), null)

fun mutableCFArrayOf(vararg items: COpaquePointer?): CFMutableArrayRef? = mutableListOf(*items).toMutableCFArray()

operator fun CFMutableArrayRef.set(index: Int, item: COpaquePointer) =
    CFArraySetValueAtIndex(this, index.toLong(), item)

fun CFMutableArrayRef.insert(index: Int, item: COpaquePointer) =
    CFArrayInsertValueAtIndex(this, index.toLong(), item)

fun CFMutableArrayRef.remove(index: Int) =
    CFArrayRemoveValueAtIndex(this, index.toLong())

fun CFNumberRef.getLong(): Long = memScoped {
    val value: LongVar = alloc()
    val hasValue = CFNumberGetValue(this@getLong, kCFNumberSInt64Type, value.ptr)
    if (!hasValue) error("Could not get value")
    return value.value
}

fun CFNumberRef.getInt(): Int = memScoped {
    val value: IntVar = alloc()
    val hasValue = CFNumberGetValue(this@getInt, kCFNumberSInt32Type, value.ptr)
    if (!hasValue) error("Could not get value")
    return value.value
}

fun CFNumberRef.getShort(): Short = memScoped {
    val value: ShortVar = alloc()
    val hasValue = CFNumberGetValue(this@getShort, kCFNumberSInt16Type, value.ptr)
    if (!hasValue) error("Could not get value")
    return value.value
}

fun CFNumberRef.getByte(): Byte = memScoped {
    val value: ByteVar = alloc()
    val hasValue = CFNumberGetValue(this@getByte, kCFNumberSInt8Type, value.ptr)
    if (!hasValue) error("Could not get value")
    return value.value
}

fun CFNumberRef.getFloat(): Float = memScoped {
    val value: FloatVar = alloc()
    val hasValue = CFNumberGetValue(this@getFloat, kCFNumberFloat32Type, value.ptr)
    if (!hasValue) error("Could not get value")
    return value.value
}

fun CFNumberRef.getDouble(): Double = memScoped {
    val value: DoubleVar = alloc()
    val hasValue = CFNumberGetValue(this@getDouble, kCFNumberFloat64Type, value.ptr)
    if (!hasValue) error("Could not get value")
    return value.value
}

fun CFStringRef.getString() = CFStringGetCStringPtr(this, kCFStringEncodingUTF8)?.toKStringFromUtf8()

fun String.toCFString() = CFStringCreateWithCString(null, this, kCFStringEncodingUTF8)
    ?: error("Could not create CFString")

fun Byte.toCFNumber(): CFNumberRef = CFBridgingRetain(NSNumber(char = this))?.reinterpret()
    ?: error("Could not create CFNumber")

fun Short.toCFNumber(): CFNumberRef = CFBridgingRetain(NSNumber(short = this))?.reinterpret()
    ?: error("Could not create CFNumber")

fun Int.toCFNumber(): CFNumberRef = CFBridgingRetain(NSNumber(int = this))?.reinterpret()
    ?: error("Could not create CFNumber")

fun Long.toCFNumber(): CFNumberRef = CFBridgingRetain(NSNumber(long = this))?.reinterpret()
    ?: error("Could not create CFNumber")

fun Float.toCFNumber(): CFNumberRef = CFBridgingRetain(NSNumber(float = this))?.reinterpret()
    ?: error("Could not create CFNumber")

fun Double.toCFNumber(): CFNumberRef = CFBridgingRetain(NSNumber(double = this))?.reinterpret()
    ?: error("Could not create CFNumber")
