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
 * Base class for homogenous (based on single [AdapterDelegate]) adapters
 * @author Mike Gorünóv
 */
abstract class SingleDelegateAdapter<T>(
    @JvmField protected val delegate: AdapterDelegate<T, *>,
    parent: MutableDelegapter?,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val recycledViewPool = parent?.recycledViewPool

    private val viewType =
        parent?.forceViewTypeOf(delegate) ?: 0

    override fun getItemViewType(position: Int): Int =
        viewType

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        if (recycledViewPool != null)
            recyclerView.setRecycledViewPool(recycledViewPool)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        delegate.create(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int): Unit =
        throw AssertionError()

    override fun onViewRecycled(holder: RecyclerView.ViewHolder): Unit =
        delegate.recycled(holder)

}

/**
 * Adapter for a single viewType and item repeated several times
 * @author Mike Gorünóv
 */
open class RepeatAdapter(
    delegate: AdapterDelegate<Unit, *>,
    size: Int = 1,
    parent: MutableDelegapter? = null,
) : SingleDelegateAdapter<Unit>(delegate, parent) {

    var size: Int = size
        set(value) {
            if (field != value) {
                if (field > value) notifyItemRangeRemoved(value, field - value)
                else notifyItemRangeInserted(field, value - field)
                field = value
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>): Unit =
        delegate.bind(holder, Unit, payloads)

    override fun getItemCount(): Int =
        size

}

/**
 * List adapter for a single viewType
 * @author Mike Gorünóv
 */
open class SingleTypeAdapter<T>(
    delegate: AdapterDelegate<T, *>,
    items: List<T> = emptyList(),
    parent: MutableDelegapter? = null,
) : SingleDelegateAdapter<T>(delegate, parent) {

    open val items: RemoveRangeMutableList<T> = ObservableList(items, this)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>): Unit =
        delegate.bind(holder, items[position], payloads)

    override fun getItemCount(): Int =
        items.size

}

/**
 * Adapter for a single diffable viewType
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
