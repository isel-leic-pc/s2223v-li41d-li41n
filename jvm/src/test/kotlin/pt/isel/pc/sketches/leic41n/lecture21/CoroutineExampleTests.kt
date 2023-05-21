package pt.isel.pc.sketches.leic41n.lecture21

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import pt.isel.pc.sketches.leic41d.lecture21.CoroutineExampleTests
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.test.assertEquals

class CoroutineExampleTests {

    @Test
    fun `runBlocking example`() {
        // runBlocking:
        // - coroutine builder
        // - scope builder
        // launch:
        // - coroutine builder
        runBlocking {
            launch {
                launch {
                    delay(1500)
                }
                delay(500)
            }
            launch {
                delay(1000)
            }
            logger.info("After launches")
        }
        logger.info("After runBlocking")
    }

    @Test
    fun `coroutineScope example`() {
        runBlocking {
            coroutineScope {
                launch {
                    delay(500)
                    logger.info("first coroutine about to end")
                }
                launch {
                    delay(1000)
                    logger.info("second coroutine about to end")
                }
                logger.info("after launching first and second coroutine")
            }
            logger.info("after coroutineScope")
            launch {
                delay(1500)
                logger.info("third coroutine about to end")
            }
        }
    }

    suspend fun mydelay(ms: Long) = try {
        delay(ms)
    } catch (ex: CancellationException) {
        logger.info("Caught CancellationException")
        throw ex
    }

    suspend fun mydelay(name: String, ms: Long) = try {
        delay(ms)
    } catch (ex: CancellationException) {
        logger.info("CancellationException happened on $name")
        throw ex
    }

    @Test
    fun `first cancel example`() {
        runBlocking(dispatcher) {
            val c0 = launch {
                launch {
                    launch {
                        mydelay(1500)
                        logger.info("c3 ending")
                    }
                    mydelay(500)
                    logger.info("c1 ending")
                }
                launch {
                    // mydelay(1000)
                    Thread.sleep(1000)
                    logger.info("c2 ending")
                }
            }
            logger.info("after launch")
            delay(250)
            c0.cancel()
        }
        logger.info("after runBlocking")
    }

    @Test
    fun `second cancel example`() {
        runBlocking(dispatcher) {
            val c0 = async {
                launch {
                    Thread.sleep(1000)
                }
                delay(250)
                logger.info("c0 computation is ending, everything went fine")
                "the return of the computation"
            }
            delay(500)
            c0.cancel()
            logger.info("state: {}", stateString(c0))
            try {
                val value = c0.await()
                logger.info("await returned: {}", value)
                logger.info("state: {}", stateString(c0))
            } catch (ex: CancellationException) {
                logger.info("Caught CancellationException")
            }
        }
    }

    @Test
    fun `error and cancellation example`() {
        val ex = assertThrows<IOException> {
            runBlocking(dispatcher) {
                launch {
                    launch {
                        mydelay("c3", 500)
                    }
                    mydelay("c1", 250)
                    throw IOException("Bang!")
                }
                launch {
                    // mydelay("c2", 1000)
                    Thread.sleep(1000)
                    throw IOException("Another Bang!")
                }
                mydelay("c0", 750)
            }
        }
        assertEquals("Bang!", ex.message)
        assertEquals("Another Bang!", ex.suppressed[0].message)
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

    fun stateString(job: Job) =
        "isActive:${job.isActive}, isCancelled: ${job.isCancelled}, isCompleted: ${job.isCompleted}"

    companion object {
        private val logger = LoggerFactory.getLogger(CoroutineExampleTests::class.java)
        private val dispatcher = Executors.newFixedThreadPool(3).asCoroutineDispatcher()
        private val dispatcher2 = Executors.newFixedThreadPool(3).asCoroutineDispatcher()
    }
}