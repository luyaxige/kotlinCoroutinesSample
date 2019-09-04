import kotlinx.coroutines.*
import java.util.*


class Activity : CoroutineScope by CoroutineScope(Dispatchers.Default) {

    fun destroy() {
        cancel() // Extension on CoroutineScope
    }
    // to be continued ...

    // class Activity continues
    fun doSomething() {
        // launch ten coroutines for a demo, each working for a different time
        repeat(10) { i ->
            launch {
                delay((i + 1) * 200L) // variable delay 200ms, 400ms, ... etc
                println("Coroutine $i is done")
            }
        }
    }
} // class Activity ends

val threadLocal = ThreadLocal<String?>() // declare thread-local variable

class ContextAndDispatchers {

    val print = printer()

    companion object {
        fun run() {
            ContextAndDispatchers().print.c { "********************" }
            ContextAndDispatchers().coroutinesDispatcherAndThread()

            println("******************************")
            ContextAndDispatchers().dispatcherCompare()

            println("******************************")
            ContextAndDispatchers().debugCoroutinesAndThreads()

            println("******************************")
            ContextAndDispatchers().jumpingBetweenThreads()

            println("******************************")
            ContextAndDispatchers().contextJob()

            println("******************************")
            ContextAndDispatchers().childrenCoroutine()

            println("******************************")
            ContextAndDispatchers().parentalCoroutines()

            println("******************************")
            ContextAndDispatchers().nameCoroutinesForDebugging()

            println("******************************")
            ContextAndDispatchers().combiningContextElements()

            println("******************************")
            ContextAndDispatchers().startCoroutineScope()

            println("******************************")
            ContextAndDispatchers().threadLocalData()
        }
    }

    private fun coroutinesDispatcherAndThread() = runBlocking {

        /**
         * it inherits the context (and thus dispatcher) from the CoroutineScope it is being launched from.
         * In this case, it inherits the context of the main runBlocking coroutine which runs in the main thread.
         * */
        launch {
            // context of the parent, main runBlocking coroutine
            print.t { "main runBlocking      : I'm working in thread ${Thread.currentThread().name}" }
        }

        /**
         * This is a special dispatcher that also appears to run in the main thread,
         * but in fact,this one is a different mechanism, we explains later.
         * */
        launch(Dispatchers.Unconfined) {
            // not confined -- will work with main thread
            print.t { "Unconfined            : I'm working in thread ${Thread.currentThread().name}" }
        }

        /**
         * The default dispatcher that is used
         * when coroutines are launched in GlobalScope is represented by Dispatchers.
         * Default and uses a shared background pool of threads,
         * so launch(Dispatchers.Default) { ... } uses the same dispatcher as GlobalScope.launch { ... }.
         * */
        launch(Dispatchers.Default) {
            // will get dispatched to DefaultDispatcher
            print.t { "Default               : I'm working in thread ${Thread.currentThread().name}" }
        }

        /**
         * creates a new thread for the coroutine to run
         * */
        launch(newSingleThreadContext("MyOwnThread")) {
            // will get its own new thread
            print.t { "newSingleThreadContext: I'm working in thread ${Thread.currentThread().name}" }
        }
    }

    private fun dispatcherCompare() = runBlocking {
        launch(Dispatchers.Unconfined) {
            // not confined -- will work with main thread
            print.t { "Unconfined      : I'm working in thread ${Thread.currentThread().name}" }
            delay(1000)
            print.t { "Unconfined      : After delay in thread ${Thread.currentThread().name}" }
        }
        launch {
            // context of the parent, main runBlocking coroutine
            print.t { "main runBlocking: I'm working in thread ${Thread.currentThread().name}" }
            delay(2000)
            print.t { "main runBlocking: After delay in thread ${Thread.currentThread().name}" }
        }
    }

    private fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

    private fun debugCoroutinesAndThreads() = runBlocking<Unit> {
        val a = async {
            log("${time.format(Date())}  I'm computing a piece of the answer")
            6
        }
        val b = async {
            log("${time.format(Date())}  I'm computing another piece of the answer")
            7
        }
        log("${time.format(Date())}  The answer is ${a.await() * b.await()}")
    }

    /**
     * newSingleThreadContext创建了一个ThreadPoolDispatcher,同时继承与CoroutineContext
     * 两个newSingleThreadContext创建了两个ThreadPoolDispatcher对象，分别是ctx1,ctx2
     * runBlocking创建一个协程，并且使用ctx1
     * 在协程中，可以通过调用withContext来进行CoroutineContext的切换（同时也会切换Dispatcher）
     * ATTENTION:withContext会阻塞协程，等到block中的内容运行完成之后才返回，可以有返回值
     * */
    private fun jumpingBetweenThreads() {
        newSingleThreadContext("Ctx1").use { ctx1 ->
            newSingleThreadContext("Ctx2").use { ctx2 ->
                runBlocking(ctx1) {
                    log("${time.format(Date())}  Started in ctx1")
                    val result = withContext(ctx2) {
                        log("${time.format(Date())}  Working in ctx2")
                        delay(1000)
                        "ctx2 finished"
                    }
                    log("${time.format(Date())}  Back to ctx1: $result")
                }
            }
        }
    }

    private fun contextJob() = runBlocking {
        print.t { "My job is ${coroutineContext[Job]}" }
    }

    private fun childrenCoroutine() = runBlocking {
        // launch a coroutine to process some kind of incoming request
        val request = launch {
            // it spawns two other jobs, one with GlobalScope
            GlobalScope.launch {
                print.t { "job1: I run in GlobalScope and execute independently!" }
                delay(1000)
                print.t { "job1: I am not affected by cancellation of the request" }
            }
            // and the other inherits the parent context
            launch {
                delay(100)
                print.t { "job2: I am a child of the request coroutine" }
                delay(1000)
                print.t { "job2: I will not execute this line if my parent request is cancelled" }
            }
        }
        delay(500)
        request.cancel() // cancel processing of the request
        delay(1000) // delay a second to see what happens
        print.t { "main: Who has survived request cancellation?" }
    }

    /**
     * A parent coroutine always waits for completion of all its children
     * doesn't have to explicitly track all the children it launches
     * doesn't have to use Job.join to wait for them at the end
     * */
    private fun parentalCoroutines() = runBlocking {
        // launch a coroutine to process some kind of incoming request
        val request = launch {
            repeat(3) { i ->
                // launch a few children jobs
                launch {
                    delay((i + 1) * 200L) // variable delay 200ms, 400ms, 600ms
                    print.t { "Coroutine $i is done" }
                }
            }
            print.t { "request: I'm done and I don't explicitly join my children that are still active" }
        }
        request.join() // wait for completion of the request, including all its children
        print.t { "Now processing of the request is complete" }
    }

    private fun nameCoroutinesForDebugging() = runBlocking {
        log("${time.format(Date())}  Started main coroutine")
        // run two background value computations
        val v1 = async(CoroutineName("${time.format(Date())}  v1coroutine")) {
            delay(500)
            log("${time.format(Date())}  Computing v1")
            252
        }
        val v2 = async(CoroutineName("${time.format(Date())}  v2coroutine")) {
            delay(1000)
            log("${time.format(Date())}  Computing v2")
            6
        }
        log("${time.format(Date())}  The answer for v1 / v2 = ${v1.await() / v2.await()}")
    }

    /**
     * can launch a coroutine with an explicitly specified dispatcher and an explicitly specified name at the same time
     * */
    private fun combiningContextElements() = runBlocking {
        launch(Dispatchers.Default + CoroutineName("test")) {
            print.t { "I'm working in thread ${Thread.currentThread().name}" }
        }
    }

    fun startCoroutineScope() = runBlocking {
        val activity = Activity()
        activity.doSomething() // run test function
        print.t { "Launched coroutines" }
        delay(500L) // delay for half a second
        print.t { "Destroying activity!" }
        activity.destroy() // cancels all coroutines
        delay(1000) // visually confirm that they don't work
    }

    fun threadLocalData() = runBlocking<Unit> {
        threadLocal.set("${time.format(Date())}  main")
        print.t { "Pre-main, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'" }
        val job = launch(Dispatchers.Default + threadLocal.asContextElement(value = "launch")) {
            print.t { "Launch start, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'" }
            yield()
            print.t { "After yield, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'" }
        }
        job.join()
        print.t { "Post-main, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'" }
    }

}