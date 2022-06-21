package net.aquadc.delegapter.decor

import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.RecyclerView

/**
 * Enumerates different (left, top, right, bottom) positions for any [android.view.View].
 * @author Mike Gorünóv
 */
enum class ViewBounds {

    /**
     * Bounds with margins and ItemDecoration offsets.
     */
    DecoratedWithMargins {
        override fun of(child: View, into: Rect, tmpRectF: RectF, axes: Int) {
            WithMargins.of(child, into, tmpRectF, axes)
            (child.parent as? RecyclerView)?.layoutManager?.let { manager ->
                into.offset(
                    -manager.getLeftDecorationWidth(child), -manager.getTopDecorationHeight(child),
                    manager.getRightDecorationWidth(child), manager.getBottomDecorationHeight(child),
                    axes,
                )
            }
        }
    },

    /**
     * Bounds with margins.
     */
    WithMargins {
        override fun of(child: View, into: Rect, tmpRectF: RectF, axes: Int) {
            Padded.of(child, into, tmpRectF, axes)
            (child.layoutParams as? MarginLayoutParams)?.let {
                into.offset(-it.leftMargin, -it.topMargin, it.rightMargin, it.bottomMargin, axes)
            }
        }
    },

    /**
     * “Normal” bounds with paddings.
     */
    Padded {
        override fun of(child: View, into: Rect, tmpRectF: RectF, axes: Int) {
            val l = into.left; val t = into.top; val r = into.right; val b = into.bottom
            child.getHitRect(into)
            if (!axes.has(RecyclerView.HORIZONTAL)) {
                into.left = l; into.right = r
            }
            if (!axes.has(RecyclerView.VERTICAL)) {
                into.top = t; into.bottom = b
            }
        }
    },

    /**
     * Bounds inset by paddings.
     */
    Unpadded {
        @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog") // no NaN check in round() please
        override fun of(child: View, into: Rect, tmpRectF: RectF, axes: Int) {
            tmpRectF.set(
                child.paddingLeft.toFloat(),
                child.paddingTop.toFloat(),
                (child.width - child.paddingRight).toFloat(),
                (child.height - child.paddingBottom).toFloat(),
            )
            child.matrix.mapRect(tmpRectF)
            val left = child.left
            val top = child.top
            if (axes.has(RecyclerView.HORIZONTAL)) {
                into.left = left + Math.round(tmpRectF.left)
                into.right = left + Math.round(tmpRectF.right)
            }
            if (axes.has(RecyclerView.VERTICAL)) {
                into.top = top + Math.round(tmpRectF.top)
                into.bottom = top + Math.round(tmpRectF.bottom)
            }
        }
    },

    ;

    /**
     * Gets bounds of [child] view into [the given Rect][into].
     */
    abstract fun of(child: View, into: Rect, tmpRectF: RectF, axes: Int)

    protected fun Rect.offset(leftBy: Int, topBy: Int, rightBy: Int, bottomBy: Int, orientations: Int) {
        if (orientations.has(RecyclerView.HORIZONTAL)) {
            left += leftBy
            right += rightBy
        }
        if (orientations.has(RecyclerView.VERTICAL)) {
            top += topBy
            bottom += bottomBy
        }
    }
    protected fun Int.has(at: Int) =
        ((1 shl at) and this) != 0

}
