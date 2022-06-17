package net.aquadc.delegapter.adapter

import android.view.ViewGroup
import net.aquadc.delegapter.Delegapter
import net.aquadc.delegapter.MutableDelegapter
import net.aquadc.delegapter.VH

/**
 * An adapter implementation with Delegapter inside.
 */
open class DelegatedAdapter @JvmOverloads constructor(
    parent: MutableDelegapter? = null,
    initialCapacity: Int = -1,
) : VHAdapter<VH<*, *, *>>() {

    @JvmField val data = Delegapter(this, parent, initialCapacity)

    final override fun getItemCount(): Int =
        data.size

    final override fun getItemViewType(position: Int): Int =
        data.viewTypeAt(position)

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH<*, *, *> =
        data.createViewHolder(parent, viewType)

    final override fun onBindViewHolder(holder: VH<*, *, *>, position: Int, payloads: List<Any>): Unit =
        data.bindViewHolder(holder, position, payloads)

}