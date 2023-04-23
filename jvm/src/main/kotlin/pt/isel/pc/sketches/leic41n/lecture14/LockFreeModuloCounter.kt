package pt.isel.pc.sketches.leic41n.lecture14

import java.util.concurrent.atomic.AtomicInteger

class LockFreeModuloCounter(
    private val modulus: Int,
) {
    init {
        require(modulus > 0) { "modulus must be strictly positive" }
    }

    private val boxWithInt = AtomicInteger()

    fun inc() {
        while (true) {
            val observed = boxWithInt.get()
            val next = if (observed < modulus - 1) {
                observed + 1
            } else {
                0
            }
            if (boxWithInt.compareAndSet(observed, next)) {
                return
            }
            // retry
        }
    }

    fun dec() {
        while (true) {
            val observed = boxWithInt.get()
            val next = if (observed == 0) {
                modulus - 1
            } else {
                observed - 1
            }
            if (boxWithInt.compareAndSet(observed, next)) {
                return
            }
            // retry
        }
    }

    fun read(): Int = boxWithInt.get()
}