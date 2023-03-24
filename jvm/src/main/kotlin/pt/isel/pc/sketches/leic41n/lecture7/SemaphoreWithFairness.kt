package pt.isel.pc.sketches.leic41n.lecture7

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

    @Throws(TimeoutException::class)
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
            remainingNanos = condition.awaitNanos(remainingNanos)
            if (nOfUnits > 0 && requestQueue.headValue == Thread.currentThread()) {
                nOfUnits -= 1
                requestQueue.remove(localNode)
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
        condition.signalAll()
    }
}