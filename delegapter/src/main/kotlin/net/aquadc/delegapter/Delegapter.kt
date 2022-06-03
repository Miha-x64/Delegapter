package net.aquadc.delegapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView


abstract class Delegapter protected constructor(initialCapacity: Int) {

    @JvmField protected var items: ArrayList<Any?> = newArrayList(initialCapacity)
    @JvmField protected var itemDelegates: ArrayList<Delegate<*>> = newArrayList(initialCapacity)

    protected fun <E> newArrayList(initialCapacity: Int): ArrayList<E> =
        if (initialCapacity < 0) ArrayList() else ArrayList(initialCapacity)

    // common mutable interface

    fun <D : Any> add(item: D, delegate: DiffDelegate<in D>): Boolean =
        addAt(items.size, item, delegate)
    abstract fun <D : Any> addAt(index: Int, item: D, delegate: DiffDelegate<in D>): Boolean
    abstract fun <D : Any> set(index: Int, item: D, delegate: DiffDelegate<in D>/*, payload: Any? = null*/): Boolean

    fun <D : Any> addAll(items: Collection<D>, delegate: DiffDelegate<in D>): Boolean =
        addAllAt(this.items.size, items, delegate)
    abstract fun <D : Any> addAllAt(index: Int, items: Collection<D>, delegate: DiffDelegate<in D>): Boolean

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

    // debug

    override fun toString(): String = buildString {
        append(super.toString()).append('(').append(items.size).append("): ").append('[').append('\n')
        for (i in items.indices) {
            append('#').append(i).append(' ').appendFun(itemDelegates[i]).append(": ").append(items[i]).append('\n')
        }
        append(']')
    }

}

fun Delegapter(
    target: ListUpdateCallback, parent: MutableDelegapter? = null, initialCapacity: Int = -1,
): MutableDelegapter =
    MutableDelegapter(target, parent, initialCapacity)

fun Delegapter(
    target: RecyclerView.Adapter<*>, parent: MutableDelegapter? = null, initialCapacity: Int = -1,
): MutableDelegapter =
    MutableDelegapter(target, parent, initialCapacity)

typealias Delegate<D> = (parent: ViewGroup) -> VH<*, *, D>
abstract class DiffDelegate<D : Any> : DiffUtil.ItemCallback<D>(), (ViewGroup) -> VH<*, *, D> // Delegate<D>
