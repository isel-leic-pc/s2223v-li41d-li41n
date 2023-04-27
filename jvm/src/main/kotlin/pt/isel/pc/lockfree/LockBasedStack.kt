package pt.isel.pc.lockfree

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class LockBasedStack<T> : Stack<T> {

    private class Node<T>(
        val value: T,
        val next: Node<T>?,
    )

    private var head: Node<T>? = null
    private val lock = ReentrantLock()

    override fun push(value: T) = lock.withLock {
        head = Node(value = value, next = head)
    }

    override fun pop(): T? = lock.withLock {
        val observedHead = head ?: return null
        val res = observedHead.value
        head = observedHead.next
        return res
    }
}