package pt.isel.pc.lockfree

import java.util.concurrent.atomic.AtomicReference

class TreiberStack<T> : Stack<T> {

    private class Node<T>(
        val value: T,
    ) {
        var next: Node<T>? = null
    }

    private val head = AtomicReference<Node<T>>(null)

    override fun push(value: T) {
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

    override fun pop(): T? {
        while (true) {
            val observedHead: Node<T> = head.get() ?: return null
            val observedHeadNext = observedHead.next
            if (head.compareAndSet(observedHead, observedHeadNext)) {
                return observedHead.value
            }
            // retry
        }
    }
}