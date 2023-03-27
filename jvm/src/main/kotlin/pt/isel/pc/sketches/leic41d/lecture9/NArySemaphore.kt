package pt.isel.pc.sketches.leic41d.lecture9

import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class NArySemaphore(
    initialUnits: Int,
) {

    private class Request(
        val requestedUnits: Int,
        val condition: Condition,
        var isDone: Boolean = false,
    )

    private var availableUnits: Int = initialUnits
    private val requestQueue = NodeLinkedList<Request>()
    private val lock = ReentrantLock()

    fun acquire(requestedUnits: Int, timeout: Duration): Boolean {
        require(requestedUnits > 0) { "requestedUnits must be greater than zero" }
        lock.withLock {
            // fast-path
            if (requestQueue.empty && availableUnits >= requestedUnits) {
                availableUnits -= requestedUnits
                return true
            }
            // wait-path
            val myRequestNode = requestQueue.enqueue(
                Request(
                    requestedUnits,
                    lock.newCondition()
                )
            )
            var remainingNanos = timeout.inWholeNanoseconds
            while (true) {
                try {
                    remainingNanos = myRequestNode.value.condition.awaitNanos(remainingNanos)
                } catch (ex: InterruptedException) {
                    if (myRequestNode.value.isDone) {
                        Thread.currentThread().interrupt()
                        return true
                    }
                    requestQueue.remove(myRequestNode)
                    completeAllThatCanBeCompleted()
                    throw ex
                }
                if (myRequestNode.value.isDone) {
                    return true
                }
                if (remainingNanos <= 0) {
                    requestQueue.remove(myRequestNode)
                    completeAllThatCanBeCompleted()
                    return false
                }
            }
        }
    }

    fun release(releasedUnits: Int) {
        require(releasedUnits > 0) { "releasedUnits must be greater than zero" }
        lock.withLock {
            availableUnits += releasedUnits
            completeAllThatCanBeCompleted()
        }
    }

    private fun completeAllThatCanBeCompleted() {
        var headRequest = requestQueue.headNode
        while (headRequest != null && availableUnits >= headRequest.value.requestedUnits) {
            availableUnits -= headRequest.value.requestedUnits
            headRequest.value.isDone = true
            headRequest.value.condition.signal()
            requestQueue.remove(headRequest)
            headRequest = requestQueue.headNode
        }
    }
}