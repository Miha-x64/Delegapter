@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("DiffAdapterDelegate")
package net.aquadc.delegapter1

import androidx.recyclerview.widget.DiffUtil
import net.aquadc.delegapter.EqualityDiff


internal typealias Diff<T> = DiffUtil.ItemCallback<in T> // just to make it shorter

/**
 * Join this [AdapterDelegate] with the given [DiffUtil.ItemCallback].
 */
@Deprecated("Renamed to times because technically it's product", ReplaceWith("this * diff"))
inline operator fun <T> AdapterDelegate<T, Nothing?>.plus(diff: Diff<T>): AdapterDelegate<T, Diff<T>> =
    AdapterDelegateDecorator(create, diff, this)

/**
 * Join this [AdapterDelegate] with the given [DiffUtil.ItemCallback].
 */
@JvmName("of")
inline operator fun <T> AdapterDelegate<T, Nothing?>.times(diff: Diff<T>): AdapterDelegate<T, Diff<T>> =
    AdapterDelegateDecorator(create, diff, this)

/**
 * Replace this [AdapterDelegate.diff] with [diff] instance.
 * Like [times] but can be applied to any [AdapterDelegate], without `D=Nothing?` requirement.
 */
fun <T, D : Diff<T>?> AdapterDelegate<T, *>.copy(diff: D): AdapterDelegate<T, D> =
    AdapterDelegateDecorator(create, diff, this)

/**
 * Gathers [DiffUtil.ItemCallback] and adds it to this [AdapterDelegate] from separate functions.
 */
@JvmName("from")
inline fun <T> AdapterDelegate<T, Nothing?>.diff(
    crossinline areItemsTheSame: (oldItem: T & Any, newItem: T & Any) -> Boolean = { _, _ -> true },
    crossinline areContentsTheSame: (oldItem: T & Any, newItem: T & Any) -> Boolean = Any::equals,
    crossinline getChangePayload: (oldItem: T & Any, newItem: T & Any) -> Any? = { _, _ -> null }
): AdapterDelegate<T, Diff<T>> = AdapterDelegateDecorator(create, object : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T & Any, newItem: T & Any): Boolean =
        areItemsTheSame.invoke(oldItem, newItem)
    override fun areContentsTheSame(oldItem: T & Any, newItem: T & Any): Boolean =
        areContentsTheSame.invoke(oldItem, newItem)
    override fun getChangePayload(oldItem: T & Any, newItem: T & Any): Any? =
        getChangePayload.invoke(oldItem, newItem)
}, this)

/**
 * Adds default [DiffUtil.ItemCallback] to this [AdapterDelegate]
 * implementing [DiffUtil.ItemCallback.areItemsTheSame] as `return true`
 * and [DiffUtil.ItemCallback.areContentsTheSame] as [Any.equals].
 */
@JvmName("equality")
inline fun <T> AdapterDelegate<T, Nothing?>.diff(): AdapterDelegate<T, Diff<T>> =
    AdapterDelegateDecorator(create, EqualityDiff, this)
