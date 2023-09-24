package net.aquadc.delegapter1.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import net.aquadc.delegapter.RemoveRangeMutableList
import net.aquadc.delegapter.adapter.Differ
import net.aquadc.delegapter.adapter.ObservableList
import net.aquadc.delegapter1.AdapterDelegate
import net.aquadc.delegapter1.Diff
import net.aquadc.delegapter1.MutableDelegapter

/**
 * Adapter for a single viewType.
 * @author Mike Gorünóv
 */
open class SingleTypeAdapter<T>(
    @JvmField protected val delegate: AdapterDelegate<T, *>,
    items: List<T> = emptyList(),
    parent: MutableDelegapter? = null,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val viewType = parent?.forceViewTypeOf(delegate) ?: 0

    open val items: RemoveRangeMutableList<T> = ObservableList(items, this)

    override fun getItemCount(): Int =
        items.size

    override fun getItemViewType(position: Int): Int =
        viewType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        delegate.create(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int): Unit =
        throw AssertionError() // unused

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>): Unit =
        delegate.bind(holder, items[position], payloads)

    override fun onViewRecycled(holder: RecyclerView.ViewHolder): Unit =
        delegate.recycled(holder)

}

/**
 * Adapter for a single diffable viewType.
 * @author Mike Gorünóv
 */
open class SingleTypeDiffAdapter<T>(
    delegate: AdapterDelegate<T, Diff<T>>,
    items: List<T> = emptyList(),
    parent: MutableDelegapter? = null,
) : SingleTypeAdapter<T>(delegate, items, parent) {

    private var differ: Differ<T>? = null

    override var items: RemoveRangeMutableList<T>
        get() = super.items
        set(value/*: wannabe List<T>*/) { replace(value) }

    open fun replace(items: List<T>, detectMoves: Boolean = true) {
        when {
            super.items.isEmpty() -> {
                (super.items as ObservableList).list = items
                notifyItemRangeInserted(0, items.size)
            }
            items.isEmpty() ->
                super.items.clear()
            else -> {
                val differ = differ
                    ?: Differ(@Suppress("UNCHECKED_CAST") (delegate as AdapterDelegate<T, Diff<T>>).diff)
                        .also { differ = it }
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
