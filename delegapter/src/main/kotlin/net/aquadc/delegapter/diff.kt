package net.aquadc.delegapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil

/**
 * A delegate which supports diffing.
 */
abstract class DiffDelegate<D> : DiffUtil.ItemCallback<D>(), (ViewGroup) -> VH<*, *, D> // Delegate<D>

/**
 * Creates a [DiffDelegate] from [this] one using [itemDiffer].
 */
operator fun <D> ((ViewGroup) -> VH<*, *, D>).plus(itemDiffer: DiffUtil.ItemCallback<in D>): DiffDelegate<D> =
    object : DelegatedDiffDelegate<D>(this) {
        override fun areItemsTheSame(oldItem: D & Any, newItem: D & Any): Boolean =
            itemDiffer.areItemsTheSame(oldItem, newItem)
        override fun areContentsTheSame(oldItem: D & Any, newItem: D & Any): Boolean =
            itemDiffer.areContentsTheSame(oldItem, newItem)
        override fun getChangePayload(oldItem: D & Any, newItem: D & Any): Any? =
            itemDiffer.getChangePayload(oldItem, newItem)
    }

/**
 * Creates a [DiffDelegate] from [this] one
 * accepting separate [areItemsTheSame], [areContentsTheSame], and [getChangePayload] functions.
 */
inline fun <D> ((ViewGroup) -> VH<*, *, D>).diff(
    crossinline areItemsTheSame: (oldItem: D & Any, newItem: D & Any) -> Boolean = { _, _ -> true },
    crossinline areContentsTheSame: (oldItem: D & Any, newItem: D & Any) -> Boolean = Any::equals,
    crossinline getChangePayload: (oldItem: D & Any, newItem: D & Any) -> Any? = { _, _ -> null }
): DiffDelegate<D> = object : DelegatedDiffDelegate<D>(this) {
    override fun areItemsTheSame(oldItem: D & Any, newItem: D & Any): Boolean =
        areItemsTheSame.invoke(oldItem, newItem)
    override fun areContentsTheSame(oldItem: D & Any, newItem: D & Any): Boolean =
        areContentsTheSame.invoke(oldItem, newItem)
    override fun getChangePayload(oldItem: D & Any, newItem: D & Any): Any? =
        getChangePayload.invoke(oldItem, newItem)
}

/**
 * Creates a [DiffDelegate] from [this] one using equality check.
 * Short-circuit overload avoiding extra class.
 */
fun <D> ((ViewGroup) -> VH<*, *, D>).diff(): DiffDelegate<D> = this + EqualityDiff

@PublishedApi internal object EqualityDiff : DiffUtil.ItemCallback<Any?>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean = true
    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean = oldItem == newItem
    override fun toString(): String = "equality"
}

/**
 * Avoids overriding existing diffing machinery of [this] delegate.
 */
@Suppress("DeprecatedCallableAddReplaceWith", "UnusedReceiverParameter")
@Deprecated("Why you wanna do this?!", level = DeprecationLevel.ERROR)
inline fun <D> DiffDelegate<D>.diff(
    crossinline areItemsTheSame: (oldItem: D & Any, newItem: D & Any) -> Boolean = { _, _ -> true },
    crossinline areContentsTheSame: (oldItem: D & Any, newItem: D & Any) -> Boolean = Any::equals,
    crossinline getChangePayload: (oldItem: D & Any, newItem: D & Any) -> Any? = { _, _ -> null }
): DiffDelegate<D> = throw AssertionError()

/**
 * Avoids overriding existing diffing machinery of [this] delegate.
 */
@Suppress("DeprecatedCallableAddReplaceWith", "UnusedReceiverParameter")
@Deprecated("Why you wanna do this?!", level = DeprecationLevel.ERROR)
fun <D> DiffDelegate<D>.diff(): DiffDelegate<D> = throw AssertionError()

/**
 * Avoids overriding existing diffing machinery of [this] delegate.
 */
@Suppress("DeprecatedCallableAddReplaceWith", "UNUSED_PARAMETER")
@Deprecated("Why you wanna do this?!", level = DeprecationLevel.ERROR)
operator fun <D> DiffDelegate<D>.plus(cb: DiffUtil.ItemCallback<in D>): DiffDelegate<D> =
    throw AssertionError()

/**
 * Creates an equating predicate.
 */
@Suppress("UNCHECKED_CAST", "USELESS_CAST")
fun <D : Any> equate(): (D, D) -> Boolean =
    { old: D, new: D -> (old == new) as Any } as (D, D) -> Boolean

/**
 * Creates an equating predicate using the [selector] function to extract a value for equating.
 */
@Suppress("UNCHECKED_CAST", "USELESS_CAST")
inline fun <D> equateBy(crossinline selector: (D) -> Any?): (D, D) -> Boolean =
    { old: D, new: D -> (selector(old) == selector(new)) as Any } as (D, D) -> Boolean


@PublishedApi internal abstract class DelegatedDiffDelegate<D>(
    private val d: (parent: ViewGroup) -> VH<*, *, D> // Delegate<D>
) : DiffDelegate<D>() {
    final override fun invoke(p1: ViewGroup): VH<*, *, D> = d.invoke(p1)
    final override fun hashCode(): Int = d.hashCode()
    final override fun equals(other: Any?): Boolean = other is DelegatedDiffDelegate<*> && d == other.d
    final override fun toString(): String = d.toString()
}
