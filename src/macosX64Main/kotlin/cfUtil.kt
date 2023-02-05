import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*

class CFMemScope {

    private val items = mutableListOf<CFTypeRef>()

    fun scoped(type: CFTypeRef) = type.also(items::add)

    fun cfArray(list: List<COpaquePointer?>) = list.toCFArray().also(::scoped)

    fun cfMutableArray(list: MutableList<COpaquePointer?>) = list.toCFMutableArray().also(::scoped)

    fun cfDictionary(map: Map<String, COpaquePointer?>) = map.toCFDictionary().also(::scoped)

    fun cfMutableDictionary(map: MutableMap<String, COpaquePointer?>) = map.toCFMutableDictionary().also(::scoped)

    fun cfSet(set: Set<COpaquePointer?>) = set.toCFSet().also(::scoped)

    fun cfMutableSet(set: MutableSet<COpaquePointer?>) = set.toCFMutableSet().also(::scoped)

    fun cfString(string: String) = string.toCFString().also(::scoped)

    fun cfNumber(value: Byte) = value.toCFNumber().also(::scoped)

    fun cfNumber(value: Short) = value.toCFNumber().also(::scoped)

    fun cfNumber(value: Int) = value.toCFNumber().also(::scoped)

    fun cfNumber(value: Long) = value.toCFNumber().also(::scoped)

    fun cfNumber(value: Float) = value.toCFNumber().also(::scoped)

    fun cfNumber(value: Double) = value.toCFNumber().also(::scoped)

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

fun CFTypeRef.asCFDictionary(): CFDictionaryRef {
    check(CFGetTypeID(this) == CFDictionaryGetTypeID()) {
        "value is not of type CFDictionary"
    }
    return this.reinterpret()
}

fun CFTypeRef.asCFMutableDictionary(): CFMutableDictionaryRef {
    check(CFGetTypeID(this) == CFDictionaryGetTypeID()) {
        "value is not of type CFDictionary"
    }
    return this.reinterpret()
}

fun CFTypeRef.asCFArray(): CFArrayRef {
    check(CFGetTypeID(this) == CFArrayGetTypeID()) {
        "value is not of type CFArray"
    }
    return this.reinterpret()
}

fun CFTypeRef.asCFMutableArray(): CFMutableArrayRef {
    check(CFGetTypeID(this) == CFArrayGetTypeID()) {
        "value is not of type CFArray"
    }
    return this.reinterpret()
}

fun CFTypeRef.asCFSet(): CFSetRef {
    check(CFGetTypeID(this) == CFSetGetTypeID()) {
        "value is not of type CFSet"
    }
    return this.reinterpret()
}

fun CFTypeRef.asCFMutableSet(): CFMutableSetRef {
    check(CFGetTypeID(this) == CFSetGetTypeID()) {
        "value is not of type CFSet"
    }
    return this.reinterpret()
}

fun CFTypeRef.asCFNumber(): CFNumberRef {
    check(CFGetTypeID(this) == CFNumberGetTypeID()) {
        "value is not of type CFNumber"
    }
    return this.reinterpret()
}

fun CFTypeRef.asCFBoolean(): CFBooleanRef {
    check(CFGetTypeID(this) == CFBooleanGetTypeID()) {
        "value is not of type CFBoolean"
    }
    return this.reinterpret()
}

fun CFTypeRef.asCFString(): CFStringRef {
    check(CFGetTypeID(this) == CFStringGetTypeID()) {
        "value is not of type CFString"
    }
    return this.reinterpret()
}

fun Set<COpaquePointer?>.toCFSet(): CFSetRef = (this as NSSet)
    .let(::CFBridgingRetain)
    ?.reinterpret()
    ?: error("Could not convert CFSet")

fun cfSetOf(vararg items: COpaquePointer?): CFSetRef = setOf(*items).toCFSet()

@Suppress("UNCHECKED_CAST")
fun CFSetRef.toSet() = CFBridgingRelease(this) as Set<COpaquePointer?>

val CFSetRef.size get() = CFSetGetCount(this).toInt()

fun MutableSet<*>.toCFMutableSet(): CFMutableSetRef = (this as NSSet)
    .let(::CFBridgingRetain)
    ?.reinterpret()
    ?: error("Could not convert CFMutableSet")

@Suppress("FunctionName")
fun CFMutableSet(capacity: Int = 0) = CFSetCreateMutable(null, capacity.toLong(), null)

fun cfMutableSetOf(vararg items: COpaquePointer?) = mutableSetOf(*items).toCFMutableSet()

fun CFMutableSetRef.set(value: COpaquePointer?) {
    CFSetSetValue(this, value)
}

fun CFMutableSetRef.remove(value: COpaquePointer?) {
    CFSetRemoveValue(this, value)
}

@Suppress("UNCHECKED_CAST")
fun CFMutableSetRef.toMutableMap() = CFBridgingRelease(this) as MutableSet<COpaquePointer?>

fun Map<String, COpaquePointer?>.toCFDictionary(): CFDictionaryRef = (this as NSDictionary)
    .let(::CFBridgingRetain)
    ?.reinterpret()
    ?: error("Could not convert CFDictionary")

fun cfDictionaryOf(vararg items: Pair<String, COpaquePointer?>): CFDictionaryRef = mapOf(*items).toCFDictionary()

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

fun CFDictionaryRef.getCFDictionaryOrNull(key: String) = get(key)?.asCFDictionary()

fun CFDictionaryRef.getCFArray(key: String) = checkNotNull(getCFArrayOrNull(key))

fun CFDictionaryRef.getCFArrayOrNull(key: String) = get(key)?.asCFArray()

fun CFDictionaryRef.getString(key: String) = checkNotNull(getStringOrNull(key))

fun CFDictionaryRef.getStringOrNull(key: String) = get(key)?.asCFString()?.getString()

fun CFDictionaryRef.getBoolean(key: String) = checkNotNull(getBooleanOrNull(key))

fun CFDictionaryRef.getBooleanOrNull(key: String) = get(key)?.asCFBoolean()?.getBoolean()

fun CFDictionaryRef.getLong(key: String) = checkNotNull(getLongOrNull(key))

fun CFDictionaryRef.getLongOrNull(key: String) = get(key)?.asCFNumber()?.getLong()

fun CFDictionaryRef.getInt(key: String) = checkNotNull(getIntOrNull(key))

fun CFDictionaryRef.getIntOrNull(key: String) = get(key)?.asCFNumber()?.getInt()

fun CFDictionaryRef.getShort(key: String) = checkNotNull(getShortOrNull(key))

fun CFDictionaryRef.getShortOrNull(key: String) = get(key)?.asCFNumber()?.getShort()

fun CFDictionaryRef.getByte(key: String) = checkNotNull(getByteOrNull(key))

fun CFDictionaryRef.getByteOrNull(key: String) = get(key)?.asCFNumber()?.getByte()

fun CFDictionaryRef.getFloat(key: String) = getFloatOrNull(key)

fun CFDictionaryRef.getFloatOrNull(key: String) = get(key)?.asCFNumber()?.getFloat()

fun CFDictionaryRef.getDouble(key: String) = checkNotNull(getDoubleOrNull(key))

fun CFDictionaryRef.getDoubleOrNull(key: String) = get(key)?.asCFNumber()?.getDouble()

fun MutableMap<String, *>.toCFMutableDictionary(): CFMutableDictionaryRef = (this as NSMutableDictionary)
    .let(::CFBridgingRetain)
    ?.reinterpret()
    ?: error("Could not convert CFMutableDictionary")

@Suppress("FunctionName")
fun CFMutableDictionary(capacity: Int = 0) = CFDictionaryCreateMutable(null, capacity.toLong(), null, null)

fun cfMutableDictionaryOf(vararg items: Pair<String, COpaquePointer?>) = mutableMapOf(*items).toCFMutableDictionary()

operator fun CFMutableDictionaryRef.set(key: String, value: COpaquePointer?) {
    CFDictionarySetValue(this, key.toCFString(), value)
}

fun CFMutableDictionaryRef.remove(key: String) {
    CFDictionaryRemoveValue(this, key.toCFString())
}

@Suppress("UNCHECKED_CAST")
fun CFMutableDictionaryRef.toMutableMap() = CFBridgingRelease(this) as MutableMap<String, COpaquePointer?>

fun List<COpaquePointer?>.toCFArray(): CFArrayRef = (this as NSArray)
    .let(::CFBridgingRetain)
    ?.reinterpret()
    ?: error("Could not convert CFArray")

fun cfArrayOf(vararg items: COpaquePointer) = items.toList().toCFArray()

operator fun CFArrayRef.get(index: Int): COpaquePointer? {
    check(index in 0 until size) { "Index out of bounds" }
    return CFArrayGetValueAtIndex(this@get, index.toLong())
}

val CFArrayRef.size get() = CFArrayGetCount(this).toInt()

@Suppress("UNCHECKED_CAST")
fun CFArrayRef.asList() = CFBridgingRelease(this) as List<COpaquePointer?>

fun CFArrayRef.getCFDictionary(index: Int) = checkNotNull(getCFDictionaryOrNull(index))

fun CFArrayRef.getCFDictionaryOrNull(index: Int) = get(index)?.asCFDictionary()

fun CFArrayRef.getCFArray(index: Int) = checkNotNull(getCFArrayOrNull(index))

fun CFArrayRef.getCFArrayOrNull(index: Int) = get(index)?.asCFArray()

fun CFArrayRef.getString(index: Int) = checkNotNull(getStringOrNull(index))

fun CFArrayRef.getStringOrNull(index: Int) = get(index)?.asCFString()?.getString()

fun CFArrayRef.getBoolean(index: Int) = checkNotNull(getBooleanOrNull(index))

fun CFArrayRef.getBooleanOrNull(index: Int) = get(index)?.asCFBoolean()?.getBoolean()

fun CFArrayRef.getLong(index: Int) = checkNotNull(getLongOrNull(index))

fun CFArrayRef.getLongOrNull(index: Int) = get(index)?.asCFNumber()?.getLong()

fun CFArrayRef.getInt(index: Int) = checkNotNull(getIntOrNull(index))

fun CFArrayRef.getIntOrNull(index: Int) = get(index)?.asCFNumber()?.getInt()

fun CFArrayRef.getShort(index: Int) = checkNotNull(getShortOrNull(index))

fun CFArrayRef.getShortOrNull(index: Int) = get(index)?.asCFNumber()?.getShort()

fun CFArrayRef.getByte(index: Int) = checkNotNull(getByteOrNull(index))

fun CFArrayRef.getByteOrNull(index: Int) = get(index)?.asCFNumber()?.getByte()

fun CFArrayRef.getFloat(index: Int) = checkNotNull(getFloatOrNull(index))

fun CFArrayRef.getFloatOrNull(index: Int) = get(index)?.asCFNumber()?.getFloat()

fun CFArrayRef.getDouble(index: Int) = checkNotNull(getDoubleOrNull(index))

fun CFArrayRef.getDoubleOrNull(index: Int) = get(index)?.asCFNumber()?.getDouble()

fun MutableList<COpaquePointer?>.toCFMutableArray(): CFMutableArrayRef = (this as NSMutableArray)
    .let(::CFBridgingRetain)
    ?.reinterpret()
    ?: error("Could not convert CFMutableSet")

@Suppress("FunctionName")
fun CFMutableArray(size: Int = 0) = CFArrayCreateMutable(null, size.toLong(), null)

fun cfMutableArrayOf(vararg items: COpaquePointer?) = mutableListOf(*items).toCFMutableArray()

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

fun Byte.toCFNumber() = CFBridgingRetain(NSNumber(char = this))?.asCFNumber()
    ?: error("Could not create CFNumber")

fun Short.toCFNumber() = CFBridgingRetain(NSNumber(short = this))?.asCFNumber()
    ?: error("Could not create CFNumber")

fun Int.toCFNumber() = CFBridgingRetain(NSNumber(int = this))?.asCFNumber()
    ?: error("Could not create CFNumber")

fun Long.toCFNumber() = CFBridgingRetain(NSNumber(long = this))?.asCFNumber()
    ?: error("Could not create CFNumber")

fun Float.toCFNumber() = CFBridgingRetain(NSNumber(float = this))?.asCFNumber()
    ?: error("Could not create CFNumber")

fun Double.toCFNumber() = CFBridgingRetain(NSNumber(double = this))?.asCFNumber()
    ?: error("Could not create CFNumber")
