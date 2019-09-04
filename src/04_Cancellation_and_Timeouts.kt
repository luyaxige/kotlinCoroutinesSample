import kotlinx.coroutines.*

class CancellationAndTimeouts {

    private val print = printer()

    companion object {
        fun run() {
            CancellationAndTimeouts().print.c { "********************" }
            CancellationAndTimeouts().cancelCoroutineExecution()

            println("******************************")
            CancellationAndTimeouts().cooperateCancellation()

            println("******************************")
            CancellationAndTimeouts().makeComputationCancellable()

            println("******************************")
            CancellationAndTimeouts().closingResourcesWithFinally()

            println("******************************")
            CancellationAndTimeouts().noncancellableBlock()

            println("******************************")
            CancellationAndTimeouts().timeoutWithoutException()

            println("******************************")
            CancellationAndTimeouts().timeout()

        }
    }

    private fun cancelCoroutineExecution() = runBlocking {
        val job = launch {
            repeat(1000) { i ->
                print.t { "job: I'm sleeping $i ..." }
                delay(500L)
            }
        }
        delay(1300L) // delay a bit
        print.t { "main: I'm tired of waiting!" }
        job.cancel() // cancels the job
        job.join() // waits for job's completion
        print.t { "main: Now I can quit." }
    }

    private fun cooperateCancellation() = runBlocking {
        val startTime = System.currentTimeMillis()
        val job = launch(Dispatchers.Default) {
            var nextPrintTime = startTime
            var i = 0
            while (i < 5) { // computation loop, just wastes CPU
                // print a message twice a second
                if (System.currentTimeMillis() >= nextPrintTime) {
                    print.t { "job: I'm sleeping ${i++} ..." }
                    nextPrintTime += 500L
                }
            }
        }
        delay(1300L) // delay a bit
        print.t { "main: I'm tired of waiting!" }
        job.cancelAndJoin() // cancels the job and waits for its completion
        print.t { "main: Now I can quit." }
    }

    private fun makeComputationCancellable() = runBlocking {
        val startTime = System.currentTimeMillis()
        val job = launch(Dispatchers.Default) {
            var nextPrintTime = startTime
            var i = 0
            while (isActive) { // cancellable computation loop
                // print a message twice a second
                if (System.currentTimeMillis() >= nextPrintTime) {
                    print.t { "job: I'm sleeping ${i++} ..." }
                    nextPrintTime += 500L
                }
            }
        }
        delay(1300L) // delay a bit
        print.t { "main: I'm tired of waiting!" }
        job.cancelAndJoin() // cancels the job and waits for its completion
        print.t { "main: Now I can quit." }
    }

    private fun closingResourcesWithFinally() = runBlocking {
        val job = launch {
            try {
                repeat(1000) { i ->
                    print.t { "job: I'm sleeping $i ..." }
                    delay(500L)
                }
            } finally {
                print.t { "job: I'm running finally" }
            }
        }
        delay(1300L) // delay a bit
        print.t { "main: I'm tired of waiting!" }
        job.cancelAndJoin() // cancels the job and waits for its completion
        print.t { "main: Now I can quit." }
    }

    private fun noncancellableBlock() = runBlocking {
        val job = launch {
            try {
                repeat(1000) { i ->
                    print.t { "job: I'm sleeping $i ..." }
                    delay(500L)
                }
            } finally {
                withContext(NonCancellable) {
                    print.t { "job: I'm running finally" }
                    delay(1000L)
                    print.t { "job: And I've just delayed for 1 sec because I'm non-cancellable" }
                }
            }
        }
        delay(1300L) // delay a bit
        print.t { "main: I'm tired of waiting!" }
        job.cancelAndJoin() // cancels the job and waits for its completion
        print.t { "main: Now I can quit." }
    }

    private fun timeout() = runBlocking {
        withTimeout(1300L) {
            repeat(1000) { i ->
                print.t { "I'm sleeping $i ..." }
                delay(500L)
            }
        }
    }

    private fun timeoutWithoutException() = runBlocking {
        val result = withTimeoutOrNull(1300L) {
            repeat(1000) { i ->
                print.t { "I'm sleeping $i ..." }
                delay(500L)
            }
            "Done" // will get cancelled before it produces this result
        }
        print.t { "Result is $result" }
    }
}