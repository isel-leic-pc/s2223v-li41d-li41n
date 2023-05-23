package pt.isel.pc.sketches.leic41n.lecture22

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SuspendFunctionsExampleTests {

    suspend fun delay1(ms: Long): String {
        logger.info("Before delay")
        delay(ms)
        logger.info("After delay")
        return "done!"
    }

    @Test
    fun `example 1`() {
        val sem = Semaphore(0)
        val nonSuspendDelay1 = ::delay1 as (Long, Continuation<String>) -> Any
        val ret = nonSuspendDelay1(
            1000,
            object : Continuation<String> {
                override fun resumeWith(result: Result<String>) {
                    logger.info("Continuation called with '{}'", result.getOrThrow())
                    sem.release()
                }

                override val context = EmptyCoroutineContext
            }
        )
        logger.info("After call to nonSuspendDelay1 with ret={}", ret)
        sem.acquire()
    }

    fun ourDelay(ms: Long, continuation: Continuation<Unit>): Any {
        scheduledExecutor.schedule({
            logger.info("scheduled executor callback, before resume")
            val interceptor = continuation.context[ContinuationInterceptor]!!
            val interceptedContinuation = interceptor.interceptContinuation(continuation)
            interceptedContinuation.resume(Unit)
            logger.info("scheduled executor callback, after resume")
        }, ms, TimeUnit.MILLISECONDS)

        return COROUTINE_SUSPENDED
    }

    @Test
    fun `example 2`() {
        val ourDelayAsSuspendFunction = ::ourDelay as suspend (Long) -> Unit
        runBlocking {
            repeat(4) {
                ourDelayAsSuspendFunction(1000)
                logger.info("after ourDelayAsSuspendFunction")
            }
        }
    }

    suspend fun finalDelay(ms: Long) {
        suspendCoroutine<Unit> { continuation ->
            scheduledExecutor.schedule({
                logger.info("scheduledExecutor callback")
                continuation.resume(Unit)
            }, ms, TimeUnit.MILLISECONDS)
        }
        logger.info("Ending finalDelay")
    }

    @Test
    fun `example 3`() {
        val ourDelayAsSuspendFunction = ::ourDelay as suspend (Long) -> Unit
        runBlocking {
            repeat(4) {
                finalDelay(1000)
                logger.info("after ourDelayAsSuspendFunction")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SuspendFunctionsExampleTests::class.java)
        private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
    }
}