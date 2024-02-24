@file:JvmName("Adapters")
package net.aquadc.delegapter1.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.aquadc.delegapter1.MutableDelegapter
import net.aquadc.delegapter1.VH
import net.aquadc.delegapter1.bind


@JvmName("delegated")
inline fun DelegatedAdapter(
    parent: MutableDelegapter? = null,
    initialCapacity: Int = -1,
    init: MutableDelegapter.() -> Unit,
): RecyclerView.Adapter<RecyclerView.ViewHolder> =
    DelegatedAdapter(parent, initialCapacity).apply { data.apply(init) }

/**
 * Adapter for a single [View].
 * @author Mike Gorünóv
 */
@Suppress("FunctionName")
@JvmName("singleItem")
fun SingleItemAdapter(view: View): RecyclerView.Adapter<*> =
    RepeatAdapter({ _: ViewGroup -> VH(view) }.bind(), 1)


