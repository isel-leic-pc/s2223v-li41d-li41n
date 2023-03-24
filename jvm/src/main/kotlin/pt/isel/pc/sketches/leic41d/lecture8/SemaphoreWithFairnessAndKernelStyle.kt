package pt.isel.pc.sketches.leic41d.lecture8

import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class SemaphoreWithFairnessAndKernelStyle(
    initialAvailableUnits: Int,
) {
    private class Request(
        val condition: Condition,
        var isDone: Boolean = false,
    )

    private var availableUnits: Int = initialAvailableUnits
    private val requestQueue = NodeLinkedList<Request>()

    private val lock = ReentrantLock()

    @Throws(InterruptedException::class)
    fun acquire(timeout: Duration): Boolean {
        lock.withLock {
            // fast-path
            if (availableUnits > 0) {
                availableUnits -= 1
                return true
            }
            // wait-path
            val localNode = requestQueue.enqueue(Request(lock.newCondition()))
            var remainingNanos: Long = timeout.inWholeNanoseconds
            while (true) {
                try {
                    remainingNanos = localNode.value.condition.awaitNanos(remainingNanos)
                } catch (e: InterruptedException) {
                    // giving-up
                    if (localNode.value.isDone) {
                        Thread.currentThread().interrupt()
                        return true
                    }
                    requestQueue.remove(localNode)
                    throw e
                }
                if (localNode.value.isDone) {
                    return true
                }
                if (remainingNanos <= 0) {
                    // giving-up
                    requestQueue.remove(localNode)
                    return false
                }
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
            availableUnits += 1
        }
    }
}