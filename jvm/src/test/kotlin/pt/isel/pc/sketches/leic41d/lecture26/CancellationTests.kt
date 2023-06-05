package pt.isel.pc.sketches.leic41d.lecture26

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory

class CancellationTests {

    @Test
    fun `cancellation of child coroutine by parent does not cancel parent coroutine`() {
        runBlocking {
            val c1 = launch {
                try {
                    delay(1000)
                } catch (ex: CancellationException) {
                    logger.info("Caught CancellationException")
                    throw ex
                }
            }
            launch {
                try {
                    delay(1000)
                    logger.info("After delay on second child coroutine")
                } catch (ex: CancellationException) {
                    logger.info("Caught CancellationException")
                    throw ex
                }
            }
            delay(500)
            c1.cancel()
        }
    }

    @Test
    fun `cancellation of child coroutine by itself does not cancel parent coroutine`() {
        runBlocking {
            launch {
                try {
                    withTimeout(500) {
                        delay(1000)
                    }
                } catch (ex: CancellationException) {
                    logger.info("Caught CancellationException")
                    throw ex
                }
            }
            launch {
                try {
                    delay(1000)
                    logger.info("After delay on second child coroutine")
                } catch (ex: CancellationException) {
                    logger.info("Caught CancellationException")
                    throw ex
                }
            }
        }
    }

    @Test
    fun `exception other than cancellation on child coroutine does cancel parent coroutine`() {
        assertThrows<RuntimeException> {
            runBlocking {
                launch {
                    try {
                        withTimeout(500) {
                            delay(1000)
                        }
                    } catch (ex: CancellationException) {
                        logger.info("Caught CancellationException")
                        throw RuntimeException("Reacting to cancellation")
                    }
                }
                launch {
                    try {
                        delay(1000)
                        logger.info("After delay on second child coroutine")
                    } catch (ex: CancellationException) {
                        logger.info("Caught CancellationException")
                        throw ex
                    }
                }
            }
        }
    }

    @Test
    fun `using supervisorScope`() {
        runBlocking {
            supervisorScope {
                launch {
                    try {
                        withTimeout(500) {
                            delay(1000)
                        }
                    } catch (ex: CancellationException) {
                        logger.info("Caught CancellationException")
                        throw RuntimeException("Reacting to cancellation")
                    }
                }
                launch {
                    try {
                        delay(1000)
                        logger.info("After delay on second child coroutine")
                    } catch (ex: CancellationException) {
                        logger.info("Caught CancellationException")
                        throw ex
                    }
                }
            }
        }
    }

    @Test
    fun `using supervisorScope and a CoroutineExceptionHandler`() {
        runBlocking {
            val exceptionHandler = CoroutineExceptionHandler { _, exception ->
                logger.info("Unhandled exception: {}", exception.message)
            }
            supervisorScope() {
                launch(exceptionHandler) {
                    try {
                        withTimeout(500) {
                            delay(1000)
                        }
                    } catch (ex: CancellationException) {
                        logger.info("Caught CancellationException")
                        throw RuntimeException("Reacting to cancellation")
                    }
                }
                launch(exceptionHandler) {
                    try {
                        delay(1000)
                    } catch (ex: CancellationException) {
                        logger.info("Caught CancellationException")
                        throw ex
                    }
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CancellationTests::class.java)
    }
}