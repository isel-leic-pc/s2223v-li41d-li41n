package pt.isel.pc.sketches.leic41d.lecture14

import java.util.concurrent.atomic.AtomicReference

class TreiberStack<T> {

    private class Node<T>(
        val value: T,
    ) {
        var next: Node<T>? = null
    }

    private val head = AtomicReference<Node<T>>(null)

    fun push(value: T) {
        val node = Node(value)
        while (true) {
            val observedHead = head.get()
            node.next = observedHead
            if (head.compareAndSet(observedHead, node)) {
                return
            }
            // retry
        }
    }
}