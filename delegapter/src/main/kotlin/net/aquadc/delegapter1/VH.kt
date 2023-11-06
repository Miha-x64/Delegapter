@file:Suppress("FunctionName", "NOTHING_TO_INLINE")
@file:JvmName("ViewHolders")
package net.aquadc.delegapter1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * [RecyclerView.Adapter.onViewRecycled] hook.
 */
interface Recyclable {
    /**
     * @see [RecyclerView.Adapter.onViewRecycled]
     */
    fun recycle()
}

/**
 * Base ViewHolder with generified [view][V] and [binding/attachment][binding].
 * @author Mike Gorünóv
 */
open class VH<out V : View, out B>(
    view: V,
    /**
     * Arbitrary attachment, typically, [ViewBinding].
     */
    @JvmField val binding: B,
) : RecyclerView.ViewHolder(view), Recyclable {
    /**
     * Typed overload of [itemView].
     */
    @Suppress("UNCHECKED_CAST")
    inline val view: V get() = itemView as V
    override fun recycle() {}
}

/**
 * Wrap [view] in a [VH].
 */
@JvmName("of")
inline fun <V : View> VH(view: V): VH<V, Nothing?> =
    VH<V, Nothing?>(view, null)

/**
 * Wrap [view] with the specified [layoutParams] in a [VH].
 * @see android.view.ViewGroup.addView
 */
@JvmName("of")
fun <V : View> VH(view: V, layoutParams: RecyclerView.LayoutParams?): VH<V, Nothing?> =
    VH<V, Nothing?>(view.also { it.layoutParams = layoutParams }, null)

/**
 * Wrap [binding]'s [ViewBinding.getRoot] view in a [VH].
 */
@JvmName("of")
fun <B : ViewBinding> VH(binding: B): VH<View, B> =
    VH(binding.root, binding)

/**
 * Inflate [ViewBinding] in [parent] and wrap in [VH].
 * @param inflate a function reference to `SomeViewBinding::inflate
 */
@JvmName("inflate")
inline fun <B : ViewBinding> inflateVH(
    parent: ViewGroup, inflate: (LayoutInflater, ViewGroup?, Boolean) -> B
): VH<View, B> {
    val binding = inflate(LayoutInflater.from(parent.context), parent, false)
    return VH(binding.root, binding)
}

/**
 * Inflate the given [layout] in [parent] and wrap in a [VH].
 */
@JvmName("inflate")
fun inflateVH(
    parent: ViewGroup, @LayoutRes layout: Int
): VH<View, Nothing?> =
    VH<View, Nothing?>(LayoutInflater.from(parent.context).inflate(layout, parent, false), null)
