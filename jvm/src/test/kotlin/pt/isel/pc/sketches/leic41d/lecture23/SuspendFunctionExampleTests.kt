package pt.isel.pc.sketches.leic41d.lecture23

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SuspendFunctionExampleTests {

    @Test
    fun first() {
        runBlocking(Dispatchers.Unconfined) {
            logger.info("before suspendCoroutine")
            val res: String = suspendCoroutine { continuation ->
                logger.info("before resume")
                continuation.resume("hello")
                logger.info("after resume")
            }
            logger.info("after suspendCoroutine with res='{}'", res)
        }
    }

    var continuationField: Continuation<String>? = null

    @Test
    fun second() {
        suspend fun f() {
            logger.info("before suspendCoroutine")
            val res: String = suspendCoroutine { continuation ->
                logger.info("before resume")
                continuationField = continuation
                continuation.resume("hello")
            }
            logger.info("continuation: after suspendCoroutine with res='{}'", res)
        }

        suspend fun g() {
            logger.info("before call to f")
            f()
            logger.info("after call to f")
        }

        runBlocking {
            g()
            // continuationField?.resume("Here")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SuspendFunctionExampleTests::class.java)
    }
}