package net.aquadc.delegapter1

import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import net.aquadc.delegapter.RemoveRangeArrayList
import net.aquadc.delegapter.RepeatList
import net.aquadc.delegapter.commitRemovals
import net.aquadc.delegapter.internal.WeakIdentityHashMap
import net.aquadc.delegapter.markForRemoval
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
@Suppress("KDocMissingDocumentation") // all members have obvious purposes
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

    private val viewTypeList: RemoveRangeArrayList<WeakReference<ViewHolderFactory>?>
    private val viewTypeMap: WeakIdentityHashMap<ViewHolderFactory, Int>

    private var repeat: RepeatList<AdapterDelegate<*, *>>? = null
        get() = field ?: parent?.repeat
        set(value) { field = value; if (parent != null) parent.repeat = value }
    private var differ: Differ? = null
        get() = field ?: parent?.differ
        set(value) { field = value; if (parent != null) parent.differ = differ }

    val recycledViewPool: RecyclerView.RecycledViewPool =
        parent?.recycledViewPool ?: RecyclerView.RecycledViewPool()

    init {
        if (parent == null) {
            viewTypeList = RemoveRangeArrayList.create(initialDelegateCapacity)
            viewTypeMap = object : WeakIdentityHashMap<ViewHolderFactory, Int>(
                if (initialDelegateCapacity < 0) 16 else initialDelegateCapacity,
            ) {
                override fun staleEntryExpunged(value: Int) {
                    recycledViewPool.setMaxRecycledViews(value, 0)
                }
            }
        } else {
            viewTypeList = parent.viewTypeList
            viewTypeMap = parent.viewTypeMap
        }
    }

    // configure like a MutableList

    override fun <D> add(delegate: AdapterDelegate<D, Diff<D>>, item: D, atIndex: Int): Unit =
        add(delegate as AdapterDelegate<D, *>, item, atIndex)
    @JvmName("addItem")
    @JvmOverloads fun <D> add(delegate: AdapterDelegate<D, *>, item: D, atIndex: Int = size) {
        items.add(atIndex, item)
        itemDelegates.add(atIndex, delegate)
        target.onInserted(atIndex, 1)
        tryAddDelegate(delegate)
    }

    override fun <D> set(delegate: AdapterDelegate<D, Diff<D>>, item: D, atIndex: Int): Unit =
        set(delegate as AdapterDelegate<D, *>, item, atIndex)
    @JvmName("setItem")
    @JvmOverloads fun <D> set(delegate: AdapterDelegate<D, *>, item: D, atIndex: Int, payload: Any? = null) {
        items[atIndex] = item
        itemDelegates[atIndex] = delegate
        target.onChanged(atIndex, 1, payload)
        tryAddDelegate(delegate)
    }

    override fun <D> addAll(delegate: AdapterDelegate<D, Diff<D>>, items: Collection<D>, atIndex: Int): Unit =
        addAll(delegate as AdapterDelegate<D, *>, items, atIndex)
    @JvmName("addItems")
    @JvmOverloads fun <D> addAll(delegate: AdapterDelegate<D, *>, items: Collection<D>, atIndex: Int = size) {
        if (items.isNotEmpty()) {
            this.items.addAll(atIndex, items)
            (repeat ?: RepeatList<AdapterDelegate<*, *>>().also { repeat = it })
                .of(delegate, items.size) { itemDelegates.addAll(atIndex, it) }
            target.onInserted(atIndex, items.size)
            tryAddDelegate(delegate)
        }
    }

    override fun addAll(items: List<Any?>, itemDelegates: List<AdapterDelegate<*, *>>, atIndex: Int) {
        // before we tryAddDelegates(), implicitly range-check `atIndex` by List internals
        this.items.addAll(atIndex, items)
        this.itemDelegates.addAll(atIndex, itemDelegates)
        // now we're safe
        for (i in itemDelegates.indices)
            tryAddDelegate(itemDelegates[i])

        target.onInserted(atIndex, items.size)
    }

    private fun tryAddDelegate(delegate: AdapterDelegate<*, *>) {
        val vhf = delegate.create
        val actual = vhf.actual
        if (!viewTypeMap.containsKey(actual)) {
            val viewType = viewTypeList.size
            viewTypeList.add(viewTypeMap.putAndGetKeyRef(actual, viewType))
            if (vhf is VHFMaxScrap) {
                recycledViewPool.setMaxRecycledViews(viewType, vhf.maxRecycledViews)
            }
        }
    }
    private val ViewHolderFactory.actual
        get() = if (this is VHFMaxScrap) this.factory else this

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
    @JvmName("removeAllBy") fun removeAll(delegate: AdapterDelegate<*, *>): Boolean = batchRemoveBy(delegate, false)
    @JvmName("retainAllBy") fun retainAll(delegate: AdapterDelegate<*, *>): Boolean = batchRemoveBy(delegate, true)
    private fun batchRemoveBy(delegate: AdapterDelegate<*, *>, complement: Boolean): Boolean =
        batchRemoveIf { (itemDelegates[it] == delegate) != complement }
    @JvmName("removeAllBy") fun removeAll(delegates: Collection<AdapterDelegate<*, *>>): Boolean = batchRemoveBy(delegates, false)
    @JvmName("retainAllBy") fun retainAll(delegates: Collection<AdapterDelegate<*, *>>): Boolean = batchRemoveBy(delegates, true)
    private fun batchRemoveBy(delegates: Collection<AdapterDelegate<*, *>>, complement: Boolean): Boolean =
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

    inline fun replace(detectMoves: Boolean = true, initialItemCapacity: Int = -1, block: Delegapter.() -> Unit) { // TODO executor for Diff
        commit(detectMoves, DiffDelegapter(initialItemCapacity).apply(block)) // TODO @AnyThread
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
        override fun <D> add(delegate: AdapterDelegate<D, Diff<D>>, item: D, atIndex: Int) {
            items.add(atIndex, item)
            itemDelegates.add(atIndex, delegate)
            tryAddDelegate(delegate)
        }
        override fun <D> set(delegate: AdapterDelegate<D, Diff<D>>, item: D, atIndex: Int) {
            items[atIndex] = item
            itemDelegates[atIndex] = delegate
            tryAddDelegate(delegate)
        }
        override fun <D> addAll(delegate: AdapterDelegate<D, Diff<D>>, items: Collection<D>, atIndex: Int) {
            if (items.isNotEmpty()) {
                this.items.addAll(atIndex, items)
                (repeat ?: RepeatList<AdapterDelegate<*, *>>().also { repeat = it })
                    .of(delegate, items.size) { itemDelegates.addAll(atIndex, it) }
                tryAddDelegate(delegate)
            }
        }
        override fun addAll(items: List<Any?>, itemDelegates: List<AdapterDelegate<*, *>>, atIndex: Int) {
            // before we tryAddDelegates(), implicitly range-check `atIndex` by List internals
            this.items.addAll(atIndex, items)
            this.itemDelegates.addAll(atIndex, itemDelegates)
            // now we're safe
            for (i in itemDelegates.indices)
                tryAddDelegate(itemDelegates[i])
        }

        fun commit() {
            this@MutableDelegapter.items = items
            this@MutableDelegapter.itemDelegates = itemDelegates
        }
    }

    fun viewTypeAt(position: Int): Int =
        viewTypeMap[itemDelegates[position].create.actual]!!

    fun forViewType(viewType: Int): ViewHolderFactory =
        viewTypeList[viewType]!!.get()!!

    /**
     * Get `viewType` of the [delegate] in this Delegapter or its parent, or `-1`,
     * if it was never ever added.
     */
    fun peekViewTypeOf(delegate: AdapterDelegate<*, *>): Int =
        viewTypeMap[delegate.create.actual] ?: -1

    /**
     * Get `viewType` of the [delegate] in this Delegapter or its parent, or `-1`,
     * if it was never ever added.
     */
    fun peekViewTypeOf(factory: ViewHolderFactory): Int =
        viewTypeMap[factory.actual] ?: -1

    /**
     * Get `viewType` of the [delegate] in this Delegapter or its parent.
     * Adds [delegate] if absent.
     */
    fun forceViewTypeOf(delegate: AdapterDelegate<*, *>): Int {
        tryAddDelegate(delegate)
        return viewTypeMap[delegate.create.actual]!!
    }

}

@Suppress("UNCHECKED_CAST") private class Differ : DiffUtil.Callback() {
    @JvmField var old: Delegapter? = null
    @JvmField var new: Delegapter? = null
    override fun getOldListSize(): Int = old!!.size
    override fun getNewListSize(): Int = new!!.size
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        new!!.delegateAt(newItemPosition).let {
            it === old!!.delegateAt(oldItemPosition) &&
                (it.diff as DiffUtil.ItemCallback<Any>)
                    .areItemsTheSame(old!!.itemAt(oldItemPosition)!!, new!!.itemAt(newItemPosition)!!)
        }
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        new!!.delegateAt(newItemPosition).let {
            it === old!!.delegateAt(oldItemPosition) &&
                (it.diff as DiffUtil.ItemCallback<Any>)
                    .areContentsTheSame(old!!.itemAt(oldItemPosition)!!, new!!.itemAt(newItemPosition)!!)
        }
    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? =
        (new!!.delegateAt(newItemPosition).takeIf { it === old!!.delegateAt(oldItemPosition) }?.diff as DiffUtil.ItemCallback<Any>?)
            ?.getChangePayload(old!!.itemAt(oldItemPosition)!!, new!!.itemAt(newItemPosition)!!)
}
