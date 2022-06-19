@file:Suppress("FunctionName")

package net.aquadc.delegapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * Base ViewHolder with generified [view][V] and [binding/attachment][binding], and typed [bind] function.
 * @author Mike Gorünóv
 */
open class VH<out V : View, out B, in D>(view: V, val binding: B) : RecyclerView.ViewHolder(view) {
    @Suppress("UNCHECKED_CAST")
    inline val view: V get() = itemView as V

    open fun bind(data: D, position: Int, payloads: List<Any>) {}
    open fun recycle() {}
}
fun <V : View> VH(view: V) = VH<V, Nothing?, Unit>(view, null)

fun <B : ViewBinding> VH(binding: B) = VH<View, B, Unit>(binding.root, binding)

inline fun <B : ViewBinding, D> VH(
    binding: B, crossinline bind: VH<View, B, D>.(D, Int, List<Any>) -> Unit
): VH<View, B, D> =
    VH(binding.root, binding, bind)

inline fun <B : ViewBinding, D> VH(
    binding: B, crossinline bind: B.(D) -> Unit
): VH<View, B, D> =
    VH(binding.root, binding, bind)

inline fun <B : ViewBinding, D> inflateVH(
    parent: ViewGroup, inflate: (LayoutInflater, ViewGroup?, Boolean) -> B
): VH<View, B, D> {
    val binding = inflate(LayoutInflater.from(parent.context), parent, false)
    return VH(binding.root, binding)
}

inline fun <B : ViewBinding, D> inflateVH(
    parent: ViewGroup, inflate: (LayoutInflater, ViewGroup?, Boolean) -> B, crossinline bind: B.(D) -> Unit
): VH<View, B, D> {
    val binding = inflate(LayoutInflater.from(parent.context), parent, false)
    return VH(binding.root, binding, bind)
}

fun <D> inflateVH(
    parent: ViewGroup, layout: Int
): VH<View, Nothing?, D> {
    return VH(LayoutInflater.from(parent.context).inflate(layout, parent, false), null)
}

inline fun <D> inflateVH(
    parent: ViewGroup, layout: Int, crossinline bind: View.(D) -> Unit
): VH<View, Nothing?, D> {
    return VH(LayoutInflater.from(parent.context).inflate(layout, parent, false), null) { d, _, _ -> view.bind(d) }
}

inline fun <V : View, D> VH(view: V, crossinline bind: V.(D) -> Unit): VH<V, Nothing?, D> =
    VH(view) { d, _, _ ->
        view.bind(d)
    }

inline fun <V : View, D> VH(
    view: V, crossinline bind: VH<V, Nothing?, D>.(D, Int, List<Any>) -> Unit
): VH<V, Nothing?, D> = VH(view, null, bind)

inline fun <V : View, B, D> VH(view: V, binding: B, crossinline bind: B.(D) -> Unit): VH<V, B, D> =
    VH(view, binding) { d, _, _ ->
        binding.bind(d)
    }

inline fun <V : View, B, D> VH(
    view: V, binding: B, crossinline bind: VH<V, B, D>.(D, Int, List<Any>) -> Unit
): VH<V, B, D> = object : VH<V, B, D>(view, binding) {
    override fun bind(data: D, position: Int, payloads: List<Any>) =
        bind.invoke(this, data, position, payloads)
}
