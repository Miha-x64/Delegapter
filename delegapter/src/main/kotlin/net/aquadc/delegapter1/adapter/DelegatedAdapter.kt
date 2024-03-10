package net.aquadc.delegapter1.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.aquadc.delegapter1.Delegapter
import net.aquadc.delegapter1.MutableDelegapter

/**
 * An adapter implementation with [Delegapter] inside.
 * @author Mike Gorünóv
 */
open class DelegatedAdapter @JvmOverloads constructor(
    parent: MutableDelegapter? = null,
    initialCapacity: Int = -1,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    @JvmField val data = Delegapter(this, parent, initialCapacity)

    override fun getItemCount(): Int =
        data.size

    override fun getItemViewType(position: Int): Int =
        data.viewTypeAt(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        data.forViewType(viewType)(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int): Unit =
        throw AssertionError()

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>): Unit =
        data.bindViewHolder(holder, position, payloads)

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) =
        data.recycled(holder)

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        recyclerView.setRecycledViewPool(data.recycledViewPool)
    }

}