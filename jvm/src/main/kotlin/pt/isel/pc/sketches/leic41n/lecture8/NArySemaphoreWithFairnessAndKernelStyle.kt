package pt.isel.pc.sketches.leic41n.lecture8

import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class NArySemaphoreWithFairnessAndKernelStyle(
    maxUnits: Int,
) {

    private class AcquireRequest(
        val condition: Condition,
        val requestedUnits: Int,
        var isDone: Boolean = false,
    )

    private var nOfUnits = maxUnits
    private val requestQueue = NodeLinkedList<AcquireRequest>()
    private val lock: Lock = ReentrantLock()

    @Throws(TimeoutException::class, InterruptedException::class)
    fun acquire(requestedUnits: Int, timeout: Duration): Unit = lock.withLock {
        require(requestedUnits > 0) { "requestedUnits must be greater than zero." }
        // fast-path?
        if (requestQueue.empty && nOfUnits >= requestedUnits) {
            nOfUnits -= requestedUnits
            return
        }
        // wait-path
        var remainingNanos = timeout.inWholeNanoseconds
        val localNode = requestQueue.enqueue(
            AcquireRequest(
                lock.newCondition(),
                requestedUnits
            )
        )
        while (true) {
            try {
                remainingNanos = localNode.value.condition.awaitNanos(remainingNanos)
            } catch (ex: InterruptedException) {
                if (localNode.value.isDone) {
                    Thread.currentThread().interrupt()
                    return
                }
                requestQueue.remove(localNode)
                completeAllThatCanBeCompleted()
                throw ex
            }
            if (localNode.value.isDone) {
                return
            }
            if (remainingNanos <= 0) {
                // timeout
                requestQueue.remove(localNode)
                completeAllThatCanBeCompleted()
                throw TimeoutException()
            }
        }
    }

    fun release(releasedUnits: Int) = lock.withLock {
        require(releasedUnits > 0) { "releasedUnits must be greater than zero." }
        nOfUnits += releasedUnits
        completeAllThatCanBeCompleted()
    }

    private fun completeAllThatCanBeCompleted() {
        var headNode = requestQueue.headNode
        while (headNode != null && nOfUnits >= headNode.value.requestedUnits) {
            nOfUnits -= headNode.value.requestedUnits
            headNode.value.isDone = true
            headNode.value.condition.signal()
            requestQueue.remove(headNode)
            headNode = requestQueue.headNode
        }
    }
}