package pt.isel.pc.sketches.leic41d.lecture8

import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class SemaphoreWithFairnessAndSpecificSignalling(
    initialAvailableUnits: Int,
) {
    private class Request(
        val condition: Condition,
    )

    private var availableUnits: Int = initialAvailableUnits
    private val requestQueue = NodeLinkedList<Request>()

    private val lock = ReentrantLock()

    @Throws(InterruptedException::class)
    fun acquire(timeout: Duration): Boolean {
        lock.withLock {
            // fast-path
            if (requestQueue.empty && availableUnits > 0) {
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
                    val isHeadNode = requestQueue.isHeadNode(localNode)
                    requestQueue.remove(localNode)
                    if (isHeadNode) {
                        val headNode = requestQueue.headNode
                        if (availableUnits > 0 && headNode != null) {
                            headNode.value.condition.signal()
                        }
                    }
                    throw e
                }
                if (availableUnits > 0 && requestQueue.isHeadNode(localNode)) {
                    requestQueue.remove(localNode)
                    availableUnits -= 1
                    // FIXME signalling is required because state was changed
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
        availableUnits += 1
        val headNode = requestQueue.headNode
        if (headNode != null) {
            headNode.value.condition.signal()
        }
    }
}