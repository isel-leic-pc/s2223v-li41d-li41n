package pt.isel.pc.sketches.leic41n.lecture9

import org.slf4j.LoggerFactory
import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SimpleThreadPool(
    private val maxThreads: Int,
) {
    private var nOfThreads = 0
    private val requestQueue = NodeLinkedList<Runnable>()
    private val lock = ReentrantLock()

    fun execute(runnable: Runnable) = lock.withLock {
        if (nOfThreads < maxThreads) {
            nOfThreads += 1
            Thread {
                // does not hold the lock
                threadLoop(runnable)
            }.also {
                it.start()
            }
        } else {
            requestQueue.enqueue(runnable)
        }
    }

    private fun getNextWorkItem(): Runnable? = lock.withLock {
        if (requestQueue.empty) {
            nOfThreads -= 1
            return null // informs the worker thread to end
        } else {
            return requestQueue.pull().value
        }
    }

    // Does not hold the lock
    private fun threadLoop(firstRunnable: Runnable) {
        safeRun(firstRunnable)
        while (true) {
            val nextRunnable = getNextWorkItem()
            if (nextRunnable == null) {
                // threads ends
                return
            }
            safeRun(nextRunnable)
        }
    }

    private fun safeRun(runnable: Runnable) = try {
        runnable.run()
    } catch (ex: Throwable) {
        logger.warn("Exception when running runnable: '{}', ignoring it", ex.message)
        // continues
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SimpleThreadPool::class.java)
    }
}