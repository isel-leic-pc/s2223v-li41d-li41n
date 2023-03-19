package pt.isel.pc.sketches.leic41n.lecture5

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class LockBasedLinkedStack<T> {

    private class Node<T>(val item: T, val next: Node<T>?)

    // mutable
    private var head: Node<T>? = null
    private val theLock: Lock = ReentrantLock()

    fun push(value: T) = theLock.withLock {
        head = Node(item = value, next = head)
    }

    fun pop(): T? = theLock.withLock {
        val observedHead = head ?: return null
        head = observedHead.next
        return observedHead.item
    }

    val isEmpty: Boolean
        get() = theLock.withLock { head == null }
}