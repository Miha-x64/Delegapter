@file:JvmName("Adapters")
package net.aquadc.delegapter1.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.aquadc.delegapter1.AdapterDelegate
import net.aquadc.delegapter1.MutableDelegapter
import net.aquadc.delegapter1.VH
import net.aquadc.delegapter1.bind

/**
 * Adapter for a single viewType and item repeated several times.
 * @author Mike Gorünóv
 */
open class RepeatAdapter(
    private val delegate: AdapterDelegate<Unit, *>,
    size: Int = 1,
    parent: MutableDelegapter? = null,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var size: Int = size
        set(value) {
            if (field != value) {
                if (field > value) notifyItemRangeRemoved(value, field - value)
                else notifyItemRangeInserted(field, value - field)
                field = value
            }
        }

    private val viewType =
        parent?.forceViewTypeOf(delegate) ?: 0

    override fun getItemCount(): Int =
        size

    override fun getItemViewType(position: Int): Int =
        viewType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        delegate.create(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int): Unit =
        throw AssertionError()

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>): Unit =
        delegate.bind(holder, Unit, payloads)

}

/**
 * Adapter for a single [View].
 * @author Mike Gorünóv
 */
@Suppress("FunctionName")
@JvmName("singleItem")
fun SingleItemAdapter(view: View): RecyclerView.Adapter<*> =
    RepeatAdapter({ _: ViewGroup -> VH(view) }.bind(), 1)
