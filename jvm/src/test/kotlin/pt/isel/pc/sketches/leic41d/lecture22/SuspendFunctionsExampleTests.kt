package pt.isel.pc.sketches.leic41d.lecture22

import kotlinx.coroutines.Dispatchers
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

    suspend fun mydelay(ms: Long): String {
        delay(ms)
        return "hello" // call continuation with "done"
    }

    @Test
    fun `example 1`() {
        val sem = Semaphore(0)
        val suspendDelay: suspend (Long) -> String = ::mydelay
        val nonSuspendDelay = suspendDelay as (Long, Continuation<String>) -> Any

        val res = nonSuspendDelay(
            1000,
            object :
                Continuation<String> {
                override fun resumeWith(result: Result<String>) {
                    logger.info("Inside continuation: {}", result.getOrThrow())
                    sem.release()
                }

                override val context = EmptyCoroutineContext
            }
        )
        logger.info("after nonSuspendDelay with res={}", res)
        sem.acquire()
    }

    fun ourDelayCps(ms: Long, continuation: Continuation<String>): Any {
        scheduledExecutor.schedule({
            logger.info("Inside scheduled function")
            val interceptor = continuation.context[ContinuationInterceptor]!!
            val interceptedContinuation = interceptor.interceptContinuation(continuation)
            interceptedContinuation.resume("hello")
        }, ms, TimeUnit.MILLISECONDS)
        return COROUTINE_SUSPENDED
    }

    @Test
    fun `example 2`() {
        val ourDelay = ::ourDelayCps as suspend (Long) -> String
        runBlocking(Dispatchers.IO) {
            logger.info("Before ourDelay")
            val res = ourDelay(1000)
            logger.info("res = {}", res)
            val res2 = ourDelay(1000)
            logger.info("res = {}", res2)
        }
    }

    suspend fun finalDelay(ms: Long): String {
        suspendCoroutine<Unit> { continuation ->
            scheduledExecutor.schedule({
                logger.info("on schedule callback")
                continuation.resume(Unit)
            }, ms, TimeUnit.MILLISECONDS)
        }
        logger.info("In the middle")
        suspendCoroutine<Unit> { continuation ->
            scheduledExecutor.schedule({
                logger.info("on schedule callback")
                continuation.resume(Unit)
            }, ms, TimeUnit.MILLISECONDS)
        }
        return "hello"
    }

    @Test
    fun `example 3`() {
        val ourDelay = ::ourDelayCps as suspend (Long) -> String
        runBlocking {
            logger.info("Before finalDelay")
            val res = finalDelay(1000)
            logger.info("res = {}", res)
            val res2 = finalDelay(1000)
            logger.info("res = {}", res2)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SuspendFunctionsExampleTests::class.java)
        private val scheduledExecutor = Executors.newScheduledThreadPool(2)
    }
}