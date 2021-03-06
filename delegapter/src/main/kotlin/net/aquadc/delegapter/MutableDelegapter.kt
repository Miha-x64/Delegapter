package net.aquadc.delegapter

import android.view.ViewGroup
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import kotlin.collections.set

/**
 * Mutable data structure for holding (delegate, item) pairs with agreed types.
 * @author Mike Gorünóv
 */
class MutableDelegapter(
    private val target: ListUpdateCallback,
    parent: MutableDelegapter? = null,
    initialCapacity: Int = -1,
) : Delegapter(initialCapacity) {

    constructor(target: RecyclerView.Adapter<*>, parent: MutableDelegapter? = null, initialCapacity: Int = -1) :
        this(AdapterListUpdateCallback(target), parent, initialCapacity)

    private val delegateList: RrAL<Delegate<*>>
    private val delegateTypes: HashMap<Delegate<*>, Int>

    private var repeat: RepeatList<Delegate<*>>? = null
    private var differ: Differ? = null

    init {
        if (parent == null) {
            delegateList = RrAL.create(initialCapacity)
            delegateTypes = if (initialCapacity < 0) HashMap() else HashMap(initialCapacity)
        } else {
            delegateList = parent.delegateList
            delegateTypes = parent.delegateTypes
            repeat = parent.repeat
            differ = parent.differ
        }
    }

    // configure like a MutableList

    override fun <D : Any> add(delegate: DiffDelegate<in D>, item: D, atIndex: Int): Boolean =
        add(delegate as Delegate<in D>, item, atIndex)
    @JvmOverloads fun <D> add(delegate: Delegate<in D>, item: D, atIndex: Int = size): Boolean {
        items.add(atIndex, item)
        itemDelegates.add(atIndex, delegate)
        target.onInserted(atIndex, 1)
        return tryAddDelegate(delegate)
    }

    override fun <D : Any> set(delegate: DiffDelegate<in D>, item: D, atIndex: Int): Boolean =
        set(delegate as Delegate<in D>, item, atIndex)
    @JvmOverloads fun <D> set(delegate: Delegate<in D>, item: D, atIndex: Int, payload: Any? = null): Boolean {
        items[atIndex] = item
        itemDelegates[atIndex] = delegate
        target.onChanged(atIndex, 1, payload)
        return tryAddDelegate(delegate)
    }

    override fun <D : Any> addAll(delegate: DiffDelegate<in D>, items: Collection<D>, atIndex: Int): Boolean =
        addAll(delegate as Delegate<in D>, items, atIndex)
    @JvmOverloads fun <D> addAll(delegate: Delegate<in D>, items: Collection<D>, atIndex: Int = size): Boolean =
        if (items.isEmpty()) false else {
            this.items.addAll(atIndex, items)
            (repeat ?: RepeatList<Delegate<*>>().also { repeat = it })
                .of(delegate, items.size) { itemDelegates.addAll(atIndex, it) }
            target.onInserted(atIndex, items.size)
            tryAddDelegate(delegate)
        }

    private fun tryAddDelegate(delegate: Delegate<*>) =
        if (delegateTypes.containsKey(delegate)) false
        else { delegateTypes[delegate] = delegateTypes.size; delegateList.add(delegate) }

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
        val size = items.size
        items.clear()
        itemDelegates.clear()
        target.onRemoved(0, size)
    }

    inline fun replace(detectMoves: Boolean = true, initialCapacity: Int = -1, block: DiffDelegapter.() -> Unit) {
        commit(detectMoves, DiffDelegapter(initialCapacity).apply(block))
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

    inner class DiffDelegapter @PublishedApi internal constructor(initialCapacity: Int) : Delegapter(initialCapacity) {
        override fun <D : Any> add(delegate: DiffDelegate<in D>, item: D, atIndex: Int): Boolean {
            items.add(atIndex, item)
            itemDelegates.add(atIndex, delegate)
            return tryAddDelegate(delegate)
        }
        override fun <D : Any> set(delegate: DiffDelegate<in D>, item: D, atIndex: Int): Boolean {
            items[atIndex] = item
            itemDelegates[atIndex] = delegate
            return tryAddDelegate(delegate)
        }
        override fun <D : Any> addAll(delegate: DiffDelegate<in D>, items: Collection<D>, atIndex: Int): Boolean =
            if (items.isEmpty()) false else {
                this.items.addAll(atIndex, items)
                (repeat ?: RepeatList<Delegate<*>>().also { repeat = it })
                    .of(delegate, items.size) { itemDelegates.addAll(atIndex, it) }
                tryAddDelegate(delegate)
            }

        private fun tryAddDelegate(delegate: DiffDelegate<*>) =
            if (delegateTypes.containsKey(delegate)) false
            else { delegateTypes[delegate] = delegateTypes.size; delegateList.add(delegate) }

        internal fun commit() {
            this@MutableDelegapter.items = items
            this@MutableDelegapter.itemDelegates = itemDelegates
        }
    }

    // use like an Adapter

    fun viewTypeAt(position: Int): Int =
        delegateTypes[itemDelegates[position]]!!

    fun createViewHolder(parent: ViewGroup, viewType: Int): VH<*, *, *> =
        delegateList[viewType](parent)

    fun bindViewHolder(holder: VH<*, *, *>, position: Int, payloads: List<Any> = emptyList()): Unit =
        @Suppress("UNCHECKED_CAST") (holder as VH<*, *, Any?>).bind(items[position], position, payloads)

    internal fun viewTypeFor(delegate: Delegate<*>): Int {
        tryAddDelegate(delegate)
        return delegateTypes[delegate]!!
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
