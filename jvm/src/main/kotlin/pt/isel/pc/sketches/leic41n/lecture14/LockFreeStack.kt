package pt.isel.pc.sketches.leic41n.lecture14

import java.util.concurrent.atomic.AtomicReference

class LockFreeStack<T> {

    private class Node<T>(
        val value: T,
    ) {
        var next: Node<T>? = null
    }

    private val head = AtomicReference<Node<T>>(null)

    fun push(value: T) {
        val node = Node(value = value)
        while (true) {
            val observedHead: Node<T>? = head.get()
            node.next = observedHead
            if (head.compareAndSet(observedHead, node)) {
                return
            }
            // retry
        }
    }
}