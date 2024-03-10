@file:JvmName("Placement")
package net.aquadc.delegapter1.decor

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder


typealias DecorationPlacement = (current: ViewHolder) -> Boolean

fun ItemDecorationBuilder.item(predicate: (ViewHolder) -> Boolean): DecorationPlacement =
    predicate

private fun DecorationPlacement.ifSibling(offset: Int, valid: (Int) -> Boolean, invalid: (ViewHolder?) -> Boolean): DecorationPlacement =
    {
        if (this(it)) {
            val pos = it.bindingAdapterPosition
            if (pos >= 0) valid(pos + offset) else {
                val parent = it.itemView.parent as RecyclerView
                val sibling = parent.getChildAt(parent.indexOfChild(it.itemView) + offset)
                invalid(sibling?.let(parent::getChildViewHolder))
            }
        } else false
    }

fun DecorationPlacement.ifPreceding(valid: (Int) -> Boolean, invalid: (ViewHolder?) -> Boolean): DecorationPlacement =
    ifSibling(-1, valid, invalid)
fun DecorationPlacement.ifFollowing(valid: (Int) -> Boolean, invalid: (ViewHolder?) -> Boolean): DecorationPlacement =
    ifSibling(+1, valid, invalid)

inline fun <reified T : ViewHolder> ItemDecorationBuilder.viewHolderIs(): DecorationPlacement =
    placement_viewHolderInstanceOf_4J(T::class.java)
@JvmName("viewHolderIs") fun placement_viewHolderInstanceOf_4J(klass: Class<out ViewHolder>): DecorationPlacement =
    klass::isInstance

@JvmField val anyViewHolder: DecorationPlacement = { _ -> true }
