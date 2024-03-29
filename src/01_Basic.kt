import kotlinx.coroutines.*
import kotlin.concurrent.thread


class HelloWorld {

    val print = printer()

    companion object {
        fun run() {
            HelloWorld().print.c { "********************" }
            HelloWorld().coroutineStart()

            println("******************************")
            HelloWorld().threadStart()

            println("******************************")
            HelloWorld().coroutineRunBlocking()

            println("******************************")
            HelloWorld().coroutinesSynchronize()

            println("******************************")
            HelloWorld().structoreConcurrency()

            println("******************************")
            HelloWorld().coroutinesScopeBuilder()

            println("******************************")
            HelloWorld().extractfunctionRefactoring()

            println("******************************")
            HelloWorld().printTenThousandsDots()

            println("******************************")
            HelloWorld().asDaemonThread()
        }
    }

    /**
     * launch a basic coroutine and a thread
     * */
    fun coroutineStart() {

        GlobalScope.launch {
            // 在后台启动一个新的协程并继续
            delay(1000) // 非阻塞的等待 1 秒钟（默认时间单位是毫秒）
            print.t { "World!" } // 在延迟后打印输出
        }

        print.t { "Hello, " } // 协程已在等待时主线程还在继续
        Thread.sleep(2000) // 阻塞主线程 2 秒钟来保证 JVM 存活
    }

    /**
     * to see the difference between thread and coroutines
     */
    private fun threadStart() {
        thread {
            Thread.sleep(1000)
            print.t { "World!" }
        }
        print.t { "Hello, " } // 协程已在等待时主线程还在继续
        Thread.sleep(2000) // 阻塞主线程 2 秒钟来保证 JVM 存活
    }

    /**
     * replace sleep with coroutine blocking builder
     */
    private fun coroutineRunBlocking() {
        GlobalScope.launch {
            // 在后台启动一个新的协程并继续
            delay(1000L)
            print.t { "World!" }
        }
        print.t { "Hello," } // 主线程中的代码会立即执行
        runBlocking {
            // 但是这个表达式阻塞了主线程
            delay(2000L)  // ……我们延迟 2 秒来保证 JVM 的存活
        }
    }

    /**
     * use non-blocking mothod finishing coroutines (delay and print World)
     * runBlocking<Unit> { ... } works as an adaptor that is used to start the top-level main coroutine.
     * We explicitly specify its Unit return type
     * because a well-formed main private function in Kotlin has to return <Unit>
     */
    private fun coroutinesSynchronize() = runBlocking<Unit> {
        val job = GlobalScope.launch {
            // 启动一个新协程并保持对这个作业的引用
            delay(1000L)
            print.t { "World!" }
        }
        print.t { "Hello," }
        job.join() // 等待直到子协程执行结束
    }

    /**
     *  turn the private function into a coroutine using runBlocking coroutine builder.
     *  Every coroutine builder, including runBlocking, adds an instance of <CoroutineScope> to the scope of its code block.
     *  We can launch coroutines in this scope without having to join them explicitly,
     *  because an outer coroutine (runBlocking in our example) does not complete
     *  until all the coroutines launched in its scope complete.
     * */
    private fun structoreConcurrency() = runBlocking {
        launch {
            // launch a new coroutine in the scope of runBlocking
            delay(1000L)
            print.t { "World!" }
        }
        print.t { "Hello," }
    }

    /**
     * declare our own scope using coroutineScope builder
     * create a <CoroutineScope> and does not complete until all launched children complete
     * <CoroutineScope> is non-blocking, (<runBlocking> is a blocks method)
     * <CoroutineScope> does not block the current thread while waiting for all children to complete
     * */
    private fun coroutinesScopeBuilder() = runBlocking {
        launch {
            delay(200L)
            print.t { "Task from runBlocking" }
        }

        coroutineScope {
            // Creates a coroutine scope
            launch {
                delay(500L)
                print.t { "Task from nested launch" }
            }

            delay(100L)
            print.t { "Task from coroutine scope" } // This line will be printed before the nested launch
        }

        print.t { "Coroutine scope is over" } // This line is not printed until the nested launch completes
    }

    /**
     * extract the coroutine task into suspending function
     * */
    private fun extractfunctionRefactoring() = runBlocking {
        launch { doWorld() }
        print.t { "Hello," }
    }

    /**
     * this is a suspend private function, can only run in coroutine
     */
    private suspend fun doWorld() {
        delay(1000L)
        print.t { "World!" }
    }

    /**
     * print 100K dot in a second with coroutine
     */
    private fun printTenThousandsDots() = runBlocking {
        repeat(100_000) {
            // launch a lot of coroutines
            launch {
                delay(1000L)
                print(".")
            }
        }
    }

    /**
     *  coroutines like daemon threads, it won't keep thread alive
     */
    private fun asDaemonThread() = runBlocking {
        GlobalScope.launch {
            repeat(1000) { i ->
                print.t { "I'm sleeping $i ..." }
                delay(1000L)
            }
        }
        delay(2000L) // just quit after delay
    }

}