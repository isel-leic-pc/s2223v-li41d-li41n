package pt.isel.pc.sketches.leic41n.lecture17

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class CompletionStageExamplesTests {

    @Test
    fun first() {
        val countDownLatch = CountDownLatch(1)
        val cs1: CompletionStage<Boolean> = delay(3.seconds)
        val cs2: CompletionStage<String> = cs1.thenApply { input -> input.toString() }
        val cs3: CompletionStage<String> = cs2.thenApply { input -> throw RuntimeException("Bang!") }
        val cs4: CompletionStage<String> = cs3.catch { input -> input.message ?: "nothing" }
        cs4.handle { t, e ->
            if (t != null) {
                logger.info("Success: {}", t)
            } else {
                logger.info("Error: {}", e.message)
            }
            countDownLatch.countDown()
        }
        logger.info("test is ending")
        countDownLatch.await()
    }

    companion object {

        private val executor = Executors.newSingleThreadScheduledExecutor()

        fun delay(duration: Duration): CompletionStage<Boolean> {
            val cf = CompletableFuture<Boolean>()
            executor.schedule(
                { cf.complete(true) },
                duration.inWholeMilliseconds,
                TimeUnit.MILLISECONDS
            )
            return cf
        }

        private val logger = LoggerFactory.getLogger(CompletionStageExamplesTests::class.java)
    }
}