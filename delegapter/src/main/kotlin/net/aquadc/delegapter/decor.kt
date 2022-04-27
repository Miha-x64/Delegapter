package net.aquadc.delegapter

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.DisplayMetrics
import android.util.SparseIntArray
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.core.math.MathUtils
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.min


inline fun MutableDelegapter.decor(
    @RecyclerView.Orientation orientation: Int,
    debugDelegates: Boolean = false,
    debugSpaces: Boolean = false,
    configure: Decor.() -> Unit = { }
): RecyclerView.ItemDecoration = (
    if (debugDelegates || debugSpaces) DebugDecor(this, orientation, debugDelegates, debugSpaces)
    else Decor(this, orientation)
    ).apply(configure)

typealias DelegatePredicate = (Delegate<*>) -> Boolean

open class Decor @PublishedApi internal constructor(
    @JvmField protected val delegapter: MutableDelegapter,
    @JvmField protected val orientation: Int,
) : RecyclerView.ItemDecoration() {
    @JvmField protected val between = ArrayList<Triple<DelegatePredicate, DelegatePredicate, Int>>()
    fun between(
        both: DelegatePredicate,
        spaceSize: Float = Float.NaN, @ComplexDimension.Unit spaceUnit: Int = TypedValue.COMPLEX_UNIT_DIP,
    ) =
        between(both, both, spaceSize, spaceUnit)
    fun between(
        prev: DelegatePredicate, next: DelegatePredicate,
        spaceSize: Float = Float.NaN, @ComplexDimension.Unit spaceUnit: Int = TypedValue.COMPLEX_UNIT_DIP,
    ) {
        if (spaceSize.isFinite()) {
            between.add(Triple(prev, next, ComplexDimension.createComplexDimension(spaceSize, spaceUnit)))
        }
    }

    private val cache = SparseIntArray()
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val myHolder = parent.getChildViewHolder(view) ?: return outRect.setEmpty()
        val pos = myHolder.bindingAdapterPosition
        if (pos < 0) return outRect.setAfter(cache.get(myHolder.layoutPosition))

        val nextDelegate = delegateAtOrNull(pos + 1) ?: return outRect.setEmpty()
        val myDelegate = delegapter.delegateAt(pos)

        val dm = parent.resources.displayMetrics
        var space = 0
        val between = between
        for (i in between.indices) {
            val (pp, np, iSpace) = between[i]
            if (pp(myDelegate) && np(nextDelegate))
                space += TypedValue.complexToDimensionPixelOffset(iSpace, dm)
        }

        if (space == 0) cache.delete(pos) else cache.put(pos, space)
        outRect.setAfter(space)
    }
    protected fun delegateAtOrNull(position: Int): Delegate<*>? =
        delegapter.takeIf { it.size > position }?.delegateAt(position)

    private fun Rect.setAfter(space: Int): Unit = set(
        0,
        0,
        if (orientation == RecyclerView.HORIZONTAL) space else 0,
        if (orientation == RecyclerView.VERTICAL) space else 0,
    )
}

@PublishedApi internal class DebugDecor(
    delegapter: MutableDelegapter,
    orientation: Int,
    private val delegates: Boolean,
    private val spaces: Boolean,
) : Decor(delegapter, orientation) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fm = Paint.FontMetricsInt()
    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val dm = parent.resources.displayMetrics
        paint.strokeWidth = dm.density // 1dp
        for (index in 0 until parent.childCount) {
            drawDebug(c, parent.getChildAt(index), parent, dm)
        }
    }
    private val bnds = Rect()
    private fun drawDebug(c: Canvas, view: View, parent: RecyclerView, dm: DisplayMetrics) {
        val myHolder = parent.getChildViewHolder(view) ?: return
        val pos = myHolder.bindingAdapterPosition
        if (pos < 0) return
        val myDelegate = delegapter.delegateAt(pos)

        view.copyBoundsWithMargins(bnds)

        if (delegates) {
            c.drawDelegate(view, myDelegate, bnds.left, bnds.top, dm)
        }

        if (spaces) {
            val nextDelegate = delegateAtOrNull(pos + 1) ?: return
            var space = 0
            val between = between
            paint.textAlign = if (orientation == RecyclerView.HORIZONTAL) Paint.Align.CENTER else Paint.Align.LEFT
            paint.color = FOREGROUND
            for (i in between.indices) {
                val (pp, np, iSpace) = between[i]
                if (pp(myDelegate) && np(nextDelegate)) {
                    space += c.drawSpace(parent, iSpace, space, bnds, dm)
                }
            }
        }
    }
    private fun View.copyBoundsWithMargins(dst: Rect) {
        getHitRect(dst)
        (layoutParams as? ViewGroup.MarginLayoutParams)?.let {
            dst.left -= it.leftMargin
            dst.top -= it.topMargin
            dst.right += it.rightMargin
            dst.bottom += it.bottomMargin
        }
    }
    private fun Canvas.drawDelegate(view: View, myDelegate: Delegate<*>, left: Int, top: Int, dm: DisplayMetrics) {
        dm.guessTextSize(min(view.width, view.height), 12f, 22f)
        val toS = myDelegate.toString()
        paint.color = BACKGROUND
        drawRect(left.toFloat(), top.toFloat(), left + paint.measureFun(toS), (top - fm.ascent + fm.descent).toFloat(), paint)
        paint.color = FOREGROUND
        drawFun(toS, left.toFloat(), (top - fm.ascent).toFloat(), paint.also { it.textAlign = Paint.Align.LEFT })
    }
    private fun Canvas.drawSpace(
        parent: RecyclerView,
        iSpace: Int,
        space: Int,
        bnds: Rect,
        dm: DisplayMetrics,
    ): Int {
        val iSpaceValue = TypedValue.complexToDimensionPixelOffset(iSpace, dm)
        dm.guessTextSize(iSpaceValue, 10f, 20f)
        val sx: Int
        val sy: Int
        val ex: Int
        val ey: Int
        val tx: Int
        val ty: Int
        if (orientation == RecyclerView.HORIZONTAL) {
            sx = bnds.right + space
            sy = bnds.centerY()
            ex = sx + iSpaceValue
            ey = sy
            tx = (sx + ex) / 2
            ty = sy - fm.bottom
        } else {
            sx = bnds.centerX()
            sy = bnds.bottom + space
            ex = sx
            ey = sy + iSpaceValue
            tx = sx + MathUtils.clamp(parent.width / 100f, 2 * dm.density, 10 * dm.density).toInt()
            ty = (sy + ey) / 2 - (fm.ascent / 2f).toInt()
        }
        drawLine(sx.toFloat(), sy.toFloat(), ex.toFloat(), ey.toFloat(), paint)
        drawText(ComplexDimension.toString(iSpace), tx.toFloat(), ty.toFloat(), paint)
        return iSpaceValue
    }
    private fun DisplayMetrics.guessTextSize(forValue: Int, min: Float, max: Float) {
        paint.textSize = MathUtils.clamp(forValue / 3f, min * scaledDensity, max * scaledDensity)
        paint.getFontMetricsInt(fm)
    }
    private companion object {
        private const val FOREGROUND = 0xFF22CC77.toInt() // random bright color visible both on black and white
        private const val BACKGROUND = 0x7F000000
    }
}
