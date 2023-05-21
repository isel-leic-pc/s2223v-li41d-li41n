package pt.isel.pc.sketches.leic41d.lecture20

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class IntroExampleTests {

    fun f() {
        repeat(10) {
            Thread.sleep(100)
            // logger.info("After sleep, on thread '{}'", Thread.currentThread().id)
        }
        // logger.info("Thread done")
    }

    suspend fun g() {
        repeat(10) {
            delay(100)
            // Thread.sleep(100)
            logger.info("After delay, on thread '{}'", Thread.currentThread().id)
        }
        // logger.info("Coroutine done")
    }

    @Test
    fun `using threads`() {
        // Try with a bigger number
        val ths = List(1_000) {
            thread { f() }
        }
        logger.info("Thread creation done")

        // val th1 = thread { f() }
        // val th2 = thread { f() }
        // th1.join()
        // th2.join()
        ths.forEach { it.join() }
    }

    @Test
    fun `using coroutines`() {
        runBlocking {
            // Try with a bigger number
            repeat(1_000) {
                launch { g() }
            }
            logger.info("After last launch")
        }
        logger.info("After runBlocking")
    }

    companion object {

        val dispatcher = Executors.newFixedThreadPool(8).asCoroutineDispatcher()

        val logger: Logger = LoggerFactory.getLogger(IntroExampleTests::class.java)
    }
}