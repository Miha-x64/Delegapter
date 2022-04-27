package net.aquadc.delegapter.adapter

import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView.Adapter
import net.aquadc.delegapter.Delegate
import net.aquadc.delegapter.MutableDelegapter
import net.aquadc.delegapter.VH
import net.aquadc.delegapter.commitRemovals
import net.aquadc.delegapter.markForRemoval
import java.util.function.Predicate

class SingleTypeAdapter<D>(
    private val delegate: Delegate<D>,
    items: List<D> = emptyList(),
    parent: MutableDelegapter? = null,
) : VHAdapter<VH<*, *, D>>() {

    private val viewType = parent?.viewTypeFor(delegate) ?: 0

    val items: MutableList<D> = ObservableList(ArrayList(items), this)

    override fun getItemCount(): Int =
        items.size

    override fun getItemViewType(position: Int): Int =
        viewType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH<*, *, D> =
        delegate(parent)

    override fun onBindViewHolder(holder: VH<*, *, D>, position: Int, payloads: List<Any>): Unit =
        holder.bind(items[position], position, payloads)

}

private class ObservableList<D>(
    private val list: ArrayList<D>,
    private val callback: Adapter<*>, // maybe use ListUpdateCallback and make this class public?
) : AbstractMutableList<D>() {

    override val size: Int
        get() = list.size

    override fun get(index: Int): D =
        list[index]

    override fun add(index: Int, element: D) {
        list.add(index, element)
        callback.notifyItemInserted(index)
    }

    override fun addAll(index: Int, elements: Collection<D>): Boolean {
        val a = list.addAll(index, elements)
        callback.notifyItemRangeInserted(index, elements.size)
        return a
    }

    override fun set(index: Int, element: D): D {
        val s = list.set(index, element)
        callback.notifyItemChanged(index)
        return s
    }

    override fun removeAt(index: Int): D {
        val r = list.removeAt(index)
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
            list.markForRemoval(i)
            callback.notifyItemRemoved(i - removed++)
        }
        return if (removed > 0) {
            list.commitRemovals()
            true
        } else false
    }

    override fun removeRange(fromIndex: Int, toIndex: Int) {
        (if (fromIndex == 0 && toIndex == list.size) list else list.subList(fromIndex, toIndex)).clear()
        callback.notifyItemRangeRemoved(fromIndex, toIndex)
    }
}
