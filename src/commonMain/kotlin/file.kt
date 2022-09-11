import kotlinx.cinterop.*
import platform.posix.*

val userHomeDir: String?
    get() = getenv("HOME")?.toKStringFromUtf8()?.takeIf { it.isNotBlank() }
        ?: getpwuid(getuid())?.pointed?.pw_dir?.toKStringFromUtf8()

private fun <T> openFile(path: String, block: (CPointer<FILE>) -> T): T {
    val handle = fopen(path, "rb") ?: error("could not open file")
    return try {
        block(handle)
    } finally {
        fclose(handle)
    }
}

private fun getFileSize(handle: CPointer<FILE>): Long {
    fseek(handle, 0, SEEK_END)
    val size = ftell(handle)
    rewind(handle)
    return size
}

private fun fileExists(path: String) = access(path, F_OK) == 0

fun readFileToString(path: String): String? {
    if (!fileExists(path)) return null

    return memScoped {
        openFile(path) {
            val size = getFileSize(it)

            val stringRef = allocArray<ByteVar>(size + 1)

            val bytesRead = fread(stringRef, size.toULong(), 1, it)
            if (bytesRead == 0.toULong()) error("Read $bytesRead bytes")

            stringRef.toKStringFromUtf8()
        }
    }
}