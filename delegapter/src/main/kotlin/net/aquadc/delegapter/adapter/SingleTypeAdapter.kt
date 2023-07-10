package net.aquadc.delegapter.adapter

import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import net.aquadc.delegapter.Delegate
import net.aquadc.delegapter.DiffDelegate
import net.aquadc.delegapter.MutableDelegapter
import net.aquadc.delegapter.RemoveRangeArrayList
import net.aquadc.delegapter.RemoveRangeMutableList
import net.aquadc.delegapter.VH
import net.aquadc.delegapter.commitRemovals
import net.aquadc.delegapter.markForRemoval
import java.util.function.Predicate

/**
 * Adapter for a single viewType.
 * @author Mike Gorünóv
 */
open class SingleTypeAdapter<D>(
    @JvmField protected val delegate: Delegate<D>,
    items: List<D> = emptyList(),
    parent: MutableDelegapter? = null,
) : VHAdapter<VH<*, *, D>>() {

    private val viewType = parent?.viewTypeFor(delegate) ?: 0

    open val items: RemoveRangeMutableList<D> = ObservableList(items, this)

    override fun getItemCount(): Int =
        items.size

    override fun getItemViewType(position: Int): Int =
        viewType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH<*, *, D> =
        delegate(parent)

    override fun onBindViewHolder(holder: VH<*, *, D>, position: Int, payloads: List<Any>): Unit =
        holder.bind(items[position], position, payloads)

}

open class SingleTypeDiffAdapter<D : Any>(
    delegate: DiffDelegate<D>,
    items: List<D> = emptyList(),
    parent: MutableDelegapter? = null,
) : SingleTypeAdapter<D>(delegate, items, parent) {

    private var differ: Differ<D>? = null

    override var items: RemoveRangeMutableList<D>
        get() = super.items
        set(value/*: wannabe List<D>*/) { setItems(value) }

    fun setItems(items: List<D>, detectMoves: Boolean = true) {
        when {
            super.items.isEmpty() -> {
                (super.items as ObservableList).list = items
                notifyItemRangeInserted(0, items.size)
            }
            items.isEmpty() ->
                super.items.clear()
            else -> {
                @Suppress("UNCHECKED_CAST")
                val differ = differ ?: Differ(delegate as DiffUtil.ItemCallback<D>).also { differ = it }
                differ.old = (super.items as ObservableList).list
                differ.new = items
                DiffUtil.calculateDiff(differ, detectMoves).dispatchUpdatesTo(this)
                differ.old = null
                differ.new = null
                (super.items as ObservableList).list = items
            }
        }
    }

}

private class Differ<T : Any>(private val itemCallback: DiffUtil.ItemCallback<T>) : DiffUtil.Callback() {
    @JvmField var old: List<T>? = null
    @JvmField var new: List<T>? = null
    override fun getOldListSize(): Int = old!!.size
    override fun getNewListSize(): Int = new!!.size
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        itemCallback.areItemsTheSame(old!![oldItemPosition], new!![newItemPosition])

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        itemCallback.areContentsTheSame(old!![oldItemPosition], new!![newItemPosition])

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? =
        itemCallback.getChangePayload(old!![oldItemPosition], new!![newItemPosition])
}


private class ObservableList<D>(
    @JvmField var list: List<D>,
    private val callback: Adapter<*>, // maybe use ListUpdateCallback and make this class public?
) : AbstractMutableList<D>(), RemoveRangeMutableList<D> {

    private val mutableList get() =
        (list as? RemoveRangeArrayList) ?: RemoveRangeArrayList(list).also { list = it }

    override val size: Int
        get() = list.size

    override fun get(index: Int): D =
        list[index]

    override fun add(index: Int, element: D) {
        mutableList.add(index, element)
        callback.notifyItemInserted(index)
    }

    override fun addAll(index: Int, elements: Collection<D>): Boolean {
        val a = mutableList.addAll(index, elements)
        callback.notifyItemRangeInserted(index, elements.size)
        return a
    }

    override fun set(index: Int, element: D): D {
        val s = mutableList.set(index, element)
        callback.notifyItemChanged(index)
        return s
    }

    override fun removeAt(index: Int): D {
        val r = mutableList.removeAt(index)
        callback.notifyItemRemoved(index)
        return r
    }

    override fun removeAll(elements: Collection<D>): Boolean =
        batchRemove(elements, false)

    @RequiresApi(24) override fun removeIf(filter: Predicate<in D>): Boolean =
        batchRemoveIf { filter.test(list[it]) }

    override fun retainAll(elements: Collection<D>): Boolean =
        batchRemove(elements, true)

    private fun batchRemove(elements: Collection<Any?>, complement: Boolean): Boolean =
        batchRemoveIf { elements.contains(list[it]) != complement }

    private inline fun batchRemoveIf(predicate: (Int) -> Boolean): Boolean {
        var removed = 0
        for (i in list.indices) if (predicate(i)) {
            mutableList.markForRemoval(i)
            callback.notifyItemRemoved(i - removed++)
        }
        return if (removed > 0) {
            mutableList.commitRemovals()
            true
        } else false
    }

    override fun removeRange(fromIndex: Int, toIndex: Int) {
        val list = list
        when {
            list is RemoveRangeArrayList -> list.removeRange(fromIndex, toIndex)
            // avoid touching removed items:
            fromIndex == 0 -> this.list = if (toIndex == list.size) emptyList() else list.subList(toIndex, list.size)
            toIndex == list.size -> this.list = list.subList(0, fromIndex)
            else -> {
                val newList = RemoveRangeArrayList<D>(list.lastIndex)
                if (fromIndex == 1)
                    newList.add(list[0]) else newList.addAll(list.subList(0, fromIndex))
                if (toIndex == list.lastIndex)
                    newList.add(list.last()) else newList.addAll(list.subList(toIndex, list.size))
                this.list = newList
            }
        }
        callback.notifyItemRangeRemoved(fromIndex, toIndex)
    }
}
