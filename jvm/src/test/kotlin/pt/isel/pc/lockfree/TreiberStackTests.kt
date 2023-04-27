package pt.isel.pc.lockfree

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import pt.isel.pc.utils.TestHelper
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds

class TreiberStackTests {

    @Test
    fun `inserts match removes`() {
        val nOfProducers = 3
        val nOfConsumers = 4
        val testHelper = TestHelper(5.seconds)
        val stack = TreiberStack<Long>()
        val insertionAccumulator = AtomicLong()
        val removalAccumulator = AtomicLong()

        testHelper.createAndStartMultiple(nOfProducers) { ix, isDone ->
            val value = (ix + 1).toLong()
            while (!isDone()) {
                stack.push(value)
                insertionAccumulator.addAndGet(value)
            }
        }

        testHelper.createAndStartMultiple(nOfConsumers) { _, isDone ->
            while (!isDone()) {
                val v = stack.pop()
                if (v != null) {
                    removalAccumulator.addAndGet(v)
                }
            }
        }
        testHelper.join()
        while (true) {
            val v = stack.pop() ?: break
            removalAccumulator.addAndGet(v)
        }
        assertEquals(removalAccumulator.get(), insertionAccumulator.get())
    }

    @Test
    @Ignore
    fun `Treiber stack with ABA`() {
        val nOfThreads = 3
        val testHelper = TestHelper(5.seconds)
        val stack = TreiberStackWithABA<Long>()
        val insertions = AtomicLong()
        val removals = AtomicLong()

        testHelper.createAndStartMultiple(nOfThreads) { ix, isDone ->
            var node: TreiberStackWithABA.Node<Long>? = TreiberStackWithABA.Node(ix.toLong())
            while (!isDone()) {
                if (node != null) {
                    stack.push(node)
                    insertions.addAndGet(node.value)
                }
                node = stack.pop()
                if (node != null) {
                    removals.addAndGet(node.value)
                }
            }
        }

        testHelper.join()
        logger.info("threads ended, consuming remaining nodes")
        val visited = mutableSetOf<TreiberStackWithABA.Node<Long>>()
        while (true) {
            val node = stack.pop() ?: break
            if (visited.contains(node)) {
                fail("Cycle detected")
            }
            visited.add(node)
            removals.addAndGet(node.value)
        }
        assertEquals(removals.get(), insertions.get())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TreiberStackTests::class.java)
    }
}