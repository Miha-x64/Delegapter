@file:JvmName("Adapters")
package net.aquadc.delegapter.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.aquadc.delegapter.Delegate
import net.aquadc.delegapter.MutableDelegapter
import net.aquadc.delegapter.VH

/**
 * Adapter for a single viewType and item repeated several times.
 * @author Mike Gorünóv
 */
open class RepeatAdapter<D>(
    private val delegate: Delegate<D>,
    private val item: D,
    size: Int = 1,
    parent: MutableDelegapter? = null,
) : VHAdapter<VH<*, *, D>>() {

    var size: Int = size
        set(value) {
            if (field != value) {
                if (field > value) notifyItemRangeRemoved(value, field - value)
                else notifyItemRangeInserted(field, value - field)
                field = value
            }
        }

    private val viewType =
        parent?.viewTypeFor(delegate) ?: 0

    override fun getItemCount(): Int =
        size

    override fun getItemViewType(position: Int): Int =
        viewType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH<*, *, D> =
        delegate(parent)

    override fun onBindViewHolder(holder: VH<*, *, D>, position: Int, payloads: List<Any>): Unit =
        holder.bind(item, position, payloads)

}

/**
 * Adapter for a single [View].
 * @author Mike Gorünóv
 */
@Suppress("FunctionName")
@JvmName("singleItem")
fun SingleItemAdapter(view: View): RecyclerView.Adapter<*> =
    RepeatAdapter({ VH(view) }, Unit, 1)
