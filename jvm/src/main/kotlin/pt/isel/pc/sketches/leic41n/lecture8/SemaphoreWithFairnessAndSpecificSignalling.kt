package pt.isel.pc.sketches.leic41n.lecture8

import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class SemaphoreWithFairnessAndSpecificSignalling(
    maxUnits: Int,
) {

    private class AcquireRequest(
        val condition: Condition,
    )

    private var nOfUnits = maxUnits
    private val requestQueue = NodeLinkedList<AcquireRequest>()

    private val lock: Lock = ReentrantLock()

    @Throws(TimeoutException::class, InterruptedException::class)
    fun acquire(timeout: Duration): Unit = lock.withLock {
        // fast-path?
        if (requestQueue.empty && nOfUnits > 0) {
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
                val isHeadNode = requestQueue.isHeadNode(localNode)
                requestQueue.remove(localNode)
                if (isHeadNode) {
                    val newHeadNode = requestQueue.headNode
                    if (newHeadNode != null && nOfUnits > 0) {
                        newHeadNode.value.condition.signal()
                    }
                }
                throw ex
            }
            if (nOfUnits > 0 && requestQueue.isHeadNode(localNode)) {
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
        val headNode = requestQueue.headNode
        if (headNode != null) {
            headNode.value.condition.signal()
        }
    }
}