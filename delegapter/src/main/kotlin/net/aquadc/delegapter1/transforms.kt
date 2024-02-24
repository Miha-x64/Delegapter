@file:JvmName("AdapterDelegateTransforms")
@file:Suppress("NOTHING_TO_INLINE")

package net.aquadc.delegapter1

import android.annotation.SuppressLint
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import net.aquadc.delegapter.EqualityDiff
import net.aquadc.delegapter.appendFun
import net.aquadc.delegapter.internal.WeakIdentityHashMap

/**
 * Delegated [AdapterDelegate].
 * @author Mike Gorünóv
 */
abstract class AdapterDelegateTransform<T, U, D : Diff<in T>?>(
    create: ViewHolderFactory,
    diff: D,
    /**
     * The other [AdapterDelegate] which does the job.
     */
    @JvmField protected val delegate: AdapterDelegate<U, *>,
) : AdapterDelegate<T, D>(create, diff) {
    @CallSuper override fun recycled(viewHolder: RecyclerView.ViewHolder): Unit =
        delegate.recycled(viewHolder)
    override fun toString(create: ViewHolderFactory, diff: Diff<*>?): String =
        delegate.toString(create, diff)
}

/**
 * Delegated [AdapterDelegate] binding the same type as the original [delegate].
 * @author Mike Gorünóv
 */
open class AdapterDelegateDecorator<T, D : Diff<in T>?>(
    create: ViewHolderFactory,
    diff: D,
    delegate: AdapterDelegate<T, *>,
) : AdapterDelegateTransform<T, T, D>(create, diff, delegate) {
    override fun bind(viewHolder: RecyclerView.ViewHolder, item: T, payloads: List<Any>): Unit =
        delegate.bind(viewHolder, item, payloads)
}

/**
 * Replace [ViewHolder factory][AdapterDelegate.create].
 * It's your responsibility to yield [RecyclerView.ViewHolder]s compatible with the original [AdapterDelegate.bind].
 * @see [then]
 */
inline fun <T, D : Diff<in T>?> AdapterDelegate<T, D>.copy(
    noinline create: ViewHolderFactory = this.create,
): AdapterDelegate<T, D> =
    if (this.create === create) this
    else AdapterDelegateDecorator(create, diff, this)

private fun <R, T> Diff<R>.unmap(transform: (T) -> R): Diff<T> = object : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T & Any, newItem: T & Any): Boolean {
        val old = transform(oldItem); val new = transform(newItem)
        return if (old === null || new === null) old === new else this@unmap.areItemsTheSame(old, new)
    }
    @SuppressLint("DiffUtilEquals") override fun areContentsTheSame(oldItem: T & Any, newItem: T & Any): Boolean {
        val old = transform(oldItem); val new = transform(newItem)
        return if (old === null || new === null) old === new else this@unmap.areContentsTheSame(old, new)
    }
    override fun getChangePayload(oldItem: T & Any, newItem: T & Any): Any? {
        val old = transform(oldItem); val new = transform(newItem)
        return if (old === null || new === null) null else this@unmap.getChangePayload(old, new)
    }
    override fun toString(): String =
        buildString { appendFun(transform).append(" | ").append(this@unmap) }
}

/**
 * Transform incoming [T] before it is passed into this [AdapterDelegate]<[R]>.
 */
inline fun <T, R> AdapterDelegate<R, Nothing?>.unmap(noinline transform: (T) -> R): AdapterDelegate<T, Nothing?> =
    unmap(null, transform)

/**
 * Transform incoming [T] before it is passed into this [AdapterDelegate]<[R]>.
 */
@JvmName("diffUnmap")
fun <T, R, D : Diff<in R>> AdapterDelegate<R, D>.unmap(transform: (T) -> R): AdapterDelegate<T, Diff<in T>> =
    unmap(diff.unmap(transform), transform)

@PublishedApi internal fun <T, R, D : Diff<in T>?> AdapterDelegate<R, *>.unmap(
    diff: D, transform: (T) -> R,
): AdapterDelegate<T, D> =
    object : AdapterDelegateTransform<T, R, D>(create, diff, this) {
        override fun bind(viewHolder: RecyclerView.ViewHolder, item: T, payloads: List<Any>): Unit =
            delegate.bind(viewHolder, transform(item), payloads)
        override fun toString(create: ViewHolderFactory, diff: Diff<*>?): String =
            buildString { appendFun(transform).append(" | ").append(super.toString(create, diff)) }
        //   https://linuxhint.com/bash_pipe_tutorial/ -^
    }

/**
 * Pre-bind this [AdapterDelegate] with the given [value] yielding an [AdapterDelegate]<[Unit]>.
 */
fun <T> AdapterDelegate<T, *>.bound(value: T): AdapterDelegate<Unit, Diff<in Unit>> =
    BoundAdapterDelegate(this, value)

private class BoundAdapterDelegate<T>(
    delegate: AdapterDelegate<T, *>,
    @JvmField val value: T,
) : AdapterDelegateTransform<Unit, T, Diff<in Unit>>(delegate.create, EqualityDiff, delegate) {
    override fun bind(viewHolder: RecyclerView.ViewHolder, item: Unit, payloads: List<Any>): Unit =
        delegate.bind(viewHolder, value, payloads)
    override fun toString(create: ViewHolderFactory, diff: Diff<*>?): String =
        "echo $value | ${super.toString(create, diff)}"
    override fun equals(other: Any?): Boolean =
        other is BoundAdapterDelegate<*> &&
            other.create == create &&
            other.delegate == delegate &&
            other.delegate.diff == delegate.diff &&
            (delegate.diff?.let {
                @Suppress("UNCHECKED_CAST") // it's Diff<T> is 'cuz != null; o.value is T 'cuz o.delegate == delegate
                (it as Diff<T>).areItemsTheSame(other.value as T, value) && it.areContentsTheSame(other.value, value)
            } ?: (other.value == value))
}


/**
 * Delegated [AdapterDelegate] having additional data per [RecyclerView.ViewHolder].
 * @author Mike Gorünóv
 */
abstract class AdapterDelegateTransformWSideTable<T, U>(
    /**
     * Wrapped [AdapterDelegate] to delegate [binding][AdapterDelegate.bind] to.
     */
    @JvmField protected val delegate: AdapterDelegate<U, *>,
): AdapterDelegate<T, Nothing?>(delegate.create, null) {
    private var sideTables: Any? = null
    /**
     * Create and register new side table.
     * By default, [T] instances are recycled along with [RecyclerView.ViewHolder]s.
     * This can be avoided by setting [scrap] to `null`.
     */
    protected inline fun <T : Any> perViewHolder(
        scrap: MutableList<T>? = ArrayList(),
        crossinline create: (RecyclerView.ViewHolder) -> T,
    ): (RecyclerView.ViewHolder) -> T = object : VHSideTableRecycler<T>(scrap) {
        override fun get(key: RecyclerView.ViewHolder?): T =
            super.get(key) ?: create(key!!).also { put(key, it) }
    }.register()
    @PublishedApi internal fun <T : Any> VHSideTableRecycler<T>.register(): VHSideTableRecycler<T> = apply {
        when (val st = sideTables) {
            null ->
                sideTables = this
            is VHSideTableRecycler<*> ->
                sideTables = ArrayList<VHSideTableRecycler<*>>(2).also { it.add(st); it.add(this) }
            else ->
                @Suppress("UNCHECKED_CAST") (st as ArrayList<VHSideTableRecycler<*>>).add(this)
        }
    }
    @PublishedApi internal abstract class VHSideTableRecycler<T : Any>(
        private val scrap: MutableList<T>?,
    ) : WeakIdentityHashMap<RecyclerView.ViewHolder, T>(), (RecyclerView.ViewHolder) -> T {
        final override fun invoke(viewHolder: RecyclerView.ViewHolder): T =
            get(viewHolder)!!
        override fun get(key: RecyclerView.ViewHolder?): T? =
            super.get(key) ?: scrap?.poll()?.also { put(key, it) }
        private fun <T> MutableList<T>.poll(): T? =
            if (isEmpty()) null else removeAt(lastIndex)

        override fun remove(key: RecyclerView.ViewHolder?): T? =
            super.remove(key)?.also(::staleEntryExpunged)
        override fun staleEntryExpunged(value: T) {
            scrap?.add(value)
        }
    }
    @CallSuper override fun recycled(viewHolder: RecyclerView.ViewHolder) {
        delegate.recycled(viewHolder)
        when (val st = sideTables) {
            null -> {}
            is VHSideTableRecycler<*> -> st.remove(viewHolder)
            else -> {
                @Suppress("UNCHECKED_CAST")
                st as ArrayList<VHSideTableRecycler<*>>
                for (i in 0 until st.size) {
                    st[i].remove(viewHolder)
                }
            }
        }
    }
    override fun toString(create: ViewHolderFactory, diff: Diff<*>?): String =
        "${javaClass.name}(${delegate.toString(create, diff)})"
}
