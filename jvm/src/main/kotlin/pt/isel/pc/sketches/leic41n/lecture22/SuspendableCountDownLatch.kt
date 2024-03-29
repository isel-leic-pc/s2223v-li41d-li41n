package pt.isel.pc.sketches.leic41n.lecture22

import org.slf4j.LoggerFactory
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SuspendableCountDownLatch(
    initialCount: Int,
) {
    private var counter = initialCount
    private val continuationList = mutableListOf<Continuation<Unit>>()
    private val lock = ReentrantLock()

    fun countdown() {
        var listToResume: List<Continuation<Unit>>? = null
        lock.withLock {
            if (counter == 0) {
                return@withLock
            }
            counter -= 1
            if (counter == 0) {
                listToResume = continuationList.toList()
            }
        }
        if (listToResume != null) {
            logger.info("Resuming continuations")
            listToResume?.forEach {
                it.resume(Unit)
            }
            logger.info("Ending resuming continuations")
        }
    }

    suspend fun await() {
        suspendCoroutine<Unit> { continuation ->
            lock.withLock {
                if (counter == 0) {
                    continuation.resume(Unit)
                } else {
                    continuationList.add(continuation)
                }
            }
        }
        logger.info("ending await")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SuspendableCountDownLatch::class.java)
    }
}