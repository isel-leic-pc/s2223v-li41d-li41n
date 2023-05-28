package pt.isel.pc.sketches.leic41n.lecture23

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SuspendFunctionsExamplesTests {

    suspend fun f() {
        logger.info("Starting f")
        val res = g()
        logger.info("Ending f with res='{}'", res)
    }

    suspend fun g(): String {
        logger.info("Starting g")
        val res = suspendCoroutine<String> { continuation ->
            logger.info("Before resume")
            continuation.resume("Resuming...")
            logger.info("After resume")
        }
        logger.info("Ending g with res='{}'", res)
        return res
    }

    @Test
    fun first() {
        runBlocking() {
            logger.info("Before f")
            f()
            logger.info("After f")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SuspendFunctionsExamplesTests::class.java)
    }
}