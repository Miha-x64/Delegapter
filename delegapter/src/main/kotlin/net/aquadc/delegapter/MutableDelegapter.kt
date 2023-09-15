package net.aquadc.delegapter

import android.view.ViewGroup
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import java.lang.ref.WeakReference

/**
 * Mutable data structure for holding (delegate, item) pairs with agreed types.
 * @author Mike Gorünóv
 *
 * @param target changes listener, typically adapter
 * @param parent delegapter father to share viewTypes with
 * @param initialDelegateCapacity how many delegates expected to manage. Ignored if [parent] is specified
 * @param initialItemCapacity how many items expected to manage
 */
class MutableDelegapter(
    private val target: ListUpdateCallback,
    private val parent: MutableDelegapter? = null,
    initialDelegateCapacity: Int = -1,
    initialItemCapacity: Int = -1,
) : Delegapter(initialItemCapacity) {

    constructor(
        target: RecyclerView.Adapter<*>,
        parent: MutableDelegapter? = null,
        initialDelegateCapacity: Int = -1,
        initialCapacity: Int = -1,
    ) : this(
        AdapterListUpdateCallback(target), parent, initialDelegateCapacity, initialCapacity,
    )

    private val viewTypeList: RemoveRangeArrayList<WeakReference<Delegate<*>>?>
    private val viewTypeMap: WeakIdentityHashMap<Delegate<*>, Int>

    private var repeat: RepeatList<Delegate<*>>? = null
        get() = field ?: parent?.repeat
        set(value) { field = value; if (parent != null) parent.repeat = value }
    private var differ: Differ? = null
        get() = field ?: parent?.differ
        set(value) { field = value; if (parent != null) parent.differ = differ }

    init {
        if (parent == null) {
            viewTypeList = RemoveRangeArrayList.create(initialDelegateCapacity)
            viewTypeMap = object : WeakIdentityHashMap<Delegate<*>, Int>(
                if (initialDelegateCapacity < 0) 16 else initialDelegateCapacity,
            ) {
                override fun staleEntryExpunged(value: Int) {
                    _viewPool?.setMaxRecycledViews(value, 0)
                }
            }
        } else {
            viewTypeList = parent.viewTypeList
            viewTypeMap = parent.viewTypeMap
        }
    }

    private var _viewPool: RecyclerView.RecycledViewPool? = null
    val recycledViewPool: RecyclerView.RecycledViewPool
        get() = parent?.recycledViewPool ?: (_viewPool ?: RecyclerView.RecycledViewPool().also { _viewPool = it })

    // configure like a MutableList

    override fun <D> add(delegate: DiffDelegate<in D>, item: D, atIndex: Int): Unit =
        add(delegate as Delegate<in D>, item, atIndex)
    @JvmOverloads fun <D> add(delegate: Delegate<in D>, item: D, atIndex: Int = size) {
        items.add(atIndex, item)
        itemDelegates.add(atIndex, delegate)
        target.onInserted(atIndex, 1)
        tryAddDelegate(delegate)
    }

    override fun <D> set(delegate: DiffDelegate<in D>, item: D, atIndex: Int): Unit =
        set(delegate as Delegate<in D>, item, atIndex)
    @JvmOverloads fun <D> set(delegate: Delegate<in D>, item: D, atIndex: Int, payload: Any? = null) {
        items[atIndex] = item
        itemDelegates[atIndex] = delegate
        target.onChanged(atIndex, 1, payload)
        tryAddDelegate(delegate)
    }

    override fun <D> addAll(delegate: DiffDelegate<in D>, items: Collection<D>, atIndex: Int): Unit =
        addAll(delegate as Delegate<in D>, items, atIndex)
    @JvmOverloads fun <D> addAll(delegate: Delegate<in D>, items: Collection<D>, atIndex: Int = size) {
        if (items.isNotEmpty()) {
            this.items.addAll(atIndex, items)
            (repeat ?: RepeatList<Delegate<*>>().also { repeat = it })
                .of(delegate, items.size) { itemDelegates.addAll(atIndex, it) }
            target.onInserted(atIndex, items.size)
            tryAddDelegate(delegate)
        }
    }

    // e.g. you've computed a chunk on a background thread and want to add its contents
    fun addAll(from: Delegapter, fromIndex: Int = 0, toIndex: Int = from.size, atIndex: Int = size) {
        require(fromIndex >= 0 && toIndex <= from.size)
        if (fromIndex == toIndex) return

        var items: List<Any?> = from.items
        var itemDelegates: List<Delegate<*>> = from.itemDelegates
        if (fromIndex != 0 || toIndex != from.size) {
            items = items.subList(fromIndex, toIndex)
            itemDelegates = itemDelegates.subList(fromIndex, toIndex)
        }

        // before we tryAddDelegates(), implicitly range-check `atIndex` by List internals
        this.items.addAll(atIndex, items)
        this.itemDelegates.addAll(atIndex, itemDelegates)
        // now we're safe
        for (i in itemDelegates.indices) {
            tryAddDelegate(itemDelegates[i])
        }

        target.onInserted(atIndex, items.size)
    }

    private fun tryAddDelegate(delegate: Delegate<*>) {
        if (!viewTypeMap.containsKey(delegate)) {
            viewTypeList.add(
                viewTypeMap.putAndGetKeyRef(delegate, viewTypeList.size)
            )
        }
    }

    fun remove(element: Any?): Boolean {
        val iof = items.indexOf(element)
        return if (iof < 0) false else { removeAt(iof); true }
    }
    fun removeAt(position: Int) {
        items.removeAt(position)
        itemDelegates.removeAt(position)
        target.onRemoved(position, 1)
    }
    fun removeRange(start: Int, endEx: Int) {
        items.removeRange(start, endEx)
        itemDelegates.removeRange(start, endEx)
        target.onRemoved(start, endEx - start)
    }
    fun removeAll(elements: Collection<Any?>): Boolean = batchRemove(elements, false)
    fun retainAll(elements: Collection<Any?>): Boolean = batchRemove(elements, true)
    private fun batchRemove(elements: Collection<Any?>, complement: Boolean): Boolean =
        batchRemoveIf { elements.contains(items[it]) != complement }
    @JvmName("removeAllBy") fun removeAll(delegate: Delegate<*>): Boolean = batchRemoveBy(delegate, false)
    @JvmName("retainAllBy") fun retainAll(delegate: Delegate<*>): Boolean = batchRemoveBy(delegate, true)
    private fun batchRemoveBy(delegate: Delegate<*>, complement: Boolean): Boolean =
        batchRemoveIf { (itemDelegates[it] == delegate) != complement }
    @JvmName("removeAllBy") fun removeAll(delegates: Collection<Delegate<*>>): Boolean = batchRemoveBy(delegates, false)
    @JvmName("retainAllBy") fun retainAll(delegates: Collection<Delegate<*>>): Boolean = batchRemoveBy(delegates, true)
    private fun batchRemoveBy(delegates: Collection<Delegate<*>>, complement: Boolean): Boolean =
        batchRemoveIf { delegates.contains(itemDelegates[it]) != complement }
    private inline fun batchRemoveIf(predicate: (Int) -> Boolean): Boolean {
        var removed = 0
        for (i in itemDelegates.indices) if (predicate(i)) {
            items.markForRemoval(i)
            itemDelegates.markForRemoval(i)
            target.onRemoved(i - removed++, 1)
        }
        return if (removed > 0) {
            items.commitRemovals()
            itemDelegates.commitRemovals()
            true
        } else false
    }

    fun clear() {
        if (!isEmpty) {
            val size = items.size
            items.clear()
            itemDelegates.clear()
            target.onRemoved(0, size)
        }
    }

    // adapter-specific things

    inline fun replace(detectMoves: Boolean = true, initialItemCapacity: Int = -1, block: Delegapter.() -> Unit) {
        commit(detectMoves, DiffDelegapter(initialItemCapacity).apply(block))
    }
    @PublishedApi internal fun commit(detectMoves: Boolean, tmp: DiffDelegapter) {
        val differ = differ ?: Differ().also { differ = it }
        differ.old = this
        differ.new = tmp
        DiffUtil.calculateDiff(differ, detectMoves).dispatchUpdatesTo(target)
        differ.old = null
        differ.new = null
        tmp.commit()
    }
    @PublishedApi internal inner class DiffDelegapter
    @PublishedApi internal constructor(initialItemCapacity: Int) : Delegapter(initialItemCapacity) {
        override fun <D> add(delegate: DiffDelegate<in D>, item: D, atIndex: Int) {
            items.add(atIndex, item)
            itemDelegates.add(atIndex, delegate)
            tryAddDelegate(delegate)
        }
        override fun <D> set(delegate: DiffDelegate<in D>, item: D, atIndex: Int) {
            items[atIndex] = item
            itemDelegates[atIndex] = delegate
            tryAddDelegate(delegate)
        }
        override fun <D> addAll(delegate: DiffDelegate<in D>, items: Collection<D>, atIndex: Int) {
            if (items.isNotEmpty()) {
                this.items.addAll(atIndex, items)
                (repeat ?: RepeatList<Delegate<*>>().also { repeat = it })
                    .of(delegate, items.size) { itemDelegates.addAll(atIndex, it) }
                tryAddDelegate(delegate)
            }
        }

        fun commit() {
            this@MutableDelegapter.items = items
            this@MutableDelegapter.itemDelegates = itemDelegates
        }
    }

    fun viewTypeAt(position: Int): Int =
        viewTypeMap[itemDelegates[position]]!!

    @Deprecated("I'm a data structure, not an Adapter", ReplaceWith("this.forViewType(viewType)(parent)"))
    fun createViewHolder(parent: ViewGroup, viewType: Int): VH<*, *, *> =
        viewTypeList[viewType]!!.get()!!(parent)

    fun forViewType(viewType: Int): Delegate<*> =
        viewTypeList[viewType]!!.get()!!

    @Deprecated("I'm a data structure, not an Adapter", ReplaceWith("(holder as VH<*, *, Any?>).bind(this.itemAt(position), position, payloads)"))
    fun bindViewHolder(holder: VH<*, *, *>, position: Int, payloads: List<Any> = emptyList()): Unit =
        @Suppress("UNCHECKED_CAST") (holder as VH<*, *, Any?>).bind(items[position], position, payloads)

    /**
     * Get `viewType` of the [delegate] in this Delegapter or its parent, or `-1`,
     * if it was never ever added.
     */
    fun peekViewTypeOf(delegate: Delegate<*>): Int =
        viewTypeMap[delegate] ?: -1

    /**
     * Get `viewType` of the [delegate] in this Delegapter or its parent.
     * Adds [delegate] if absent.
     * Useful when configuring [androidx.recyclerview.widget.RecyclerView.RecycledViewPool].
     */
    fun forceViewTypeOf(delegate: Delegate<*>): Int {
        tryAddDelegate(delegate)
        return viewTypeMap[delegate]!!
    }

}

@Suppress("UNCHECKED_CAST") private class Differ : DiffUtil.Callback() {
    @JvmField var old: Delegapter? = null
    @JvmField var new: Delegapter? = null
    override fun getOldListSize(): Int = old!!.size
    override fun getNewListSize(): Int = new!!.size
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        new!!.delegateAt(newItemPosition).let {
            it == old!!.delegateAt(oldItemPosition) &&
                (it as DiffUtil.ItemCallback<Any>)
                    .areItemsTheSame(old!!.itemAt(oldItemPosition)!!, new!!.itemAt(newItemPosition)!!)
        }
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        new!!.delegateAt(newItemPosition).let {
            it == old!!.delegateAt(oldItemPosition) &&
                (it as DiffUtil.ItemCallback<Any>)
                    .areContentsTheSame(old!!.itemAt(oldItemPosition)!!, new!!.itemAt(newItemPosition)!!)
        }
    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? =
        (new!!.delegateAt(newItemPosition).takeIf { it == old!!.delegateAt(oldItemPosition) } as DiffUtil.ItemCallback<Any>?)
            ?.getChangePayload(old!!.itemAt(oldItemPosition)!!, new!!.itemAt(newItemPosition)!!)
}
