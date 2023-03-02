package pt.isel.pc.sketches.leic41d.lecture2

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ThreadBasicsTests {

    private var anInteger: Int = 0

    @Test
    fun first() {
        val th = Thread {
            anInteger += 1
        }
        th.start()
        th.join()
        assertEquals(1, anInteger)
    }

    @Test
    fun second() {
        val N_OF_THREADS = 10
        val N_OF_REPS = 100
        val ths = mutableListOf<Thread>()
        repeat(N_OF_THREADS) {
            val th = Thread {
                repeat(N_OF_REPS) {
                    anInteger += 1
                }
            }
            th.start()
            ths.add(th)
        }
        ths.forEach {
            it.join()
        }
        // this will fail: assertEquals(N_OF_THREADS * N_OF_REPS, anInteger)
    }
}