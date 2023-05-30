package pt.isel.pc.sketches.leic41d.lecture24

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CancellationExampleTests {

    @Test
    fun first() {
        assertThrows<CancellationException> {
            runBlocking {
                launch {
                    nonCancellableDelay(1000)
                    logger.info("isCancelled={}", this.coroutineContext.job.isCancelled)
                    logger.info("After nonCancellableDelay")
                }
                delay(500)
                logger.info("Cancelling parent coroutine")
                cancel()
            }
        }
    }

    @Test
    fun second() {
        assertThrows<CancellationException> {
            runBlocking {
                launch {
                    try {
                        cancellableDelay(1000)
                        logger.info("isCancelled={}", this.coroutineContext.job.isCancelled)
                        logger.info("After cancellableDelay")
                    } catch (ex: CancellationException) {
                        logger.info("On catch of CancellationException")
                    }
                }
                delay(500)
                logger.info("Cancelling parent coroutine")
                cancel()
            }
        }
        Thread.sleep(1000)
    }

    @Test
    fun third() {
        runBlocking {
            launch {
                logger.info("Before withTimeoutOrNull")
                withTimeoutOrNull(500) {
                    try {
                        logger.info("Inside withTimeoutOrNull")
                        cancellableDelay(250)
                        logger.info("isCancelled={}", this.coroutineContext.job.isCancelled)
                        logger.info("After cancellableDelay")
                    } catch (ex: CancellationException) {
                        logger.info("isCancelled: {}", coroutineContext.job.isCancelled)
                        logger.info("On catch of CancellationException")
                    }
                }
                logger.info("isCancelled: {}", coroutineContext.job.isCancelled)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CancellationExampleTests::class.java)
        private val scheduler = Executors.newScheduledThreadPool(1)

        suspend fun nonCancellableDelay(ms: Long) {
            suspendCoroutine<Unit> { continuation ->
                scheduler.schedule({
                    continuation.resume(Unit)
                }, ms, TimeUnit.MILLISECONDS)
            }
        }

        suspend fun cancellableDelay(ms: Long) {
            suspendCancellableCoroutine<Unit> { continuation ->
                val future = scheduler.schedule({
                    logger.info("Calling continuation on scheduled callback")
                    continuation.resume(Unit)
                    logger.info("After calling continuation on scheduled callback")
                }, ms, TimeUnit.MILLISECONDS)
                continuation.invokeOnCancellation {
                    future.cancel(false)
                }
            }
        }
    }
}