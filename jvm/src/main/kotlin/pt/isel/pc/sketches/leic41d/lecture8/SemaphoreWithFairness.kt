package pt.isel.pc.sketches.leic41d.lecture8

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

    @Throws(InterruptedException::class)
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
                try {
                    remainingNanos = condition.awaitNanos(remainingNanos)
                } catch (e: InterruptedException) {
                    // giving-up
                    if (availableUnits > 0 && queue.headValue == Thread.currentThread()) {
                        condition.signalAll()
                    }
                    queue.remove(localNode)
                    throw e
                }
                if (availableUnits > 0 && queue.headValue == Thread.currentThread()) {
                    queue.remove(localNode)
                    availableUnits -= 1
                    return true
                }
                if (remainingNanos <= 0) {
                    // giving-up
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