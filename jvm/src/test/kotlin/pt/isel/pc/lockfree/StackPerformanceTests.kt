package pt.isel.pc.lockfree

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import pt.isel.pc.utils.TestHelper
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class StackPerformanceTests {

    private fun nonConcurrentWork(nOfIterations: Int): Int {
        var res = 1
        repeat(nOfIterations) {
            res *= 3
        }
        return res
    }

    private fun insertAndRemove(
        stack: Stack<Int>,
        nOfThreads: Int,
        duration: Duration,
        nonConcurrentIterations: Int,
    ): Long {
        val nOfReps = 1
        val testHelper = TestHelper(duration)
        val insertions = AtomicLong()
        val removals = AtomicLong()
        testHelper.createAndStartMultiple(nOfThreads) { _, isDone ->
            while (!isDone()) {
                repeat(nOfReps) {
                    stack.push(1)
                }
                nonConcurrentWork(nonConcurrentIterations)
                insertions.addAndGet(nOfReps.toLong())
            }
        }
        testHelper.createAndStartMultiple(nOfThreads) { _, isDone ->
            while (!isDone()) {
                stack.pop() ?: continue
                nonConcurrentWork(nonConcurrentIterations)
                removals.addAndGet(1)
            }
        }
        testHelper.join()
        while (true) {
            stack.pop() ?: break
            removals.addAndGet(1)
        }
        assertEquals(removals.get(), insertions.get())
        return removals.get()
    }

    @Test
    fun `performance comparison with fixed contention`() {
        val treiberStack = TreiberStack<Int>()
        val lockBasedStack = LockBasedStack<Int>()
        val nonConcurrentIterations = 1024
        val duration = 3.seconds
        val nOfThreads = listOf(1, 2, 4, 8, 16)
        nOfThreads.forEach {
            println(
                "Lock-free,  $it, $nonConcurrentIterations, " +
                    "${insertAndRemove(treiberStack, it, duration, nonConcurrentIterations)}"
            )
            println(
                "Lock-based, $it, $nonConcurrentIterations, " +
                    "${insertAndRemove(lockBasedStack, it, duration, nonConcurrentIterations)}"
            )
        }
    }

    @Test
    fun `performance comparison with variable contention`() {
        val treiberStack = TreiberStack<Int>()
        val lockBasedStack = LockBasedStack<Int>()
        val duration = 3.seconds
        val nOfThreads = 4
        val nonConcurrentIterations = listOf(1, 64, 128, 256, 512, 1024)
        nonConcurrentIterations.forEach {
            println("Lock-free,   $nOfThreads, $it, ${insertAndRemove(treiberStack, nOfThreads, duration, it)}")
            println("Lock-based,  $nOfThreads, $it, ${insertAndRemove(lockBasedStack, nOfThreads, duration, it)}")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StackPerformanceTests::class.java)
    }
}