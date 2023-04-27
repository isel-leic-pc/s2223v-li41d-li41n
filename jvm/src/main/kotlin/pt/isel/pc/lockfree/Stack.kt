package pt.isel.pc.lockfree

interface Stack<T> {
    fun push(value: T)
    fun pop(): T?
}