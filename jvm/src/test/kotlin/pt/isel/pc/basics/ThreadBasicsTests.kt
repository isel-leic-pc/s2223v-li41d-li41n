package pt.isel.pc.basics

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ThreadBasicsTests {

    @Test
    fun thread_create_and_synchronization() {
        var anInteger = 0
        val th = Thread {
            anInteger = 1
        }
        th.start()
        th.join()
        assertEquals(1, anInteger)
    }
}