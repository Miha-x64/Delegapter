@file:JvmName("Delegapters")
package net.aquadc.delegapter1

import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import net.aquadc.delegapter.RemoveRangeArrayList

/**
 * Data structure for holding (delegate, item) pairs with agreed types.
 * @author Mike Gorünóv
 */
@Suppress("KDocMissingDocumentation") // all members have obvious purposes
abstract class Delegapter protected constructor(initialItemCapacity: Int) {

    @JvmField protected var itemDelegates: RemoveRangeArrayList<AdapterDelegate<*, *>> = RemoveRangeArrayList.create(initialItemCapacity)
    @JvmField protected var items: RemoveRangeArrayList<Any?> = RemoveRangeArrayList.create(initialItemCapacity)

    // common mutable interface

    abstract fun <T> add(delegate: AdapterDelegate<T, Diff<T>>, item: T, atIndex: Int = size)
    abstract fun <T> set(delegate: AdapterDelegate<T, Diff<T>>, item: T, atIndex: Int)
    abstract fun <T> addAll(delegate: AdapterDelegate<T, Diff<T>>, items: Collection<T>, atIndex: Int = size)

    // use like a List

    val size: Int
        get() = items.size

    fun itemAt(position: Int): Any? =
        items[position]

    fun delegateAt(position: Int): AdapterDelegate<*, *> =
        itemDelegates[position]

    fun contains(item: Any?): Boolean =
        items.contains(item)

    fun containsAll(items: Collection<Any?>): Boolean =
        this.items.containsAll(items)

    fun containsAny(delegate: AdapterDelegate<*, *>): Boolean =
        itemDelegates.contains(delegate)

    fun <T> indexOf(
        delegate: AdapterDelegate<T, *>, item: T,
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
    fun <T> indexOf(
        delegate: AdapterDelegate<T, *>,
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
            append('#').append(i).append(' ').append(itemDelegates[i]).append(": ").append(items[i]).append('\n')
        }
        append(']')
    }

    // hacks

    // fuck Kotlin access rules!
    protected val Delegapter.items: RemoveRangeArrayList<Any?> get() = this.items
    protected val Delegapter.itemDelegates: RemoveRangeArrayList<AdapterDelegate<*, *>> get() = this.itemDelegates

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

inline fun Delegapter.findIndexOf(
    delegate: (AdapterDelegate<*, *>) -> Boolean, item: (Any?) -> Boolean = { true },
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

inline fun <T> Delegapter.findIndexOfBy(
    delegate: AdapterDelegate<T, *>, item: (T) -> Boolean = { true },
    startIndex: Int = 0, direction: Int = 1,
): Int {
    require(direction != 0)
    var i = startIndex
    while (i in 0 until size) {
        if (delegate == delegateAt(i) && item(itemAt(i) as T))
            return i
        i += direction
    }
    return -1
}

inline fun <T> Delegapter.findBy(
    delegate: AdapterDelegate<T, *>, item: (T) -> Boolean = { true },
    startIndex: Int = 0, direction: Int = 1,
): T? {
    val i = findIndexOfBy(delegate, item, startIndex, direction)
    return if (i < 0) null else itemAt(i) as T
}

inline val Delegapter.isEmpty: Boolean
    get() = size == 0

inline val Delegapter.lastIndex: Int
    get() = size - 1

fun Delegapter.add(delegate: AdapterDelegate<Unit, Diff<Unit>>, atIndex: Int = size): Unit = add(delegate, Unit, atIndex)
fun Delegapter.set(delegate: AdapterDelegate<Unit, Diff<Unit>>, atIndex: Int): Unit = set(delegate, Unit, atIndex)
fun MutableDelegapter.add(delegate: AdapterDelegate<Unit, *>, atIndex: Int = size): Unit = add(delegate, Unit, atIndex)
fun MutableDelegapter.set(delegate: AdapterDelegate<Unit, *>, atIndex: Int): Unit = set(delegate, Unit, atIndex)
// workarounds to fix overload resolution ambiguity: TODO figure out whether we need these
//fun MutableDelegapter.add(delegate: AdapterDelegate<Unit, Diff<Unit>>, atIndex: Int = size): Unit = add(delegate, Unit, atIndex)
//fun MutableDelegapter.set(delegate: AdapterDelegate<Unit, Diff<Unit>>, atIndex: Int): Unit = set(delegate, Unit, atIndex)

inline fun Delegapter.forEachIndexed(block: (index: Int, delegate: AdapterDelegate<*, *>, item: Any?) -> Unit) {
    repeat(size) {
        block(it, delegateAt(it), itemAt(it))
    }
}
inline fun <T> Delegapter.forEachIndexed(delegate: AdapterDelegate<T, *>, block: (index: Int, item: T) -> Unit) {
    forEachIndexed { index, curDelegate, item ->
        if (curDelegate === delegate)
            block(index, item as T)
    }
}
inline fun Delegapter.forEach(block: (delegate: AdapterDelegate<*, *>, item: Any?) -> Unit) {
    forEachIndexed { _, delegate, item ->
        block(delegate, item)
    }
}
inline fun <T> Delegapter.forEach(delegate: AdapterDelegate<T, *>, block: (item: T) -> Unit) {
    forEachIndexed(delegate) { _, item ->
        block(item)
    }
}

/**
 * Create [span size lookup][SpanSizeLookup] determining span size
 * based on position, an according item, and its delegate.
 *
 * Caches [span indices][SpanSizeLookup.setSpanIndexCacheEnabled]
 * and [group indices][SpanSizeLookup.setSpanGroupIndexCacheEnabled].
 */
inline fun Delegapter.spanSizeLookup(
    crossinline getSpanSize: (position: Int, delegate: AdapterDelegate<*, *>, item: Any?) -> Int,
): SpanSizeLookup = object : SpanSizeLookup() {
    init {
        isSpanIndexCacheEnabled = true
        isSpanGroupIndexCacheEnabled = true
    }
    override fun getSpanSize(position: Int): Int =
        getSpanSize.invoke(position, delegateAt(position), itemAt(position))
}

