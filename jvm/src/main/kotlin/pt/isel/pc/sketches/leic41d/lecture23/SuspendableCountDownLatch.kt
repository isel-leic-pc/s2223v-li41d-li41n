package pt.isel.pc.sketches.leic41d.lecture23

import org.slf4j.LoggerFactory
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SuspendableCountDownLatch(
    initialCount: Int,
) {
    private var count: Int = initialCount
    private var continuationList = mutableListOf<Continuation<Unit>>()
    private val lock = ReentrantLock()

    fun countDown() {
        var continuationListToResume: List<Continuation<Unit>>? = null
        lock.withLock {
            if (count == 0) {
                return@withLock
            }
            count -= 1
            if (count == 0) {
                continuationListToResume = continuationList.toList()
                continuationList.clear()
            }
        }
        continuationListToResume?.forEach {
            it.resume(Unit)
        }
    }

    suspend fun await() {
        suspendCoroutine<Unit> { continuation ->
            lock.withLock {
                if (count == 0) {
                    continuation.resume(Unit)
                } else {
                    continuationList.add(continuation)
                }
            }
        }
        logger.info("await is returning")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SuspendableCountDownLatch::class.java)
    }
}