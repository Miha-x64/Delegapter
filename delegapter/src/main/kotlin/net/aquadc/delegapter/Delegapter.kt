@file:JvmName("Delegapters")
package net.aquadc.delegapter

import android.view.ViewGroup
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import net.aquadc.delegapter.decor.DelegatePredicate

/**
 * Data structure for holding (delegate, item) pairs with agreed types.
 * @author Mike Gorünóv
 */
abstract class Delegapter protected constructor(initialCapacity: Int) {

    @JvmField protected var itemDelegates: RemoveRangeArrayList<Delegate<*>> = RemoveRangeArrayList.create(initialCapacity)
    @JvmField protected var items: RemoveRangeArrayList<Any?> = RemoveRangeArrayList.create(initialCapacity)

    // common mutable interface

    abstract fun <D : Any> add(delegate: DiffDelegate<in D>, item: D, atIndex: Int = size): Boolean
    abstract fun <D : Any> set(delegate: DiffDelegate<in D>, item: D, atIndex: Int): Boolean
    abstract fun <D : Any> addAll(delegate: DiffDelegate<in D>, items: Collection<D>, atIndex: Int = size): Boolean

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

    fun <D> indexOf(
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
    fun <D> indexOf(
        delegate: Delegate<D>,
        startIndex: Int = 0, direction: Int = 1,
    ): Int {
        require(direction != 0)
        var i = startIndex
        while (i in 0 until size) {
            if (delegate == itemDelegates[i])
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

inline fun Delegapter.findIndexOf(
    delegate: DelegatePredicate, item: (Any?) -> Boolean = { true },
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

inline fun <D> Delegapter.findIndexOfBy(
    noinline delegate: Delegate<D>, item: (D) -> Boolean = { true },
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

inline fun <D : Any> Delegapter.findBy(
    noinline delegate: Delegate<D>, item: (D) -> Boolean = { true },
    startIndex: Int = 0, direction: Int = 1,
): D? {
    val i = findIndexOfBy(delegate, item, startIndex, direction)
    return if (i < 0) null else itemAt(i) as D
}

inline val Delegapter.lastIndex: Int
    get() = size - 1

fun Delegapter.add(delegate: DiffDelegate<in Unit>, atIndex: Int = size): Boolean = add(delegate, Unit, atIndex)
fun Delegapter.set(delegate: DiffDelegate<in Unit>, atIndex: Int): Boolean = set(delegate, Unit, atIndex)
fun MutableDelegapter.add(delegate: Delegate<in Unit>, atIndex: Int = size): Boolean = add(delegate, Unit, atIndex)
fun MutableDelegapter.set(delegate: Delegate<in Unit>, atIndex: Int): Boolean = set(delegate, Unit, atIndex)