@file:JvmName("Delegapters")
package net.aquadc.delegapter

import android.view.ViewGroup
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView


abstract class Delegapter protected constructor(initialCapacity: Int) {

    @JvmField protected var items: ArrayList<Any?> = newArrayList(initialCapacity)
    @JvmField protected var itemDelegates: ArrayList<Delegate<*>> = newArrayList(initialCapacity)

    protected fun <E> newArrayList(initialCapacity: Int): ArrayList<E> =
        if (initialCapacity < 0) ArrayList() else ArrayList(initialCapacity)

    // common mutable interface

    fun <D : Any> add(delegate: DiffDelegate<in D>, item: D): Boolean =
        addAt(items.size, delegate, item)
    abstract fun <D : Any> addAt(index: Int, delegate: DiffDelegate<in D>, item: D): Boolean
    abstract fun <D : Any> set(index: Int, delegate: DiffDelegate<in D>, item: D): Boolean

    fun <D : Any> addAll(delegate: DiffDelegate<in D>, items: Collection<D>): Boolean =
        addAllAt(this.items.size, delegate, items)
    abstract fun <D : Any> addAllAt(index: Int, delegate: DiffDelegate<in D>, items: Collection<D>): Boolean

    // use like a List

    val size: Int
        get() = items.size

    val isEmpty: Boolean
        get() = items.isEmpty()

    fun itemAt(position: Int): Any? =
        items[position]

    fun delegateAt(position: Int): Delegate<*> =
        itemDelegates[position]

    fun contains(element: Any?): Boolean =
        items.contains(element)

    fun containsAll(elements: Collection<Any?>): Boolean =
        items.containsAll(elements)

    fun containsAny(delegate: Delegate<*>): Boolean =
        itemDelegates.contains(delegate)

    @JvmName("indexOfDelegateAndItem") fun <D> indexOf(
        delegate: Delegate<D>, item: D,
        startIndex: Int = 0, direction: Int = 1,
    ): Int {
        require(direction != 0)
        var i = startIndex
        while (i in 0 until size) {
            if (delegate == itemDelegates[i] && item == items[i])
                return i
            i += direction
        }
        return -1
    }

    // debug

    override fun toString(): String = buildString {
        append(super.toString()).append('(').append(items.size).append("): ").append('[').append('\n')
        for (i in items.indices) {
            append('#').append(i).append(' ').appendFun(itemDelegates[i]).append(": ").append(items[i]).append('\n')
        }
        append(']')
    }

}

@Suppress("NOTHING_TO_INLINE") @JvmName("create") @JvmOverloads
inline fun Delegapter(
    target: ListUpdateCallback, parent: MutableDelegapter? = null, initialCapacity: Int = -1,
): MutableDelegapter =
    MutableDelegapter(target, parent, initialCapacity)

@Suppress("NOTHING_TO_INLINE") @JvmName("create") @JvmOverloads
inline fun Delegapter(
    target: RecyclerView.Adapter<*>, parent: MutableDelegapter? = null, initialCapacity: Int = -1,
): MutableDelegapter =
    MutableDelegapter(target, parent, initialCapacity)

typealias Delegate<D> = (parent: ViewGroup) -> VH<*, *, D>

inline fun Delegapter.indexOf(
    delegate: (Delegate<*>) -> Boolean, item: (Any?) -> Boolean,
    startIndex: Int = 0, direction: Int = 1,
): Int {
    require(direction != 0)
    var i = startIndex
    while (i in 0 until size) {
        if (delegate(delegateAt(i)) && item(itemAt(i)))
            return i
        i += direction
    }
    return -1
}

@JvmName("indexOfByDelegate") inline fun <D> Delegapter.indexOf(
    noinline delegate: Delegate<D>, item: (D) -> Boolean,
    startIndex: Int = 0, direction: Int = 1,
): Int {
    require(direction != 0)
    var i = startIndex
    while (i in 0 until size) {
        if (delegate == delegateAt(i) && item(itemAt(i) as D))
            return i
        i += direction
    }
    return -1
}

inline val Delegapter.lastIndex: Int
    get() = size - 1
