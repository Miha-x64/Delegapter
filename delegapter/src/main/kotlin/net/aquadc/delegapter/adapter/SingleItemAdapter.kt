@file:Suppress("FunctionName")

package net.aquadc.delegapter.adapter

import android.view.View
import android.view.ViewGroup
import net.aquadc.delegapter.Delegate
import net.aquadc.delegapter.MutableDelegapter
import net.aquadc.delegapter.VH

class SingleItemAdapter<D>(
    private val delegate: Delegate<D>,
    private val item: D,
    parent: MutableDelegapter? = null,
) : VHAdapter<VH<*, *, D>>() {

    private val viewType =
        parent?.viewTypeFor(delegate) ?: 0

    override fun getItemCount(): Int =
        1

    override fun getItemViewType(position: Int): Int =
        viewType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH<*, *, D> =
        delegate(parent)

    override fun onBindViewHolder(holder: VH<*, *, D>, position: Int, payloads: List<Any>): Unit =
        holder.bind(item, position, payloads)

}

fun SingleItemAdapter(view: View): SingleItemAdapter<Unit> =
    SingleItemAdapter({ VH(view) }, Unit)
