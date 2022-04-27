package net.aquadc.delegapter

import java.util.Arrays


@Suppress("UNCHECKED_CAST")
internal class RepeatList<T> : AbstractList<T>() {
    private var _size = 0 // getterless access
    private var element: T? = null

    override val size: Int = _size

    override fun get(index: Int): T =
        if (index in 0 until _size) element as T else throw oob(index)

    private fun oob(index: Int) = // extracted rare/unreachable path, don't bother JIT with StringBuilder chain
        IndexOutOfBoundsException("$index ∉ [0, $_size)")

    override fun toArray(): Array<Any?> = // ArrayList#addAll uses .toArray()
        arrayOfNulls<Any>(_size).also { Arrays.fill(it, element) }

    internal fun of(times: Int, what: T) { // outlined to avoid two synthetic accessors for private fields
        _size = times
        element = what
    }

    internal inline fun <R> of(what: T, times: Int, block: (List<T>) -> R): R {
        of(times, what)
        val r = block(this)
        of(0, null as T) // unleak
        return r
    }
}

private val REMOVED = Any()
private val REMOVED_LIST = setOf(REMOVED)
@Suppress("UNCHECKED_CAST")
internal fun MutableList<*>.markForRemoval(at: Int) {
    (this as MutableList<Any>)[at] = REMOVED
}
internal fun MutableList<*>.commitRemovals() {
    removeAll(REMOVED_LIST)
}
