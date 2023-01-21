import kotlinx.cinterop.*
import platform.Foundation.*
import platform.Foundation.NSURL.Companion.fileURLWithPath


class ExecuteAction(private val command: Command) : Action {
    override suspend fun execute(): Boolean {
        val pipe = NSPipe()
        val process = NSTask().apply {
            standardOutput = pipe
            standardError = pipe
            standardInput = null
            executableURL = fileURLWithPath(command.executable)
            arguments = command.arguments
            command.workingDir?.let { currentDirectoryURL = fileURLWithPath(it) }
        }

        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            if (!process.launchAndReturnError(error.ptr)) {
                println("[ERROR] ${error.value?.localizedDescription}")
                return false
            }
        }

        for (data in pipe.fileHandleForReading) {
            println("[INFO] ${NSString.create(data, NSUTF8StringEncoding)?.toString()?.trim()}")
        }

        return true
    }

    private operator fun NSFileHandle.iterator() = ReadIterator(this)

    private class ReadIterator(private val fileHandle: NSFileHandle) : Iterator<NSData> {

        private var data: NSData? = null

        override fun hasNext(): Boolean {
            val readData = fileHandle.availableData
            data = readData
            return readData.length != 0uL
        }

        override fun next() = data ?: NSData()
    }
}
