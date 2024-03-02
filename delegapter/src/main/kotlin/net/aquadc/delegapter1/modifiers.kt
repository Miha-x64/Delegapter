@file:JvmName("AdapterDelegateModifiers")
package net.aquadc.delegapter1

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView


@RequiresApi(21) private class ViewHolderBackground(
    private var drawable: Drawable?,
    private val factory: Drawable.ConstantState,
) : (RecyclerView.ViewHolder) -> Unit {
    override fun invoke(viewHolder: RecyclerView.ViewHolder) {
        val itemView = viewHolder.itemView
        val original = itemView.background
        val bg = drawable?.also { drawable = null } ?: factory.newDrawableCompat(itemView.context)
        itemView.background = if (original == null) bg else StackedLayerDrawableCompat(bg, original)
    }
    private fun Drawable.ConstantState.newDrawableCompat(context: Context) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) newDrawable(context.resources, context.theme)
        else factory.newDrawable(context.resources)
    @Suppress("FunctionName") private fun StackedLayerDrawableCompat(bg: Drawable, original: Drawable) =
        LayerDrawable(arrayOf(bg, original)).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                paddingMode = LayerDrawable.PADDING_MODE_STACK
        }
    override fun toString(): String =
        "background($factory)"
}

/**
 * Create new [ViewType] by adding background from [factory].
 * If [RecyclerView.ViewHolder.itemView] already has a background,
 * it will be preserved, and new background will be added behind.
 * On Android versions <21 composite background [may be screwed][LayerDrawable.PADDING_MODE_NEST].
 */
@RequiresApi(21)
fun ViewType.background(factory: Drawable.ConstantState): ViewType =
    then(ViewHolderBackground(null, factory))

/**
 * Create new [AdapterDelegate] by adding background from [factory].
 * If [RecyclerView.ViewHolder.itemView] already has a background,
 * it will be preserved, and new background will be added behind.
 * On Android versions <21 composite background [may be screwed][LayerDrawable.PADDING_MODE_NEST].
 */
@RequiresApi(21)
fun <T, D : Diff<T>?> AdapterDelegate<T, D>.background(factory: Drawable.ConstantState): AdapterDelegate<T, D> =
    copy(create = create.background(factory))

/**
 * Create new [ViewType] by adding background [drawable]. It must implement [Drawable.getConstantState].
 * If [RecyclerView.ViewHolder.itemView] already has a background,
 * it will be preserved, and new background will be added behind.
 * On Android versions <21 composite background [may be screwed][LayerDrawable.PADDING_MODE_NEST].
 */
@RequiresApi(21)
fun ViewType.background(drawable: Drawable): ViewType =
    then(ViewHolderBackground(drawable, drawable.constantState!!))

/**
 * Create new [AdapterDelegate] by adding background [drawable]. It must implement [Drawable.getConstantState].
 * If [RecyclerView.ViewHolder.itemView] already has a background,
 * it will be preserved, and new background will be added behind.
 * On Android versions <21 composite background [may be screwed][LayerDrawable.PADDING_MODE_NEST].
 */
@RequiresApi(21)
fun <T, D : Diff<T>?> AdapterDelegate<T, D>.background(drawable: Drawable): AdapterDelegate<T, D> =
    copy(create = create.background(drawable))

/**
 * Create new [ViewType] by adding margin to [RecyclerView.ViewHolder.itemView].
 */
fun ViewType.plusMargins(
    @Px left: Int = 0, @Px top: Int = 0, @Px right: Int = 0, @Px bottom: Int = 0,
): ViewType =
    if (left or top or right or bottom == 0) this
    else then(object : (RecyclerView.ViewHolder) -> Unit {
        override fun invoke(vh: RecyclerView.ViewHolder) {
            val lp = vh.itemView.layoutParams as RecyclerView.LayoutParams
            lp.leftMargin += left
            lp.topMargin += top
            lp.rightMargin += right
            lp.bottomMargin += bottom
            vh.itemView.layoutParams = lp
        }
        override fun toString(): String = buildString {
            append("margins").append('(')
            if (left > 0) append('+')
            append(left).append(", ")
            if (top > 0) append('+')
            append(top).append(" - ") // like Rect but with explicit +
            if (right > 0) append('+')
            append(right).append(", ")
            if (bottom > 0) append('+')
            append(bottom).append(')')
        }
    })

/**
 * Create new [AdapterDelegate] by adding margin to [RecyclerView.ViewHolder.itemView].
 */
fun <T, D : Diff<T>?> AdapterDelegate<T, D>.plusMargins(
    @Px left: Int = 0, @Px top: Int = 0, @Px right: Int = 0, @Px bottom: Int = 0,
): AdapterDelegate<T, D> =
    copy(create = create.plusMargins(left, top, right, bottom) )
