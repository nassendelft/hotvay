import kotlinx.cinterop.*
import platform.posix.*

val userHomeDir: String?
    get() = memScoped {
        getenv("HOME")?.toKStringFromUtf8()?.takeIf { it.isNotBlank() }
            ?: getpwuid(getuid())?.pointed?.pw_dir?.toKStringFromUtf8()
    }

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

fun readFileToString(path: String) = memScoped {
    openFile(path) {
        val size = getFileSize(it)

        val stringRef = allocArray<ByteVar>(size + 1)

        val bytesRead = fread(stringRef, size.toULong(), 1, it)
        if (bytesRead == 0.toULong()) error("Read $bytesRead bytes")

        stringRef.toKStringFromUtf8()
    }
}