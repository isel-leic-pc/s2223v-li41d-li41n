package pt.isel.pc.sketches.leic41n.lecture8

import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class SemaphoreWithFairnessAndKernelStyle(
    maxUnits: Int,
) {

    private class AcquireRequest(
        val condition: Condition,
        var isDone: Boolean = false,
    )

    private var nOfUnits = maxUnits
    private val requestQueue = NodeLinkedList<AcquireRequest>()

    private val lock: Lock = ReentrantLock()

    @Throws(TimeoutException::class, InterruptedException::class)
    fun acquire(timeout: Duration): Unit = lock.withLock {
        // fast-path?
        if (nOfUnits > 0) {
            nOfUnits -= 1
            return
        }
        // wait-path
        var remainingNanos = timeout.inWholeNanoseconds
        val localNode = requestQueue.enqueue(AcquireRequest(lock.newCondition()))
        while (true) {
            try {
                remainingNanos = localNode.value.condition.awaitNanos(remainingNanos)
            } catch (ex: InterruptedException) {
                if (localNode.value.isDone) {
                    Thread.currentThread().interrupt()
                    return
                }
                requestQueue.remove(localNode)
                throw ex
            }
            if (localNode.value.isDone) {
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
        val headNode = requestQueue.headNode
        if (headNode != null) {
            headNode.value.condition.signal()
            headNode.value.isDone = true
            requestQueue.remove(headNode)
        } else {
            nOfUnits += 1
        }
    }
}