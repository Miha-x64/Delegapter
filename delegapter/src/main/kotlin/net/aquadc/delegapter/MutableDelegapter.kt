package net.aquadc.delegapter

import android.view.ViewGroup
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import kotlin.collections.set

/**
 * @author Mike Gorünóv
 */
class MutableDelegapter(
    private val target: ListUpdateCallback,
    parent: MutableDelegapter? = null,
    initialCapacity: Int = -1,
) : Delegapter(initialCapacity) {

    constructor(target: RecyclerView.Adapter<*>, parent: MutableDelegapter? = null, initialCapacity: Int = -1) :
        this(AdapterListUpdateCallback(target), parent, initialCapacity)

    private val delegateList: ArrayList<Delegate<*>>
    private val delegateTypes: HashMap<Delegate<*>, Int>
    init {
        if (parent == null) {
            delegateList = newArrayList(initialCapacity)
            delegateTypes = if (initialCapacity < 0) HashMap() else HashMap(initialCapacity)
        } else {
            delegateList = parent.delegateList
            delegateTypes = parent.delegateTypes
        }
    }

    // configure like a MutableList

    fun <D> add(item: D, delegate: Delegate<in D>): Boolean =
        addAt(items.size, item, delegate)
    override fun <D : Any> addAt(index: Int, item: D, delegate: DiffDelegate<in D>): Boolean =
        addAt(index, item, delegate as Delegate<in D>)
    fun <D> addAt(index: Int, item: D, delegate: Delegate<in D>): Boolean {
        items.add(index, item)
        itemDelegates.add(index, delegate)
        target.onInserted(index, 1)
        return tryAddDelegate(delegate)
    }

    override fun <D : Any> set(index: Int, item: D, delegate: DiffDelegate<in D>): Boolean =
        set(index, item, delegate as Delegate<in D>)
    fun <D> set(index: Int, item: D, delegate: Delegate<in D>, payload: Any? = null): Boolean {
        items[index] = item
        itemDelegates[index] = delegate
        target.onChanged(index, 1, payload)
        return tryAddDelegate(delegate)
    }

    private var repeat: RepeatList<Delegate<*>>? = null
    fun <D> addAll(items: Collection<D>, delegate: Delegate<in D>): Boolean =
        addAllAt(this.items.size, items, delegate)
    override fun <D : Any> addAllAt(index: Int, items: Collection<D>, delegate: DiffDelegate<in D>): Boolean =
        addAllAt(index, items, delegate as Delegate<in D>)
    fun <D> addAllAt(index: Int, items: Collection<D>, delegate: Delegate<in D>): Boolean =
        if (items.isEmpty()) false else {
            this.items.addAll(index, items)
            (repeat ?: RepeatList<Delegate<*>>().also { repeat = it })
                .of(delegate, items.size) { itemDelegates.addAll(index, it) }
            target.onInserted(index, items.size)
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
        items.subList(start, endEx).clear()
        itemDelegates.subList(start, endEx).clear()
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

    private var differ: Differ? = null
    @PublishedApi internal fun commit(detectMoves: Boolean, tmp: DiffDelegapter) {
        val differ = differ ?: Differ(this).also { differ = it }
        differ.new = tmp
        DiffUtil.calculateDiff(differ, detectMoves).dispatchUpdatesTo(target)
        differ.new = null
        tmp.commit()
    }

    inner class DiffDelegapter @PublishedApi internal constructor(initialCapacity: Int) : Delegapter(initialCapacity) {
        override fun <D : Any> addAt(index: Int, item: D, delegate: DiffDelegate<in D>): Boolean {
            items.add(index, item)
            itemDelegates.add(index, delegate)
            return tryAddDelegate(delegate)
        }
        override fun <D : Any> set(index: Int, item: D, delegate: DiffDelegate<in D>/*, payload: Any? = null*/): Boolean {
            items[index] = item
            itemDelegates[index] = delegate
            return tryAddDelegate(delegate)
        }

        override fun <D : Any> addAllAt(index: Int, items: Collection<D>, delegate: DiffDelegate<in D>): Boolean =
            if (items.isEmpty()) false else {
                this.items.addAll(index, items)
                (repeat ?: RepeatList<Delegate<*>>().also { repeat = it })
                    .of(delegate, items.size) { itemDelegates.addAll(index, it) }
                tryAddDelegate(delegate)
            }

        private fun tryAddDelegate(delegate: DiffDelegate<*>) =
            if (delegateTypes.containsKey(delegate)) false
            else { delegateTypes[delegate] = delegateTypes.size; delegateList.add(delegate) }

        /*fun remove(element: Any?): Boolean {
            val iof = items.indexOf(element)
            return if (iof < 0) false else { removeAt(iof); true }
        }
        fun removeAt(position: Int) {
            items.removeAt(position)
            itemDelegates.removeAt(position)
        }
        fun removeRange(start: Int, endEx: Int) {
            items.subList(start, endEx).clear()
            itemDelegates.subList(start, endEx).clear()
        }
        fun removeAll(elements: Collection<Any?>): Boolean = batchRemove(elements, false)
        fun retainAll(elements: Collection<Any?>): Boolean = batchRemove(elements, true)
        private fun batchRemove(elements: Collection<Any?>, complement: Boolean): Boolean =
            batchRemoveIf { elements.contains(items[it]) != complement }
        @JvmName("removeAllBy") fun removeAll(delegate: DiffDelegate<*>): Boolean = batchRemoveBy(delegate, false)
        @JvmName("retainAllBy") fun retainAll(delegate: DiffDelegate<*>): Boolean = batchRemoveBy(delegate, true)
        private fun batchRemoveBy(delegate: DiffDelegate<*>, complement: Boolean): Boolean =
            batchRemoveIf { (itemDelegates[it] == delegate) != complement }
        @JvmName("removeAllBy") fun removeAll(delegates: Collection<DiffDelegate<*>>): Boolean = batchRemoveBy(delegates, false)
        @JvmName("retainAllBy") fun retainAll(delegates: Collection<DiffDelegate<*>>): Boolean = batchRemoveBy(delegates, true)
        private fun batchRemoveBy(delegates: Collection<DiffDelegate<*>>, complement: Boolean): Boolean =
            batchRemoveIf { delegates.contains(itemDelegates[it]) != complement }
        private inline fun batchRemoveIf(predicate: (Int) -> Boolean): Boolean {
            var removed = 0
            for (i in itemDelegates.indices) if (predicate(i)) {
                items.markForRemoval(i)
                itemDelegates.markForRemoval(i)
                removed++
            }
            return if (removed > 0) {
                items.commitRemovals()
                itemDelegates.commitRemovals()
                true
            } else false
        }*/
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

@Suppress("UNCHECKED_CAST") private class Differ(
    private val old: MutableDelegapter,
) : DiffUtil.Callback() {
    @JvmField var new: MutableDelegapter.DiffDelegapter? = null
    override fun getOldListSize(): Int = old.size
    override fun getNewListSize(): Int = new!!.size
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        new!!.delegateAt(newItemPosition).let {
            it == old.delegateAt(oldItemPosition) &&
                (it as DiffUtil.ItemCallback<Any>)
                    .areItemsTheSame(old.itemAt(oldItemPosition)!!, new!!.itemAt(newItemPosition)!!)
        }
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        new!!.delegateAt(newItemPosition).let {
            it == old.delegateAt(oldItemPosition) &&
                (it as DiffUtil.ItemCallback<Any>)
                    .areContentsTheSame(old.itemAt(oldItemPosition)!!, new!!.itemAt(newItemPosition)!!)
        }
    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? =
        (new!!.delegateAt(newItemPosition).takeIf { it == old.delegateAt(oldItemPosition) } as DiffUtil.ItemCallback<Any>?)
            ?.getChangePayload(old.itemAt(oldItemPosition)!!, new!!.itemAt(newItemPosition)!!)
}
