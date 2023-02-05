import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*

class CFMemScope {

    private val items = mutableListOf<CFTypeRef?>()

    fun cfType(type: CFTypeRef?) = type.also(items::add)

    fun cfArray(list: List<COpaquePointer?>) = list.toCFArray().also(items::add)

    fun cfMutableArray(list: MutableList<COpaquePointer?>) = list.toCFMutableArray().also(items::add)

    fun cfDictionary(map: Map<String, COpaquePointer?>) = map.toCFDictionary().also(items::add)

    fun cfMutableDictionary(map: MutableMap<String, COpaquePointer?>) = map.toCFMutableDictionary().also(items::add)

    fun cfSet(set: Set<COpaquePointer?>) = set.toCFSet().also(items::add)

    fun cfMutableSet(set: MutableSet<COpaquePointer?>) = set.toCFMutableSet().also(items::add)

    fun cfString(string: String) = string.toCFString().also(items::add)

    fun cfNumber(value: Byte) = value.toCFNumber().also(items::add)

    fun cfNumber(value: Short) = value.toCFNumber().also(items::add)

    fun cfNumber(value: Int) = value.toCFNumber().also(items::add)

    fun cfNumber(value: Long) = value.toCFNumber().also(items::add)

    fun cfNumber(value: Float) = value.toCFNumber().also(items::add)

    fun cfNumber(value: Double) = value.toCFNumber().also(items::add)

    @PublishedApi
    internal fun clear() = items.forEach(::CFRelease)
}

inline fun <R> cfMemScoped(block: CFMemScope.() -> R): R {
    val scope = CFMemScope()
    try {
        return scope.block()
    } finally {
        scope.clear()
    }
}

fun CFTypeRef?.asCFDictionary(): CFDictionaryRef? {
    check(CFGetTypeID(this) == CFDictionaryGetTypeID()) {
        "value is not of type CFDictionary"
    }
    return this?.reinterpret()
}

fun CFTypeRef?.asCFMutableDictionary(): CFMutableDictionaryRef? {
    check(CFGetTypeID(this) == CFDictionaryGetTypeID()) {
        "value is not of type CFDictionary"
    }
    return this?.reinterpret()
}

fun CFTypeRef?.asCFArray(): CFArrayRef? {
    check(CFGetTypeID(this) == CFArrayGetTypeID()) {
        "value is not of type CFArray"
    }
    return this?.reinterpret()
}

fun CFTypeRef?.asCFMutableArray(): CFMutableArrayRef? {
    check(CFGetTypeID(this) == CFArrayGetTypeID()) {
        "value is not of type CFArray"
    }
    return this?.reinterpret()
}

fun CFTypeRef?.asCFSet(): CFSetRef? {
    check(CFGetTypeID(this) == CFSetGetTypeID()) {
        "value is not of type CFSet"
    }
    return this?.reinterpret()
}

fun CFTypeRef?.asCFMutableSet(): CFMutableSetRef? {
    check(CFGetTypeID(this) == CFSetGetTypeID()) {
        "value is not of type CFSet"
    }
    return this?.reinterpret()
}

fun CFTypeRef?.asCFNumber(): CFNumberRef? {
    check(CFGetTypeID(this) == CFNumberGetTypeID()) {
        "value is not of type CFNumber"
    }
    return this?.reinterpret()
}

fun CFTypeRef?.asCFBoolean(): CFBooleanRef? {
    check(CFGetTypeID(this) == CFBooleanGetTypeID()) {
        "value is not of type CFBoolean"
    }
    return this?.reinterpret()
}

fun CFTypeRef?.asCFString(): CFStringRef? {
    check(CFGetTypeID(this) == CFStringGetTypeID()) {
        "value is not of type CFString"
    }
    return this?.reinterpret()
}

fun Set<COpaquePointer?>.toCFSet(): CFSetRef? =
    (this as NSSet).let(::CFBridgingRetain)?.reinterpret()

fun cfSetOf(vararg items: COpaquePointer?): CFSetRef? = setOf(*items).toCFSet()

@Suppress("UNCHECKED_CAST")
fun CFSetRef.toSet() = CFBridgingRelease(this) as Set<COpaquePointer?>

val CFSetRef.size get() = CFSetGetCount(this).toInt()

fun MutableSet<*>.toCFMutableSet(): CFMutableSetRef? =
    (this as NSSet).let(::CFBridgingRetain)?.reinterpret()

@Suppress("FunctionName")
fun CFMutableSet(capacity: Int = 0) = CFSetCreateMutable(null, capacity.toLong(), null)

fun cfMutableSetOf(vararg items: COpaquePointer?): CFMutableSetRef? =
    mutableSetOf(*items).toCFMutableSet()

fun CFMutableSetRef.set(value: COpaquePointer?) {
    CFSetSetValue(this, value)
}

fun CFMutableSetRef.remove(value: COpaquePointer?) {
    CFSetRemoveValue(this, value)
}

@Suppress("UNCHECKED_CAST")
fun CFMutableSetRef.toMutableMap() = CFBridgingRelease(this) as MutableSet<COpaquePointer?>

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

val CFDictionaryRef.size get() = CFDictionaryGetCount(this).toInt()

@Suppress("UNCHECKED_CAST")
fun CFDictionaryRef.toMap() = CFBridgingRelease(this) as Map<String, COpaquePointer?>

fun CFDictionaryRef.getCFDictionary(key: String) = checkNotNull(getCFDictionaryOrNull(key))

fun CFDictionaryRef.getCFDictionaryOrNull(key: String) = get(key).asCFDictionary()

fun CFDictionaryRef.getCFArray(key: String) = checkNotNull(getCFArrayOrNull(key))

fun CFDictionaryRef.getCFArrayOrNull(key: String) = get(key).asCFArray()

fun CFDictionaryRef.getString(key: String) = checkNotNull(getStringOrNull(key))

fun CFDictionaryRef.getStringOrNull(key: String) = get(key).asCFString()?.getString()

fun CFDictionaryRef.getBoolean(key: String) = checkNotNull(getBooleanOrNull(key))

fun CFDictionaryRef.getBooleanOrNull(key: String) = get(key).asCFBoolean()?.getBoolean()

fun CFDictionaryRef.getLong(key: String) = checkNotNull(getLongOrNull(key))

fun CFDictionaryRef.getLongOrNull(key: String) = get(key).asCFNumber()?.getLong()

fun CFDictionaryRef.getInt(key: String) = checkNotNull(getIntOrNull(key))

fun CFDictionaryRef.getIntOrNull(key: String) = get(key).asCFNumber()?.getInt()

fun CFDictionaryRef.getShort(key: String) = checkNotNull(getShortOrNull(key))

fun CFDictionaryRef.getShortOrNull(key: String) = get(key).asCFNumber()?.getShort()

fun CFDictionaryRef.getByte(key: String) = checkNotNull(getByteOrNull(key))

fun CFDictionaryRef.getByteOrNull(key: String) = get(key).asCFNumber()?.getByte()

fun CFDictionaryRef.getFloat(key: String) = getFloatOrNull(key)

fun CFDictionaryRef.getFloatOrNull(key: String) = get(key).asCFNumber()?.getFloat()

fun CFDictionaryRef.getDouble(key: String) = checkNotNull(getDoubleOrNull(key))

fun CFDictionaryRef.getDoubleOrNull(key: String) = get(key).asCFNumber()?.getDouble()

fun MutableMap<String, *>.toCFMutableDictionary(): CFMutableDictionaryRef? =
    (this as NSMutableDictionary).let(::CFBridgingRetain)?.reinterpret()

@Suppress("FunctionName")
fun CFMutableDictionary(capacity: Int = 0) = CFDictionaryCreateMutable(null, capacity.toLong(), null, null)

fun cfMutableDictionaryOf(vararg items: Pair<String, COpaquePointer?>): CFMutableDictionaryRef? =
    mutableMapOf(*items).toCFMutableDictionary()

operator fun CFMutableDictionaryRef.set(key: String, value: COpaquePointer?) {
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

operator fun CFArrayRef.get(index: Int): COpaquePointer? {
    val count = CFArrayGetCount(this)
    check(index <= count - 1) { "Index ($index) out of bounds ($count)" }
    return CFArrayGetValueAtIndex(this@get, index.toLong())
}

val CFArrayRef.size get() = CFArrayGetCount(this).toInt()

@Suppress("UNCHECKED_CAST")
fun CFArrayRef.asList() = CFBridgingRelease(this) as List<COpaquePointer?>

fun CFArrayRef.getCFDictionary(index: Int) = checkNotNull(getCFDictionaryOrNull(index))

fun CFArrayRef.getCFDictionaryOrNull(index: Int) = get(index).asCFDictionary()

fun CFArrayRef.getCFArray(index: Int) = checkNotNull(getCFArrayOrNull(index))

fun CFArrayRef.getCFArrayOrNull(index: Int) = get(index).asCFArray()

fun CFArrayRef.getString(index: Int) = checkNotNull(getStringOrNull(index))

fun CFArrayRef.getStringOrNull(index: Int) = get(index).asCFString()?.getString()

fun CFArrayRef.getBoolean(index: Int) = checkNotNull(getBooleanOrNull(index))

fun CFArrayRef.getBooleanOrNull(index: Int) = get(index).asCFBoolean()?.getBoolean()

fun CFArrayRef.getLong(index: Int) = checkNotNull(getLongOrNull(index))

fun CFArrayRef.getLongOrNull(index: Int) = get(index).asCFNumber()?.getLong()

fun CFArrayRef.getInt(index: Int) = checkNotNull(getIntOrNull(index))

fun CFArrayRef.getIntOrNull(index: Int) = get(index).asCFNumber()?.getInt()

fun CFArrayRef.getShort(index: Int) = checkNotNull(getShortOrNull(index))

fun CFArrayRef.getShortOrNull(index: Int) = get(index).asCFNumber()?.getShort()

fun CFArrayRef.getByte(index: Int) = checkNotNull(getByteOrNull(index))

fun CFArrayRef.getByteOrNull(index: Int) = get(index).asCFNumber()?.getByte()

fun CFArrayRef.getFloat(index: Int) = checkNotNull(getFloatOrNull(index))

fun CFArrayRef.getFloatOrNull(index: Int) = get(index).asCFNumber()?.getFloat()

fun CFArrayRef.getDouble(index: Int) = checkNotNull(getDoubleOrNull(index))

fun CFArrayRef.getDoubleOrNull(index: Int) = get(index).asCFNumber()?.getDouble()

fun MutableList<COpaquePointer?>.toCFMutableArray(): CFMutableArrayRef? =
    (this as NSMutableArray).let(::CFBridgingRetain)?.reinterpret()

@Suppress("FunctionName")
fun CFMutableArray(size: Int = 0) = CFArrayCreateMutable(null, size.toLong(), null)

fun cfMutableArrayOf(vararg items: COpaquePointer?): CFMutableArrayRef? = mutableListOf(*items).toCFMutableArray()

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

fun CFBooleanRef.getBoolean() = CFBooleanGetValue(this)

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
