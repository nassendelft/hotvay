@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import platform.CoreFoundation.*
import platform.CoreServices.*

actual fun watchFile(vararg filePath: String) = channelFlow<Unit> {
    val runLoop = CFRunLoopGetCurrent()

    val pathsToWatch = cfArrayOf(*filePath.map(String::toCFString).toTypedArray())
    val callback: FSEventStreamCallback =
        staticCFunction { streamRef, clientCallBackInfo, numEvents, eventPaths, eventFlags, eventIds ->
            clientCallBackInfo?.reinterpret<FSEventStreamContext>()
                ?.asStableRef<ProducerScope<Unit>>()
                ?.get()
                ?.trySend(Unit)
        }

    val channelRef = StableRef.create(this@channelFlow)
    val context = cValue<FSEventStreamContext> { info = channelRef.asCPointer() }

    val streamRef = FSEventStreamCreate(
        null,
        callback,
        context,
        pathsToWatch,
        kFSEventStreamEventIdSinceNow,
        0.0,
        kFSEventStreamCreateFlagFileEvents
    )

    @Suppress("OPT_IN_USAGE")
    invokeOnClose {
        channelRef.dispose()
        FSEventStreamUnscheduleFromRunLoop(streamRef, runLoop, kCFRunLoopDefaultMode)
        CFRelease(pathsToWatch)
    }

    FSEventStreamScheduleWithRunLoop(streamRef, runLoop, kCFRunLoopDefaultMode)
    FSEventStreamStart(streamRef)

    CFRunLoopRun()
}.flowOn(Dispatchers.Default)
