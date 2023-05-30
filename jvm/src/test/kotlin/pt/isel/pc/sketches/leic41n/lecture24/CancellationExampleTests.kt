package pt.isel.pc.sketches.leic41n.lecture24

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
                    logger.info("After nonCancellableDelay, isCancelled={}", coroutineContext.job.isCancelled)
                }
                delay(500)
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
                        logger.info("After cancellableDelay, isCancelled={}", coroutineContext.job.isCancelled)
                    } catch (ex: CancellationException) {
                        logger.info("Caught CancellationException, isCancelled={}", coroutineContext.job.isCancelled)
                    }
                }
                delay(500)
                cancel()
                logger.info("isCancelled =  {}", coroutineContext.job.isCancelled)
                withContext(NonCancellable) {
                    logger.info("isCancelled =  {}", coroutineContext.job.isCancelled)
                    delay(1000)
                }
            }
        }
        logger.info("After runBlocking")
    }

    @Test
    fun third() {
        runBlocking {
            launch {
                try {
                    withTimeoutOrNull(750) {
                        try {
                            delay(1000)
                            logger.info("After delay, isCancelled={}", coroutineContext.job.isCancelled)
                        } catch (ex: CancellationException) {
                            logger.info(
                                "Caught CancellationException, isCancelled={}",
                                coroutineContext.job.isCancelled
                            )
                        }
                    }
                    logger.info("After withTimeout, isCancelled={}", coroutineContext.job.isCancelled)
                } catch (ex: CancellationException) {
                    logger.info(
                        "Caught CancellationException, isCancelled={}",
                        coroutineContext.job.isCancelled
                    )
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CancellationExampleTests::class.java)
        private val scheduler = Executors.newScheduledThreadPool(1)
    }

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
                logger.info("On scheduled callback, before resuming continuation")
                continuation.resume(Unit)
                logger.info("On scheduled callback, after resuming continuation")
            }, ms, TimeUnit.MILLISECONDS)
            continuation.invokeOnCancellation {
                future.cancel(false)
            }
        }
    }
}