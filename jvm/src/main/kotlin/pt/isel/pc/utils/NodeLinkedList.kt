package pt.isel.pc.utils

class NodeLinkedList<T> {

    interface Node<T> {
        val value: T
    }

    private class NodeImpl<T>(val maybeValue: T?) : Node<T> {

        constructor (maybeValue: T?, n: NodeImpl<T>, p: NodeImpl<T>) : this(maybeValue) {
            next = n
            prev = p
        }

        lateinit var next: NodeImpl<T>
        lateinit var prev: NodeImpl<T>

        override val value: T
            get() {
                require(maybeValue != null) { "Only nodes with non-null values can be exposed publicly" }
                return maybeValue
            }
    }

    private val head: NodeImpl<T> = NodeImpl(null)

    init {
        head.next = head
        head.prev = head
    }

    var count = 0
        private set

    fun enqueue(value: T): Node<T> {
        val tail: NodeImpl<T> = head.prev
        val node: NodeImpl<T> = NodeImpl(value, head, tail)
        head.prev = node
        tail.next = node
        count += 1
        return node
    }

    val empty: Boolean
        get() = head === head.prev

    val notEmpty: Boolean
        get() = !empty

    val headValue: T?
        get() {
            return if (notEmpty) {
                head.next.value
            } else {
                null
            }
        }

    val headNode: Node<T>?
        get() {
            return if (notEmpty) {
                head.next
            } else {
                null
            }
        }

    fun isHeadNode(node: Node<T>) = head.next === node

    inline fun headCondition(cond: (T) -> Boolean): Boolean = headValue?.let { cond(it) } == true

    fun pull(): Node<T> {
        require(!empty) { "cannot pull from an empty list" }
        val node = head.next
        head.next = node.next
        node.next.prev = head
        count -= 1
        return node
    }

    fun remove(node: Node<T>) {
        require(node is NodeImpl<T>) { "node must be an internal node" }
        node.prev.next = node.next
        node.next.prev = node.prev
        count -= 1
    }
}