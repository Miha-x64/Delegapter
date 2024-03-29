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
abstract class Delegapter protected constructor(initialItemCapacity: Int) {

    @JvmField protected var itemDelegates: RemoveRangeArrayList<Delegate<*>> = RemoveRangeArrayList.create(initialItemCapacity)
    @JvmField protected var items: RemoveRangeArrayList<Any?> = RemoveRangeArrayList.create(initialItemCapacity)

    // common mutable interface

    abstract fun <D> add(delegate: DiffDelegate<in D>, item: D, atIndex: Int = size)
    abstract fun <D> set(delegate: DiffDelegate<in D>, item: D, atIndex: Int)
    abstract fun <D> addAll(delegate: DiffDelegate<in D>, items: Collection<D>, atIndex: Int = size)

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

    override fun toString(): String = if (isEmpty) "[]" else buildString {
        append(super.toString()).append('(').append(items.size).append("): ").append('[').append('\n')
        for (i in items.indices) {
            append('#').append(i).append(' ').appendFun(itemDelegates[i]).append(": ").append(items[i]).append('\n')
        }
        append(']')
    }

    // hacks

    // fuck Kotlin access rules!
    protected val Delegapter.items: RemoveRangeArrayList<Any?> get() = this.items
    protected val Delegapter.itemDelegates: RemoveRangeArrayList<Delegate<*>> get() = this.itemDelegates

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

inline fun <D> Delegapter.findBy(
    noinline delegate: Delegate<D>, item: (D) -> Boolean = { true },
    startIndex: Int = 0, direction: Int = 1,
): D? {
    val i = findIndexOfBy(delegate, item, startIndex, direction)
    return if (i < 0) null else itemAt(i) as D
}

inline val Delegapter.lastIndex: Int
    get() = size - 1

fun Delegapter.add(delegate: DiffDelegate<in Unit>, atIndex: Int = size): Unit = add(delegate, Unit, atIndex)
fun Delegapter.set(delegate: DiffDelegate<in Unit>, atIndex: Int): Unit = set(delegate, Unit, atIndex)
fun MutableDelegapter.add(delegate: Delegate<in Unit>, atIndex: Int = size): Unit = add(delegate, Unit, atIndex)
fun MutableDelegapter.set(delegate: Delegate<in Unit>, atIndex: Int): Unit = set(delegate, Unit, atIndex)
// workarounds to fix overload resolution ambiguity:
fun MutableDelegapter.add(delegate: DiffDelegate<in Unit>, atIndex: Int = size): Unit = add(delegate, Unit, atIndex)
fun MutableDelegapter.set(delegate: DiffDelegate<in Unit>, atIndex: Int): Unit = set(delegate, Unit, atIndex)

inline fun Delegapter.forEachIndexed(block: (index: Int, delegate: Delegate<*>, item: Any?) -> Unit) {
    repeat(size) {
        block(it, delegateAt(it), itemAt(it))
    }
}
inline fun <D> Delegapter.forEachIndexed(noinline delegate: Delegate<D>, block: (index: Int, item: D) -> Unit) {
    forEachIndexed { index, curDelegate, item ->
        if (curDelegate === delegate)
            block(index, item as D)
    }
}
inline fun Delegapter.forEach(block: (delegate: Delegate<*>, item: Any?) -> Unit) {
    forEachIndexed { _, delegate, item ->
        block(delegate, item)
    }
}
inline fun <D> Delegapter.forEach(noinline delegate: Delegate<D>, block: (item: D) -> Unit) {
    forEachIndexed(delegate) { _, item ->
        block(item)
    }
}
