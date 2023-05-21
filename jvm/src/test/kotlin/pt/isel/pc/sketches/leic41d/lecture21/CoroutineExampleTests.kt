package pt.isel.pc.sketches.leic41d.lecture21

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.test.assertTrue

class CoroutineExampleTests {

    @Test
    fun `runBlocking example`() {
        logger.info("Before runBlocking")
        runBlocking {
            launch {
                launch {
                    delay(3000)
                }
                delay(1000)
            }
            launch {
                delay(2000)
            }
            logger.info("after launches")
        }
        logger.info("After runBlocking")
    }

    @Test
    fun `coroutineScope example`() {
        runBlocking {
            coroutineScope {
                delay(500)
                launch {
                    delay(1000)
                }
                launch {
                    delay(2000)
                }
            }
            logger.info("Both coroutines ended")
            launch {
                delay(500)
            }
        }
        logger.info("After runBlocking")
    }

    @Test
    fun `cancellation example`() {
        runBlocking(Dispatchers.IO) {
            val job = launch {
                // Thread.sleep(1000)
                try {
                    delay(1000)
                } catch (ex: CancellationException) {
                    logger.info("CancellationException", ex)
                }
            }
            launch {
                delay(500)
                job.cancel()
            }
            logger.info("After launch")
            while (!job.isCompleted) {
                delay(100)
                logger.info(
                    "isActive={}, isCancelled={}, isCompleted={}",
                    job.isActive,
                    job.isCancelled,
                    job.isCompleted
                )
            }
        }
    }

    @Test
    fun `cancellation example 2`() {
        assertThrows<CancellationException> {
            runBlocking {
                launch {
                    // Thread.sleep(1000)
                    try {
                        delay(1000)
                    } catch (ex: CancellationException) {
                        logger.info("CancellationException", ex)
                    }
                }
                launch {
                    // Thread.sleep(1000)
                    try {
                        Thread.sleep(1000)
                    } catch (ex: CancellationException) {
                        logger.info("CancellationException", ex)
                    }
                }
                delay(500)
                this.cancel()
            }
        }
    }

    @Test
    fun `cancellation example 3`() {
        assertThrows<IOException> {
            runBlocking {
                launch {
                    delay(500)
                    throw IOException("Bang!")
                }
                launch {
                    // Thread.sleep(1000)
                    launch {
                        try {
                            delay(1000)
                        } catch (ex: CancellationException) {
                            logger.info("CancellationException on first grandchild", ex)
                        }
                    }
                    try {
                        delay(1000)
                    } catch (ex: CancellationException) {
                        logger.info("CancellationException on second child", ex)
                    }
                }
                try {
                    delay(1000)
                } catch (ex: CancellationException) {
                    logger.info("CancellationException on parent", ex)
                }
            }
        }
    }

    @Test
    fun `cancellation example 4`() {
        assertThrows<IOException> {
            runBlocking {
                launch {
                    launch {
                        delay(500)
                        throw IOException("Bang!")
                    }
                    try {
                        delay(1000)
                    } catch (ex: CancellationException) {
                        logger.info("CancellationException on child", ex)
                    }
                }
                try {
                    delay(1000)
                } catch (ex: CancellationException) {
                    logger.info("CancellationException on parent", ex)
                }
            }
        }
    }

    @Test
    fun `runBlocking example with exceptions`() {
        val block: suspend (CoroutineScope.() -> Unit) = {
            // Notice this block has a `this` as a CoroutineScope
            // Note also that this block is already running on a coroutine (e.g. try changing the dispatcher)
            logger.info("Started lambda passed in to runBlocking")
            launch {
                logger.info("Nested coroutine starting")
                delay(1000)
                logger.info("Nested coroutine ending")
                throw IOException("First bang!")
            }
            launch {
                logger.info("Nested coroutine starting")
                // delay(1500)
                Thread.sleep(1500)
                logger.info("Nested coroutine ending")
                throw IOException("Second bang!")
            }
            logger.info("Ending lambda passed in to runBlocking")
        }
        val exception = assertThrows<IOException> { runBlocking(Dispatchers.IO, block) }
        assertTrue(exception.message == "First bang!")
        assertTrue(exception.suppressedExceptions[0].message == "Second bang!")
        logger.info("runBlocking ending")
    }

    @Test
    fun `context example`() {
        runBlocking(dispatcher) {
            logger.info("before delay")
            delay(500)
            logger.info("after delay")
            withContext(dispatcher2) {
                logger.info("before delay")
                delay(500)
                logger.info("after delay")
            }
            logger.info("before delay")
            delay(500)
            logger.info("after delay")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CoroutineExampleTests::class.java)
        private val dispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
        private val dispatcher2 = Executors.newFixedThreadPool(2).asCoroutineDispatcher()

        private fun showState(name: String, job: Job?) {
            if (job != null) {
                println("$name: active: ${job.isActive}, cancelled: ${job.isCancelled}, completed: ${job.isCompleted}")
            }
        }
    }
}