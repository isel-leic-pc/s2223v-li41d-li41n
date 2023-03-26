package pt.isel.pc.sketches.leic41n.lecture8

import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class SemaphoreWithFairness(
    maxUnits: Int,
) {
    private var nOfUnits = maxUnits
    private val requestQueue = NodeLinkedList<Thread>()

    private val lock: Lock = ReentrantLock()

    // condition: nOfUnits > 0
    private val condition: Condition = lock.newCondition()

    @Throws(TimeoutException::class, InterruptedException::class)
    fun acquire(timeout: Duration): Unit = lock.withLock {
        // fast-path?
        if (requestQueue.empty && nOfUnits > 0) {
            nOfUnits -= 1
            return
        }
        // wait-path
        var remainingNanos = timeout.inWholeNanoseconds
        val localNode = requestQueue.enqueue(Thread.currentThread())
        while (true) {
            try {
                remainingNanos = condition.awaitNanos(remainingNanos)
            } catch (ex: InterruptedException) {
                requestQueue.remove(localNode)
                if (nOfUnits > 0 && requestQueue.notEmpty) {
                    condition.signalAll()
                }
                throw ex
            }
            if (nOfUnits > 0 && requestQueue.headValue == Thread.currentThread()) {
                nOfUnits -= 1
                requestQueue.remove(localNode)
                // FIXME signalling is required because state was changed
                return
            }
            if (remainingNanos <= 0) {
                // timeout
                requestQueue.remove(localNode)
                throw TimeoutException()
            }
        }
    }

    fun release() = lock.withLock {
        nOfUnits += 1
        // nOfUnits > 0
        if (requestQueue.notEmpty) {
            condition.signalAll()
        }
    }
}