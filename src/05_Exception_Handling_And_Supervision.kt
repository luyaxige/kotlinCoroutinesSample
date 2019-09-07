import kotlinx.coroutines.*
import java.io.IOException

class ExceptionHandlingAndSupervision {

    val print = printer()

    companion object {
        fun run() {
            ExceptionHandlingAndSupervision().print.c { "********************" }
            ExceptionHandlingAndSupervision().exceptionPropagation()

            println("******************************")
            ExceptionHandlingAndSupervision().exceptionHandler()

            println("******************************")
            ExceptionHandlingAndSupervision().cancellationAndExceptions()

            println("******************************")
            ExceptionHandlingAndSupervision().cancellationWithExceptionHandler()

            println("******************************")
            ExceptionHandlingAndSupervision().exceptionAggregation()

            println("******************************")
            ExceptionHandlingAndSupervision().transparentCancellationAndUnwrapped()

            println("******************************")
            ExceptionHandlingAndSupervision().supervisionJob()

            println("******************************")
            ExceptionHandlingAndSupervision().supervisionScope()

            println("******************************")
            ExceptionHandlingAndSupervision().supervisionExceptions()

        }
    }

    private fun exceptionPropagation() = runBlocking {
        val job = GlobalScope.launch {
            println("Throwing exception from launch")
            throw IndexOutOfBoundsException() // Will be printed to the console by Thread.defaultUncaughtExceptionHandler
        }
        job.join()
        println("Joined failed job")
        val deferred = GlobalScope.async {
            println("Throwing exception from async")
            throw ArithmeticException() // Nothing is printed, relying on user to call await
        }
        try {
            deferred.await()
            println("Unreached")
        } catch (e: ArithmeticException) {
            println("Caught ArithmeticException")
        }
    }

    /**
     * need additional effort to understand for me
     * */
    private fun exceptionHandler() = runBlocking {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("Caught $exception")
        }
        val job = GlobalScope.launch(handler) {
            throw AssertionError()
        }
        val deferred = GlobalScope.async(handler) {
            throw ArithmeticException() // Nothing will be printed, relying on user to call deferred.await()
        }
        joinAll(job, deferred)
    }

    /**
     * cancellation is tightly bound with exceptions
     * coroutines internally use CancellationException for cancel
     * these exceptions are ignored by all handlers
     * so they should be used only as the source of additional debug information, which is obtained by catch
     *
     * when a coroutine is cancelled using Job().cancel without a cause,
     * it terminates but doesn't cancel its parent.
     * Cancelling without cause is a mechanism for parent to cancel its children without cancelling itself
     * */
    private fun cancellationAndExceptions() = runBlocking {
        val job = launch {
            val child = launch {
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    println("Child is cancelled")
                }
            }
            yield()
            println("Cancelling child")
            child.cancel()
            child.join()
            yield()
            println("Parent is not cancelled")
        }
        job.join()
    }

    /**
     * if a coroutine encounter other than CancellationException, it cancels its parent with that exception
     * this behavior cannot be overridden
     * and is used to provide stable coroutines hierarchies for structured concurrency
     * which do not depend on CoroutineExceptionHandler implementation
     * the original exception is handled by the parent when all its children terminate
     *
     * that's why CotourineExceptionHandler is always installed to a coroutine that is created in GlobalScope
     * because main coroutine will always be cancelled, make the child's coroutine handler no sense
     * */
    private fun cancellationWithExceptionHandler() = runBlocking {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("Caught $exception")
        }
        val job = GlobalScope.launch(handler) {
            launch {
                // 第一个子协程
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    withContext(NonCancellable) {
                        println("Children are cancelled, but exception is not handled until all children terminate")
                        delay(100)
                        println("The first child finished its non cancellable block")
                    }
                }
            }
            launch {
                // 第二个子协程
                delay(10)
                println("Second child throws an exception")
                throw ArithmeticException()
            }
        }
        job.join()
    }

    /**
     * when occur multiple children throw an exception
     * the general rule is "the first exception wins"
     * the first thrown exception is exposed to the handler
     * additional exceptions are suppressed
     * */
    private fun exceptionAggregation() = runBlocking {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("Caught $exception with suppressed ${exception.suppressed!!.contentToString()}")
        }
        val job = GlobalScope.launch(handler) {
            launch {
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    throw ArithmeticException()
                }
            }
            launch {
                delay(100)
                throw IOException()
            }
            delay(Long.MAX_VALUE)
        }
        job.join()
    }

    private fun transparentCancellationAndUnwrapped() = runBlocking {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("Caught original $exception")
        }
        val job = GlobalScope.launch(handler) {
            val inner = launch {
                launch {
                    launch {
                        throw IOException()
                    }
                }
            }
            try {
                inner.join()
            } catch (e: CancellationException) {
                println("Rethrowing CancellationException with original cause")
                throw e
            }
        }
        job.join()
    }

    /**
     * Supervision
     *
     * a UI component with the job defined in its scope
     * if any UI's children tasks failed, not always necessary to cancel(effectively kill) the whole UI component
     * but it UI component is destroyed(and its job is cancelled), it's necessary to fail all child jobs
     * as their results are no longer required
     *
     * or in server process that spawns several children jobs and needs to supervise their execution,
     * tracking their failures and restarting just those children jobs that had failed
     * */

    /**
     * SupervisionJob similar to a regular Job
     * with only exception that cancellation is propagated only downwards
     * */
    private fun supervisionJob() = runBlocking {
        val supervisor = SupervisorJob()
        with(CoroutineScope(coroutineContext + supervisor)) {
            // launch the first child -- its exception is ignored for this example (don't do this in practice!)
            val firstChild = launch(CoroutineExceptionHandler { _, _ -> }) {
                println("First child is failing")
                throw AssertionError("First child is cancelled")
            }
            // launch the second child
            val secondChild = launch {
                firstChild.join()
                // Cancellation of the first child is not propagated to the second child
                println("First child is cancelled: ${firstChild.isCancelled}, but second one is still active")
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    // But cancellation of the supervisor is propagated
                    println("Second child is cancelled because supervisor is cancelled")
                }
            }
            // wait until the first child fails & completes
            firstChild.join()
            println("Cancelling supervisor")
            supervisor.cancel()
            secondChild.join()
        }
    }

    /**
     * use scope concurrency supervisorScope instead of coroutineScope
     * this propagates cancellation only in one direction
     * and cancels all children only if it has failed itself
     * it also waits for all children before completion just like coroutineScope does
     * */
    private fun supervisionScope() = runBlocking {
        try {
            supervisorScope {
                launch {
                    try {
                        println("Child is sleeping")
                        delay(Long.MAX_VALUE)
                    } finally {
                        println("Child is cancelled")
                    }
                }
                // Give our child a chance to execute and print using yield
                yield()
                println("Throwing exception from scope")
                throw AssertionError()
            }
        } catch (e: AssertionError) {
            println("Caught assertion error")
        }
    }

    /**
     * every child should handle its exceptions by itself via exception handling mechanisms
     * the child's failure is not propagated to the parent
     * */
    private fun supervisionExceptions() = runBlocking {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("Caught $exception")
        }
        supervisorScope {
            launch(handler) {
                println("Child throws an exception")
                throw AssertionError()
            }
            println("Scope is completing")
        }
        println("Scope is completed")
    }
}