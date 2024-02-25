@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.delegapter1

import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import net.aquadc.delegapter.appendVHF


/**
 * A thing to delegate some [RecyclerView.Adapter] methods to,
 * specifically [RecyclerView.Adapter.getItemViewType], [RecyclerView.Adapter.onCreateViewHolder]
 * and [RecyclerView.Adapter.onBindViewHolder].
 * @author Mike Gorünóv
 */
abstract class AdapterDelegate<in T, D : Diff<T>?>(
    /**
     * Create [RecyclerView.ViewHolder] and associated [View].
     * Called from [RecyclerView.Adapter.onCreateViewHolder].
     * [Equal][Any.equals] instances share the same [view type][RecyclerView.Adapter.getItemViewType].
     */
    @JvmField val create: ViewType,

    /**
     * Optional [DiffUtil.ItemCallback] instance.
     */
    @JvmField val diff: D,
) {
    /**
     * Bind [item] to [viewHolder].
     * Called from [RecyclerView.Adapter.onBindViewHolder].
     * The [viewHolder] is guaranteed to be the one created by [create] function.
     */
    abstract fun bind(viewHolder: RecyclerView.ViewHolder, item: T, payloads: List<Any>)

    /**
     * Free the data associated with [viewHolder] as it goes to the [RecyclerView.getRecycledViewPool].
     * Called from [RecyclerView.Adapter.onViewRecycled].
     */
    open fun recycled(viewHolder: RecyclerView.ViewHolder) {}

    /**
     * String representation of an [AdapterDelegate] like this
     * but with the given [ViewHolder factory][create] and [diff].
     */
    protected open fun toString(create: ViewType, diff: Diff<*>?): String = buildString {
        append("AD").append('(').appendVHF(create).append(')')
        if (diff != null) append(" ≏ ").append(diff)
    }

    /**
     * Call other instance's protected function bypassing Kotlin access rules.
     */
    protected inline fun AdapterDelegate<*, *>.toString(noinline create: ViewType, diff: Diff<*>?): String =
        toString(create, diff)

    final override fun toString(): String =
        toString(create, diff)
}
