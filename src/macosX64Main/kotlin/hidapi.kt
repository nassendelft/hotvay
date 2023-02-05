@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

import kotlinx.cinterop.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import platform.CoreFoundation.*
import platform.IOKit.*

// https://developer.apple.com/library/archive/technotes/tn2187/_index.html
// https://eleccelerator.com/tutorial-about-usb-hid-report-descriptors/

internal actual fun connectDevice(vendorId: Int, productId: Int) = channelFlow<DeviceEvent> {
    val runLoop = CFRunLoopGetCurrent()

    val managerRef = IOHIDManagerCreate(kCFAllocatorDefault, kIOHIDOptionsTypeNone)

    val deviceMatch = cfDictionaryOf(
        kIOHIDVendorIDKey to vendorId.toCFNumber(),
        kIOHIDProductIDKey to productId.toCFNumber()
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

private val IOHIDDeviceRef.serial: String?
    get() {
        val key = kIOHIDSerialNumberKey.toCFString()
        val cfString: CFStringRef? = IOHIDDeviceGetProperty(this, key)?.reinterpret()
        CFRelease(key)
        return cfString?.getString()
    }
