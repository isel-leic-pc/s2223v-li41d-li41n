package pt.isel.pc.sketches.leic41d.lecture7

import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class SemaphoreWithFairness(
    initialAvailableUnits: Int,
) {

    private var availableUnits: Int = initialAvailableUnits
    private val queue = NodeLinkedList<Thread>()

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    fun acquire(timeout: Duration): Boolean {
        lock.withLock {
            // fast-path
            if (queue.empty && availableUnits > 0) {
                availableUnits -= 1
                return true
            }
            // wait-path
            val localNode = queue.enqueue(Thread.currentThread())
            var remainingNanos: Long = timeout.inWholeNanoseconds
            while (true) {
                // FIXME this version is still missing interrupt handling, which was only presented on lecture 8
                remainingNanos = condition.awaitNanos(remainingNanos)
                if (availableUnits > 0 && queue.headValue == Thread.currentThread()) {
                    queue.remove(localNode)
                    availableUnits -= 1
                    // FIXME signalling is required because state was changed
                    return true
                }
                if (remainingNanos <= 0) {
                    queue.remove(localNode)
                    return false
                }
            }
        }
    }

    fun release() = lock.withLock {
        availableUnits += 1
        if (queue.notEmpty) {
            condition.signalAll()
        }
    }
}