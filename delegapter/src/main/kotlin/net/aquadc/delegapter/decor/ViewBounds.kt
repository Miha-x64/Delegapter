package net.aquadc.delegapter.decor

import android.graphics.Rect
import android.graphics.RectF
import android.os.Looper
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.RecyclerView
import net.aquadc.delegapter1.decor.ViewBounds

/**
 * Enumerates different (left, top, right, bottom) positions for any [android.view.View].
 * @author Mike Gorünóv
 */
@Deprecated("moved", ReplaceWith("BoundsInParent", "net.aquadc.delegapter1.decor.BoundsInParent"))
enum class ViewBounds : ViewBounds {

    /**
     * Bounds with margins and ItemDecoration offsets.
     */
    DecoratedWithMargins {
        override fun ofView(child: View, axes: Int, into: Rect, inParent: Boolean) {
            WithMargins.ofView(child, axes, into, inParent)
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
        override fun ofView(child: View, axes: Int, into: Rect, inParent: Boolean) {
            Padded.ofView(child, axes, into, inParent)
            (child.layoutParams as? MarginLayoutParams)?.let {
                into.offset(-it.leftMargin, -it.topMargin, it.rightMargin, it.bottomMargin, axes)
            }
        }
    },

    /**
     * “Normal” bounds with paddings.
     */
    Padded {
        override fun ofView(child: View, axes: Int, into: Rect, inParent: Boolean) {
            if (inParent) {
                val l = into.left; val t = into.top; val r = into.right; val b = into.bottom
                child.getHitRect(into)
                if (!axes.has(RecyclerView.HORIZONTAL)) { into.left = l; into.right = r }
                if (!axes.has(RecyclerView.VERTICAL)) { into.top = t; into.bottom = b }
            } else {
                if (axes.has(RecyclerView.HORIZONTAL)) { into.left = 0; into.right = child.width }
                if (axes.has(RecyclerView.VERTICAL)) { into.top = 0; into.bottom = child.height }
            }
        }
    },

    /**
     * Bounds inset by paddings.
     */
    Unpadded {
        private val tmpRectF = RectF()
        @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog") // no NaN check in round() please
        override fun ofView(child: View, axes: Int, into: Rect, inParent: Boolean) {
            if (inParent) {
                val tmpRectF = if (Looper.myLooper() === Looper.getMainLooper()) tmpRectF else RectF()
                tmpRectF.set(
                    child.paddingLeft.toFloat(),
                    child.paddingTop.toFloat(),
                    (child.width - child.paddingRight).toFloat(),
                    (child.height - child.paddingBottom).toFloat(),
                )
                child.matrix.mapRect(tmpRectF)
                if (axes.has(RecyclerView.HORIZONTAL)) {
                    val left = child.left
                    into.left = left + Math.round(tmpRectF.left)
                    into.right = left + Math.round(tmpRectF.right)
                }
                if (axes.has(RecyclerView.VERTICAL)) {
                    val top = child.top
                    into.top = top + Math.round(tmpRectF.top)
                    into.bottom = top + Math.round(tmpRectF.bottom)
                }
            } else {
                if (axes.has(RecyclerView.HORIZONTAL)) {
                    into.left = child.paddingLeft
                    into.right = child.width - child.paddingRight
                }
                if (axes.has(RecyclerView.VERTICAL)) {
                    into.top = child.paddingTop
                    into.bottom = child.height - child.paddingBottom
                }
            }
        }
    },

    ;

    /**
     * Gets bounds of [child] view into [the given Rect][into].
     */
    fun of(child: View, into: Rect, tmpRectF: RectF, axes: Int): Unit =
        ofView(child, axes, into, inParent = true)

    protected fun Rect.offset(leftBy: Int, topBy: Int, rightBy: Int, bottomBy: Int, axes: Int) {
        if (axes.has(RecyclerView.HORIZONTAL)) {
            left += leftBy
            right += rightBy
        }
        if (axes.has(RecyclerView.VERTICAL)) {
            top += topBy
            bottom += bottomBy
        }
    }
    protected fun Int.has(at: Int) =
        ((1 shl at) and this) != 0

}
