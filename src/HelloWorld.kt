import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.concurrent.thread


class HelloWorld {

    fun coroutineStart() {

        GlobalScope.launch {
            // 在后台启动一个新的协程并继续
            delay(1000) // 非阻塞的等待 1 秒钟（默认时间单位是毫秒）
            println("${time.format(Date())}  World!") // 在延迟后打印输出
        }

        println("${time.format(Date())}  Hello, ") // 协程已在等待时主线程还在继续
        Thread.sleep(2000) // 阻塞主线程 2 秒钟来保证 JVM 存活
    }

    /**
     * to see the difference between thread and coroutines
     */
    fun threadStart() {
        thread {
            Thread.sleep(1000)
            println("${time.format(Date())}  World!")
        }
        println("${time.format(Date())}  Hello, ") // 协程已在等待时主线程还在继续
        Thread.sleep(2000) // 阻塞主线程 2 秒钟来保证 JVM 存活
    }

    /**
     * replace sleep with coroutine blocking builder
     */
    fun coroutineRunBlocking() {
        GlobalScope.launch {
            // 在后台启动一个新的协程并继续
            delay(1000L)
            println("${time.format(Date())}  World!")
        }
        println("${time.format(Date())}  Hello,") // 主线程中的代码会立即执行
        runBlocking {
            // 但是这个表达式阻塞了主线程
            delay(2000L)  // ……我们延迟 2 秒来保证 JVM 的存活
        }
    }

    fun extractFunctionRefactoring() = runBlocking {
        launch { doWorld() }
        println("${time.format(Date())}  Hello,")
    }

    /**
     * this is a suspend function, can only run in coroutine
     */
    suspend fun doWorld() {
        delay(1000L)
        println("${time.format(Date())}  World!")
    }

    /**
     * print 100K dot in a second with coroutine
     */
    fun printTenThousandsDots() = runBlocking {
        repeat(100_000) { // launch a lot of coroutines
            launch {
                delay(1000L)
                print(".")
            }
        }
    }

    /**
     *  coroutines like daemon threads, it won't keep thread alive
     */
    fun asDaemonThread() = runBlocking {
        GlobalScope.launch {
            repeat(1000) { i ->
                println("I'm sleeping $i ...")
                delay(500L)
            }
        }
        delay(1300L) // just quit after delay
    }

}