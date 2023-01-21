import kotlinx.coroutines.runBlocking
import platform.Foundation.NSRunLoop
import platform.Foundation.run


fun main(): Unit = runBlocking {
    runApplication()
    NSRunLoop.currentRunLoop.run()
}
