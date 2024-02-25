@file:JvmName("ViewType")
package net.aquadc.delegapter1

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.aquadc.delegapter.appendFun
import net.aquadc.delegapter.appendVHF

/**
 * Instantiates [RecyclerView.ViewHolder]s
 */
typealias ViewType = (parent: ViewGroup) -> RecyclerView.ViewHolder

private typealias VHF<VH> = (parent: ViewGroup) -> VH // make it shorter

internal class VHFMaxScrap<VH : RecyclerView.ViewHolder>(
    @JvmField internal val factory: VHF<VH>,
    @JvmField internal val maxRecycledViews: Int,
): VHF<VH> by factory {
    override fun toString(): String = buildString {
        appendVHF(factory).append('.').append("maxRecycledViews").append('(').append(maxRecycledViews).append(')')
    }
}

/**
 * Limit number of [RecyclerView.ViewHolder] instances inside [RecyclerView.RecycledViewPool].
 */
fun <VH : RecyclerView.ViewHolder> VHF<VH>.maxRecycledViews(count: Int): (parent: ViewGroup) -> VH {
    val factory = if (this is VHFMaxScrap) { if (maxRecycledViews == count) return this; factory } else this
    return if (count == 5) factory else VHFMaxScrap(factory, count)
    //                  ^- androidx.recyclerview.widget.RecyclerView.RecycledViewPool.DEFAULT_MAX_SCRAP
}

/**
 * Apply additional changes to a [RecyclerView.ViewHolder] newly instantiated by this [ViewType].
 */
fun <VH : RecyclerView.ViewHolder> VHF<VH>.then(block: VH.() -> Unit): VHF<VH> {
    val base = if (this is VHFMaxScrap) factory else this
    val func = object : VHF<VH> {
        override fun invoke(parent: ViewGroup): VH =
            base(parent).apply(block)
        override fun toString(): String =
            buildString { appendVHF(base).append('.').appendFun(block) }
// https://wiki.haskell.org/Function_composition -^
    }
    return if (this is VHFMaxScrap) func.maxRecycledViews(this.maxRecycledViews) else func
}

/**
 * Create [AdapterDelegate] from [ViewType] and [bind]ing function.
 */
inline fun <VH : RecyclerView.ViewHolder, T> ((parent: ViewGroup) -> VH).bind(
    crossinline bind: VH.(item: T, payloads: List<Any>) -> Unit,
): AdapterDelegate<T, Nothing?> = object : AdapterDelegate<T, Nothing?>(this, null) {
    override fun bind(viewHolder: RecyclerView.ViewHolder, item: T, payloads: List<Any>): Unit =
        // VH type is AD's implementation detail not visible through type
        @Suppress("UNCHECKED_CAST") (viewHolder as VH).bind(item, payloads)
}

/**
 * Create [AdapterDelegate]<[Unit]> from [ViewType].
 */
fun ViewType.bind(): AdapterDelegate<Unit, Nothing?> =
    object : AdapterDelegate<Unit, Nothing?>(this, null) {
        override fun bind(viewHolder: RecyclerView.ViewHolder, item: Unit, payloads: List<Any>): Unit =
            Unit
    }

/**
 * Create [AdapterDelegate] from [ViewType] and [bind]ing function.
 */
@JvmName("bindWithRecycleHook")
inline fun <VH, T> ((parent: ViewGroup) -> VH).bind(
    crossinline bind: VH.(item: T, payloads: List<Any>) -> Unit,
): AdapterDelegate<T, Nothing?> where VH : RecyclerView.ViewHolder, VH : Recyclable =
    @Suppress("UNCHECKED_CAST") // VH type is AD's implementation detail not visible through type
    object : AdapterDelegate<T, Nothing?>(this, null) {
        override fun bind(viewHolder: RecyclerView.ViewHolder, item: T, payloads: List<Any>): Unit =
            (viewHolder as VH).bind(item, payloads)
        override fun recycled(viewHolder: RecyclerView.ViewHolder): Unit =
            (viewHolder as VH).recycle()
    }
