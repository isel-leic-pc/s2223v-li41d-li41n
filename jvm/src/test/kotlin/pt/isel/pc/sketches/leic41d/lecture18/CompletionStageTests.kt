package pt.isel.pc.sketches.leic41d.lecture18

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import pt.isel.pc.sketches.leic41d.lecture17.FutureExampleTests
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompletionStageTests {

    @Test
    fun third() {
        val sem = Semaphore(0)
        val futures = (1L..10L).map {
            delay(Duration.ofMillis(it * 100)) {
                true
            }
        }
        val allFuture = all(futures)
        allFuture.thenApplyAsync {
            logger.info("On thenApply")
            sem.release()
        }
        sem.acquire()
    }

    @Test
    fun first() {
        val start = Instant.now()
        val futures = (1L..10L).map {
            delay(Duration.ofMillis(it * 100)) {
                true
            }
        }
        val allFuture = all(futures)
        val res = allFuture.toCompletableFuture().get()
        val delta = Duration.between(start, Instant.now())
        assertTrue(delta.toMillis() >= 10 * 100)
        assertTrue(res.all { it })
    }

    @Test
    fun second() {
        val start = Instant.now()
        val futures = (1L..10L).map {
            delay(Duration.ofMillis(it * 100)) {
                true
            }
        }
        val errorFuture = delay<Boolean>(Duration.ofMillis(5 * 100)) {
            throw RuntimeException("Expected error")
        }
        val allFuture = all(futures + errorFuture)
        val res = assertThrows<ExecutionException> { allFuture.toCompletableFuture().get() }
        val delta = Duration.between(start, Instant.now())
        assertTrue(delta.toMillis() >= 5 * 100)
        assertEquals("Expected error", res.cause?.message)
    }

    companion object {
        private val delayExecutor = Executors.newSingleThreadScheduledExecutor()

        private val logger = LoggerFactory.getLogger(FutureExampleTests::class.java)

        fun <T> delay(duration: Duration, supplier: () -> T): CompletableFuture<T> {
            val cf = CompletableFuture<T>()
            delayExecutor.schedule(
                {
                    try {
                        cf.complete(supplier())
                    } catch (ex: Throwable) {
                        cf.completeExceptionally(ex)
                    }
                },
                duration.toMillis(),
                TimeUnit.MILLISECONDS
            )
            return cf
        }
    }
}