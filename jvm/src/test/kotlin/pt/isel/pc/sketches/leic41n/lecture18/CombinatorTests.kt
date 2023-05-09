package pt.isel.pc.sketches.leic41n.lecture18

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import pt.isel.pc.sketches.leic41d.lecture17.FutureExampleTests
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class CombinatorTests {

    @Test
    fun first() {
        val sem = Semaphore(0)
        val futures = List(10) {
            delay(Duration.ofMillis(it.toLong() * 100)) {
                it
            }
        }

        val allFuture: CompletionStage<List<Int>> = all(futures)
        allFuture.thenApplyAsync {
            logger.info("thenApply: {}", it)
            sem.release()
        }
        val executor = Executors.newSingleThreadExecutor()
        allFuture.thenApplyAsync({
            logger.info("thenApply: {}", it)
            sem.release()
        }, executor)

        sem.acquire()
    }

    @Test
    fun second() {
        val cf = CompletableFuture<String>()
        cf.complete("Hello")
        logger.info("Before thenApply")
        cf.thenApplyAsync {
            logger.info("callback: {}", it)
        }
        logger.info("After then Apply")
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