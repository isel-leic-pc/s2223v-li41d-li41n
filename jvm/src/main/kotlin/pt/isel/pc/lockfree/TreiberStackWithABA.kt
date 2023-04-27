package pt.isel.pc.lockfree

import java.util.concurrent.atomic.AtomicReference


class TreiberStackWithABA<T> {

    class Node<T>(
        val value: T,
    ) {
        var next: Node<T>? = null
    }

    private val head = AtomicReference<Node<T>>(null)

    fun push(node: Node<T>) {
        while (true) {
            val observedHead: Node<T>? = head.get()
            node.next = observedHead
            if (head.compareAndSet(observedHead, node)) {
                return
            }
            // retry
        }
    }

    fun pop(): Node<T>? {
        while (true) {
            val observedHead: Node<T> = head.get() ?: return null
            val observedNext = observedHead.next
            if (head.compareAndSet(observedHead, observedNext)) {
                return observedHead
            }
            // retry
        }
    }
}