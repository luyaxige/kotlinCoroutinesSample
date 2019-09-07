import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

class ComposingSuspndingFunctions {

    val print = printer()

    companion object {
        fun run() {
            ContextAndDispatchers().print.c { "********************" }
            ComposingSuspndingFunctions().sequentialDefault()

            println("******************************")
            ComposingSuspndingFunctions().concurrentUseAsync()

            println("******************************")
            ComposingSuspndingFunctions().lazyStartAsync()

            println("******************************")
            ComposingSuspndingFunctions().asyncStyleFunctions()

            println("******************************")
            ComposingSuspndingFunctions().structureConcurrencyWithAsync()

            println("******************************")
            ComposingSuspndingFunctions().cancellation()
        }

    }

    private suspend fun doSomethingUsefulOne(): Int {
        delay(1000L) // pretend we are doing something useful here
        return 13
    }

    private suspend fun doSomethingUsefulTwo(): Int {
        delay(1000L) // pretend we are doing something useful here, too
        return 29
    }

    fun sequentialDefault() = runBlocking {
        val time = measureTimeMillis {
            val one = doSomethingUsefulOne()
            val two = doSomethingUsefulTwo()
            println("The answer is ${one + two}")
        }
        println("Completed in $time ms")
    }

    /**
     * the async will return a Deffered, -- a light-weight non-blocking future
     * that represents a promise to provide a result later
     * use .await() function on a deffered value to get its eventually result
     * */
    fun concurrentUseAsync() = runBlocking {
        val time = measureTimeMillis {
            val one = async { doSomethingUsefulOne() }
            val two = async { doSomethingUsefulTwo() }
            println("The answer is ${one.await() + two.await()}")
        }
        println("Completed in $time ms")
    }

    /**
     * Optionally, async can be made lazy start
     * by set start paramter to CoroutineStart.LAZY
     * in this mode it only starts the coroutine when its result is required by await or start function is invoked*/
    fun lazyStartAsync() = runBlocking {
        val time = measureTimeMillis {
            val one = async(start = CoroutineStart.LAZY) { doSomethingUsefulOne() }
            val two = async(start = CoroutineStart.LAZY) { doSomethingUsefulTwo() }
            one.start() // start the first one
            two.start() // start the second one
            println("The answer is ${one.await() + two.await()}")
        }
        println("Completed in $time ms")
    }

    /**
     * async style functions are not suspending functions, so they can be used from anywhere
     * however their use always implies asynchrounous(here means concurrent) execution of their action with the invoking code
     * but this style is not recommend, for there will be logic error between 'Structure Concurrency With Async'
     * */
    // The result type of somethingUsefulOneAsync is Deferred<Int>
    private fun somethingUsefulOneAsync() = GlobalScope.async {
        doSomethingUsefulOne()
    }

    // The result type of somethingUsefulTwoAsync is Deferred<Int>
    private fun somethingUsefulTwoAsync() = GlobalScope.async {
        doSomethingUsefulTwo()
    }

    fun asyncStyleFunctions() {
        val time = measureTimeMillis {
            val one = somethingUsefulOneAsync()
            val two = somethingUsefulTwoAsync()
            runBlocking {
                println("The answer is ${one.await() + two.await()}")
            }
        }
        println("Completed in $time ms")
    }

    suspend fun concurrentSum(): Int = coroutineScope {
        val one = async { doSomethingUsefulOne() }
        val two = async { doSomethingUsefulTwo() }
        one.await() + two.await()
    }

    fun structureConcurrencyWithAsync() = runBlocking {
        val time = measureTimeMillis {
            println("The answer is ${concurrentSum()}")
        }
        println("Completed in $time ms")
    }

    fun cancellation() = runBlocking<Unit> {
        try {
            failedConcurrentSum()
        } catch (e: ArithmeticException) {
            print.t { "Computation failed with ArithmeticException" }
        }
    }

    private suspend fun failedConcurrentSum(): Int = coroutineScope {
        val one = async<Int> {
            try {
                delay(Long.MAX_VALUE) // Emulates very long computation
                42
            } finally {
                print.t { "First child was cancelled" }
            }
        }
        val two = async<Int> {
            print.t { "Second child throws an exception" }
            throw ArithmeticException()
        }
        one.await() + two.await()
    }
}