package pt.isel.pc.sketches.leic41n.lecture2

import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

class ThreadBasicsTests {

    private var anInteger = 0

    @Test
    fun first() {
        val th = Thread {
            anInteger += 1
        }
        th.start()
        th.join()
        assertEquals(1, anInteger)
    }

    private var counter = 0

    @Test
    fun second() {
        val N_OF_THREADS = 10
        val N_OF_REPS = 1000
        val ths = mutableListOf<Thread>()
        repeat(N_OF_THREADS) {
            val th = Thread {
                repeat(N_OF_REPS) {
                    counter += 1
                }
            }
            th.start()
            ths.add(th)
        }
        ths.forEach {
            it.join()
        }
        // This will most probably fail: assertEquals(N_OF_THREADS * N_OF_REPS, counter)
    }

    @Test
    fun third() {
        val map = ConcurrentHashMap<Int, AtomicInteger>()
        val N_OF_THREADS = 10
        val N_OF_REPS = 1000
        val ths = mutableListOf<Thread>()
        repeat(N_OF_THREADS) {
            val th = Thread {
                var ix = 0
                repeat(N_OF_REPS) {
                    // Check-then-Act (CtA)
                    // check
                    val atomicInteger = map[ix]
                    if (atomicInteger == null) {
                        // act
                        map[ix] = AtomicInteger(1)
                    } else {
                        atomicInteger.incrementAndGet()
                    }
                    ix += 1
                }
            }
            th.start()
            ths.add(th)
        }
        ths.forEach {
            it.join()
        }
        var count = 0
        map.forEach { (_, data) ->
            count += data.get()
        }
        // This will most probably fail : assertEquals(N_OF_THREADS * N_OF_REPS, count)
    }
}