package net.aquadc.delegapter.decor

import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.RecyclerView

/**
 * Enumerates different (left, top, right, bottom) positions for any [android.view.View].
 */
enum class ViewBounds {

    /**
     * Bounds with margins and ItemDecoration offsets.
     */
    DecoratedWithMargins {
        override fun of(child: View, into: Rect, tmpRectF: RectF) {
            WithMargins.of(child, into, tmpRectF)
            (child.parent as? RecyclerView)?.layoutManager?.let { manager ->
                into.offset(
                    -manager.getLeftDecorationWidth(child), -manager.getTopDecorationHeight(child),
                    manager.getRightDecorationWidth(child), manager.getBottomDecorationHeight(child),
                )
            }
        }
    },

    /**
     * Bounds with margins.
     */
    WithMargins {
        override fun of(child: View, into: Rect, tmpRectF: RectF) {
            child.getHitRect(into)
            (child.layoutParams as? MarginLayoutParams)?.let {
                into.offset(-it.leftMargin, -it.topMargin, it.rightMargin, it.bottomMargin)
            }
        }
    },

    /**
     * “Normal” bounds with paddings.
     */
    Padded {
        override fun of(child: View, into: Rect, tmpRectF: RectF) {
            child.getHitRect(into)
        }
    },

    /**
     * Bounds inset by paddings.
     */
    Unpadded {
        override fun of(child: View, into: Rect, tmpRectF: RectF) {
            tmpRectF.set(
                child.paddingLeft.toFloat(),
                child.paddingTop.toFloat(),
                (child.width - child.paddingRight).toFloat(),
                (child.height - child.paddingBottom).toFloat(),
            )
            child.matrix.mapRect(tmpRectF)
            val left = child.left
            val top = child.top
            @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog") // no NaN check please
            into.set(
                left + Math.round(tmpRectF.left), top + Math.round(tmpRectF.top),
                left + Math.round(tmpRectF.right), top + Math.round(tmpRectF.bottom),
            )
        }
    },

    ;

    /**
     * Gets bounds of [child] view into [the given Rect][into].
     */
    abstract fun of(child: View, into: Rect, tmpRectF: RectF)

    protected fun Rect.offset(leftBy: Int, topBy: Int, rightBy: Int, bottomBy: Int) {
        left += leftBy
        top += topBy
        right += rightBy
        bottom += bottomBy
    }

    companion object {
        private val VALS = values()
        /** Returns value at [index]. */
        operator fun get(index: Int) = VALS[index]
    }
}
