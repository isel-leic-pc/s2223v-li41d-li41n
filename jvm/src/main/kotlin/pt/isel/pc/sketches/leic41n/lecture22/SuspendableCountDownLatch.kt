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
        listToResume?.forEach {
            it.resume(Unit)
        }
    }

    suspend fun await() {
        lock.lock()
        if (counter == 0) {
            lock.unlock()
            return
        }
        suspendCoroutine<Unit> { continuation ->
            continuationList.add(continuation)
            lock.unlock()
        }
        logger.info("ending await")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SuspendableCountDownLatch::class.java)
    }
}