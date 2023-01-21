@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

import kotlinx.cinterop.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import platform.CoreFoundation.*
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSNumber
import platform.IOKit.*

// https://developer.apple.com/library/archive/technotes/tn2187/_index.html
// https://eleccelerator.com/tutorial-about-usb-hid-report-descriptors/

internal actual fun connectDevice(vendorId: Int, productId: Int) = channelFlow<DeviceEvent> {
    val runLoop = CFRunLoopGetCurrent()

    val managerRef = IOHIDManagerCreate(kCFAllocatorDefault, kIOHIDOptionsTypeNone)

    val deviceMatch = CFDictionaryCreateMutable(null, 2, null, null)
    CFDictionarySetValue(
        deviceMatch,
        CFStringCreateWithCString(null, kIOHIDVendorIDKey, kCFStringEncodingUTF8),
        CFBridgingRetain(NSNumber(int = vendorId))
    )
    CFDictionarySetValue(
        deviceMatch,
        CFStringCreateWithCString(null, kIOHIDProductIDKey, kCFStringEncodingUTF8),
        CFBridgingRetain(NSNumber(int = productId))
    )

    IOHIDManagerSetDeviceMatching(managerRef, deviceMatch)

    IOHIDManagerScheduleWithRunLoop(managerRef, runLoop, kCFRunLoopDefaultMode)

    IOHIDManagerOpen(managerRef, 0)

    val matchingCallback: IOHIDDeviceCallback = staticCFunction { context, result, sender, device ->
        if (device == null) return@staticCFunction
        context?.asStableRef<ProducerScope<DeviceEvent>>()
            ?.get()
            ?.trySend(DeviceEvent.Connected(device.serial))

        readDevice(device, context)
    }

    val removalCallback: IOHIDDeviceCallback =
        staticCFunction { context, result, sender, device ->
            if (device == null) return@staticCFunction
            context?.asStableRef<ProducerScope<DeviceEvent>>()
                ?.get()
                ?.trySend(DeviceEvent.Disconnected(device.serial))
        }

    val context = StableRef.create(this@channelFlow)
    IOHIDManagerRegisterDeviceMatchingCallback(managerRef, matchingCallback, context.asCPointer())
    IOHIDManagerRegisterDeviceRemovalCallback(managerRef, removalCallback, context.asCPointer())

    @Suppress("OPT_IN_USAGE")
    invokeOnClose {
        IOHIDManagerClose(managerRef, kIOHIDOptionsTypeNone)
        context.dispose()
        CFRelease(deviceMatch)
        CFRunLoopStop(runLoop)
    }

    CFRunLoopRun()
}.distinctUntilChanged()

private fun readDevice(deviceRef: IOHIDDeviceRef?, context: COpaquePointer?) = memScoped {
    val callback: IOHIDReportCallback =
        staticCFunction { context, result, sender, type, reportId, report, reportLength ->
            val data = report ?: return@staticCFunction
            val device: IOHIDDeviceRef? = sender?.reinterpret()
            val keyEvent = DeviceEvent.Key(
                device?.serial,
                data[3],
                if (data[4] == 0u.toUByte()) KeyState.DOWN else KeyState.UP,
            )
            context?.asStableRef<ProducerScope<DeviceEvent>>()
                ?.get()
                ?.trySend(keyEvent)
        }

    val data = allocArray<UByteVar>(16)
    IOHIDDeviceRegisterInputReportCallback(deviceRef, data, 16, callback, context)
}

@Suppress("UNCHECKED_CAST")
private val IOHIDDeviceRef.serial: String?
    get() {
        val key = CFStringCreateWithCString(null, kIOHIDSerialNumberKey, kCFStringEncodingUTF8)
        return IOHIDDeviceGetProperty(this, key)?.let { it as CFStringRef }?.toKStringFromUtf8()
    }

private fun CFStringRef.toKStringFromUtf8() = CFStringGetCStringPtr(this, kCFStringEncodingUTF8)?.toKStringFromUtf8()
