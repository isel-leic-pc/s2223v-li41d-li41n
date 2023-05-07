package pt.isel.pc.sketches.leic41d.lecture17

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class FutureExampleTests {

    @Test
    fun firstExample() {
        val f1: CompletableFuture<Boolean> = delay(60.seconds)
        val f2: CompletableFuture<String> = f1.thenApply { input -> input.toString() }
        val f3: CompletableFuture<Int> = f2.thenApply { input -> input.length }
        val f4: CompletableFuture<Boolean> = f3.thenCompose { input -> delay(input.seconds) }
        logger.info("Test ends")
    }

    companion object {
        private val delayExecutor = Executors.newSingleThreadScheduledExecutor()

        private val logger = LoggerFactory.getLogger(FutureExampleTests::class.java)

        fun delay(duration: Duration): CompletableFuture<Boolean> {
            val cf = CompletableFuture<Boolean>()
            delayExecutor.schedule(
                { cf.complete(true) },
                duration.inWholeMilliseconds,
                TimeUnit.MILLISECONDS
            )
            return cf
        }
    }
}