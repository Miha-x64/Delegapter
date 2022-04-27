package net.aquadc.delegapter.adapter

import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView
import net.aquadc.delegapter.VH


abstract class VHAdapter<VHT : VH<*, *, *>> : RecyclerView.Adapter<VHT>() {

    // useless overload, never called by RecyclerView
    final override fun onBindViewHolder(holder: VHT, position: Int): Unit =
        onBindViewHolder(holder, position, emptyList())

    // useful overload, implement it plz
    abstract override fun onBindViewHolder(holder: VHT, position: Int, payloads: List<Any>)

    @CallSuper override fun onViewRecycled(holder: VHT): Unit =
        holder.recycle()

}
