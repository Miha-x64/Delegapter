package net.aquadc.delegapter.adapter

import android.view.ViewGroup
import net.aquadc.delegapter.Delegapter
import net.aquadc.delegapter.MutableDelegapter
import net.aquadc.delegapter.VH

/**
 * An adapter implementation with [Delegapter] inside.
 * @author Mike Gorünóv
 */
open class DelegatedAdapter @JvmOverloads constructor(
    parent: MutableDelegapter? = null,
    initialCapacity: Int = -1,
) : VHAdapter<VH<*, *, *>>() {

    @JvmField val data = Delegapter(this, parent, initialCapacity)

    override fun getItemCount(): Int =
        data.size

    override fun getItemViewType(position: Int): Int =
        data.viewTypeAt(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH<*, *, *> =
        data.createViewHolder(parent, viewType)

    override fun onBindViewHolder(holder: VH<*, *, *>, position: Int, payloads: List<Any>): Unit =
        data.bindViewHolder(holder, position, payloads)

}