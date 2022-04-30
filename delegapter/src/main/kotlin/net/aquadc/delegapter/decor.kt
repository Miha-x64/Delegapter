package net.aquadc.delegapter

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.DisplayMetrics
import android.util.SparseIntArray
import android.util.TypedValue
import android.util.TypedValue.complexToDimensionPixelOffset
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
    @JvmField protected val between = ArrayList<Triple<DelegatePredicate?, DelegatePredicate?, Int>>()

    fun around(
        delegate: DelegatePredicate,
        spaceSize: Float = Float.NaN, @ComplexDimension.Unit spaceUnit: Int = TypedValue.COMPLEX_UNIT_DIP,
    ) {
        addDecor(delegate, null, spaceSize, spaceUnit)
        addDecor(null, delegate, spaceSize, spaceUnit)
    }

    fun before(
        next: DelegatePredicate,
        spaceSize: Float = Float.NaN, @ComplexDimension.Unit spaceUnit: Int = TypedValue.COMPLEX_UNIT_DIP,
    ): Unit = addDecor(null, next, spaceSize, spaceUnit)

    fun between(
        both: DelegatePredicate,
        spaceSize: Float = Float.NaN, @ComplexDimension.Unit spaceUnit: Int = TypedValue.COMPLEX_UNIT_DIP,
    ): Unit = addDecor(both, both, spaceSize, spaceUnit)

    fun after(
        prev: DelegatePredicate,
        spaceSize: Float = Float.NaN, @ComplexDimension.Unit spaceUnit: Int = TypedValue.COMPLEX_UNIT_DIP,
    ): Unit = addDecor(prev, null, spaceSize, spaceUnit)

    fun between(
        prev: DelegatePredicate, next: DelegatePredicate,
        spaceSize: Float = Float.NaN, @ComplexDimension.Unit spaceUnit: Int = TypedValue.COMPLEX_UNIT_DIP,
    ): Unit = addDecor(prev, next, spaceSize, spaceUnit)

    private fun addDecor(prev: DelegatePredicate?, next: DelegatePredicate?, spaceSize: Float, spaceUnit: Int) {
        if (spaceSize.isFinite()) {
            between.add(Triple(prev, next, ComplexDimension.createComplexDimension(spaceSize, spaceUnit)))
        }
    }

    private val before = SparseIntArray()
    private val after = SparseIntArray()
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val myHolder = parent.getChildViewHolder(view) ?: return outRect.setEmpty()
        val pos = myHolder.bindingAdapterPosition
        if (pos < 0) {
            val lp = myHolder.layoutPosition
            return outRect.setBeforeAfter(before.get(lp), after.get(lp))
        }

        val myDelegate = delegapter.delegateAt(pos)
        val nextDelegate = delegateAtOrNull(pos + 1)

        val dm = parent.resources.displayMetrics
        var before = 0
        var after = 0
        val between = between
        for (i in between.indices) {
            val (pp, np, dimension) = between[i]
            if (pp == null) {
                if (np!!(myDelegate)) before += complexToDimensionPixelOffset(dimension, dm)
            } else if (pp(myDelegate) && (np == null || nextDelegate != null && np(nextDelegate))) {
                after += complexToDimensionPixelOffset(dimension, dm)
            }
        }

        if (before == 0) this.before.delete(pos) else this.before.put(pos, before)
        if (after == 0) this.after.delete(pos) else this.after.put(pos, after)
        outRect.setBeforeAfter(before, after)
    }
    protected fun delegateAtOrNull(position: Int): Delegate<*>? =
        delegapter.takeIf { it.size > position }?.delegateAt(position)

    private fun Rect.setBeforeAfter(before: Int, after: Int) {
        val h = orientation == RecyclerView.HORIZONTAL
        set(if (h) before else 0, if (!h) before else 0, if (h) after else 0, if (!h) after else 0)
    }
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
            val nextDelegate = delegateAtOrNull(pos + 1)
            var before = 0
            var after = 0
            for (i in between.indices) {
                val (pp, np, dimension) = between[i]
                if (pp == null) {
                    if (np!!(myDelegate)) {
                        paint.color = FOREGROUND_BEFORE
                        before += c.drawSpace(parent, dimension, before, bnds, -1, dm)
                    }
                } else if (pp(myDelegate) && (np == null || nextDelegate != null && np(nextDelegate))) {
                    paint.color = if (np == null) FOREGROUND_AFTER else FOREGROUND_BETWEEN
                    after += c.drawSpace(parent, dimension, after, bnds, 1, dm)
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
        var padding = paint.textSize / 4f
        val toS = myDelegate.toString()
        paint.color = BACKGROUND
        drawRect(
            left.toFloat(), top.toFloat(),
            left + paint.measureFun(toS) + padding,
            top - fm.ascent + fm.descent + padding,
            paint
        )
        paint.color = FOREGROUND_BETWEEN
        padding /= 2f
        drawFun(
            toS,
            left + padding,
            top - fm.ascent + padding,
            paint.also { it.textAlign = Paint.Align.LEFT },
        )
    }
    private fun Canvas.drawSpace(parent: RecyclerView, dimension: Int, space: Int, bnds: Rect, direction: Int, dm: DisplayMetrics): Int {
        val dimensionPx = complexToDimensionPixelOffset(dimension, dm)
        dm.guessTextSize(dimensionPx, 10f, 20f)
        val sx: Int
        val sy: Int
        val ex: Int
        val ey: Int
        val tx: Int
        val ty: Int
        if (orientation == RecyclerView.HORIZONTAL) {
            sx = if (direction < 0) bnds.left - space else bnds.right + space
            sy = bnds.centerY()
            ex = sx + direction * dimensionPx
            ey = sy
            tx = (sx + ex) / 2
            ty = if (direction < 0) sy - fm.bottom else sy - fm.top
            paint.textAlign = Paint.Align.CENTER
        } else {
            sx = bnds.centerX()
            sy = if (direction < 0) bnds.top - space else bnds.bottom + space
            ex = sx
            ey = sy + direction * dimensionPx
            tx = sx + direction * MathUtils.clamp(parent.width / 100f, 2 * dm.density, 10 * dm.density).toInt()
            ty = (sy + ey) / 2 - (fm.ascent / 2f).toInt()
            paint.textAlign = if (direction < 0) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        drawLine(sx.toFloat(), sy.toFloat(), ex.toFloat(), ey.toFloat(), paint)
        drawText(ComplexDimension.toString(dimension), tx.toFloat(), ty.toFloat(), paint)
        return dimensionPx
    }
    private fun DisplayMetrics.guessTextSize(forValue: Int, min: Float, max: Float) {
        paint.textSize = MathUtils.clamp(forValue / 3f, min * scaledDensity, max * scaledDensity)
        paint.getFontMetricsInt(fm)
    }
    private companion object {
        // random bright colors visible both on black and white
        private const val FOREGROUND_BEFORE = 0xCC22EE66.toInt()
        private const val FOREGROUND_BETWEEN = 0xCC22AAAA.toInt()
        private const val FOREGROUND_AFTER = 0xCC2266EE.toInt()
        private const val BACKGROUND = 0x33000000
    }
}
