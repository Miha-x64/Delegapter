package net.aquadc.delegapter.decor

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.util.SparseLongArray
import android.util.TypedValue
import android.util.TypedValue.complexToDimensionPixelOffset
import android.view.Gravity
import android.view.Gravity.FILL_HORIZONTAL
import android.view.Gravity.FILL_VERTICAL
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.annotation.RequiresApi
import androidx.core.math.MathUtils
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView.LAYOUT_DIRECTION_RTL
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import net.aquadc.delegapter.Delegate
import net.aquadc.delegapter.MutableDelegapter
import net.aquadc.delegapter.adapter.DelegatedAdapter
import net.aquadc.delegapter.decor.ComplexDimension.ComplexDimensionUnit
import net.aquadc.delegapter.drawFun
import net.aquadc.delegapter.measureFun
import kotlin.math.min

/**
 * Build an [RecyclerView.ItemDecoration].
 * @param forAdapter if specified, consider only items from this adapter
 * @param debugDelegates draw delegate names (debug feature)
 * @param debugSpaces draw space sizes (debug feature)
 */
@RequiresApi(18) inline fun MutableDelegapter.decor(
    @RecyclerView.Orientation orientation: Int,
    forAdapter: RecyclerView.Adapter<*>? = null,
    debugDelegates: Boolean = false,
    debugSpaces: Boolean = false,
    configure: Decor.() -> Unit
): RecyclerView.ItemDecoration = (
    if (debugDelegates || debugSpaces) DebugDecor(this, orientation, forAdapter, debugDelegates, debugSpaces)
    else Decor(this, orientation, forAdapter)
    ).apply(configure)

/**
 * Build an [RecyclerView.ItemDecoration] for [this] adapter.
 * @param debugDelegates draw delegate names (debug feature)
 * @param debugSpaces draw space sizes (debug feature)
 */
@RequiresApi(18) inline fun DelegatedAdapter.decor(
    @RecyclerView.Orientation orientation: Int,
    debugDelegates: Boolean = false,
    debugSpaces: Boolean = false,
    configure: Decor.() -> Unit
): RecyclerView.ItemDecoration =
    data.decor(orientation, this, debugDelegates, debugSpaces, configure)

typealias DelegatePredicate = (Delegate<*>) -> Boolean

@RequiresApi(18) // 18+ SparseLongArray and 17+ Gravity.apply(..., direction)
open class Decor @PublishedApi internal constructor(
    @JvmField protected val delegapter: MutableDelegapter,
    @JvmField protected val orientation: Int,
    @JvmField protected val forAdapter: RecyclerView.Adapter<*>?,
) : RecyclerView.ItemDecoration() {
    init {
        require(orientation == HORIZONTAL || orientation == VERTICAL) { "Invalid orientation: $orientation" }
    }
    @JvmField protected var ints = IntArray(6) // size, bounds, drawableGravity: 3 ints per decoration
    @JvmField protected val objs = ArrayList<Any?>() // prev, next, drawable: 3 objs per decoration

    /**
     * Add decoration before matching [delegate][next]s.
     * Either [size] or [drawable] must be specified.
     * If the [drawable] has no intrinsic bounds, [size] is still required,
     * and [drawableGravity] for [orientation] must be [Gravity.FILL].
     * The [level][Drawable.setLevel] of [drawable]
     * will be set to [bindingAdapterPosition][RecyclerView.ViewHolder.getBindingAdapterPosition],
     * or to `-1 - [RecyclerView.ViewHolder.getLayoutPosition]`, if the former is invalid.
     * The [state][Drawable.setState] of [drawable] will be set to view's [drawableState][View.getDrawableState].
     * [Alpha][Drawable.setAlpha] will match [View's one][View.getAlpha].
     */
    fun before(
        next: DelegatePredicate,
        size: Int = 0, @ComplexDimensionUnit unit: Int = TypedValue.COMPLEX_UNIT_DIP,
        drawable: Drawable? = null, drawableBounds: ViewBounds = ViewBounds.Padded, drawableGravity: Int = Gravity.FILL,
    ): Unit = addDecor(null, next, size, unit, drawable, drawableBounds, BoundsNegotiation.Average, drawableGravity)

    /**
     * Add decoration before matching [delegate][prev]s.
     * Either [size] or [drawable] must be specified.
     * If the [drawable] has no intrinsic bounds, [size] is still required,
     * and [drawableGravity] for [orientation] must be [Gravity.FILL].
     * The [level][Drawable.setLevel] of [drawable]
     * will be set to [bindingAdapterPosition][RecyclerView.ViewHolder.getBindingAdapterPosition],
     * or to `-1 - [RecyclerView.ViewHolder.getLayoutPosition]`, if the former is invalid.
     * The [state][Drawable.setState] of [drawable] will be set to view's [drawableState][View.getDrawableState].
     * [Alpha][Drawable.setAlpha] will match [View's one][View.getAlpha].
     */
    fun after(
        prev: DelegatePredicate,
        size: Int = 0, @ComplexDimensionUnit unit: Int = TypedValue.COMPLEX_UNIT_DIP,
        drawable: Drawable? = null, drawableBounds: ViewBounds = ViewBounds.Padded, drawableGravity: Int = Gravity.FILL,
    ): Unit = addDecor(prev, null, size, unit, drawable, drawableBounds, BoundsNegotiation.Average, drawableGravity)

    /**
     * Add decoration before and after matching [delegate]s.
     * Either [size] or [drawable] must be specified.
     * If the [drawable] has no intrinsic bounds, [size] is still required,
     * and [drawableGravity] for [orientation] must be [Gravity.FILL].
     * The [level][Drawable.setLevel] of [drawable]
     * will be set to [bindingAdapterPosition][RecyclerView.ViewHolder.getBindingAdapterPosition],
     * or to `-1 - [RecyclerView.ViewHolder.getLayoutPosition]`, if the former is invalid.
     * The [state][Drawable.setState] of [drawable] will be set to view's [drawableState][View.getDrawableState].
     * [Alpha][Drawable.setAlpha] will match [View's one][View.getAlpha].
     */
    fun around(
        delegate: DelegatePredicate,
        size: Int = 0, @ComplexDimensionUnit unit: Int = TypedValue.COMPLEX_UNIT_DIP,
        drawable: Drawable? = null, drawableBounds: ViewBounds = ViewBounds.Padded, drawableGravity: Int = Gravity.FILL,
    ) {
        checkDimensions(size, drawable, drawableGravity)
        ensureSize(2)
        insert(delegate, null, size, unit, drawable, drawableBounds, BoundsNegotiation.Average, drawableGravity)
        insert(null, delegate, size, unit, drawable, drawableBounds, BoundsNegotiation.Average, drawableGravity)
    }

    /**
     * Add decoration between [delegate][both]s.
     * Either [size] or [drawable] must be specified.
     * If the [drawable] has no intrinsic bounds, [size] is still required,
     * and [drawableGravity] for [orientation] must be [Gravity.FILL].
     * The [level][Drawable.setLevel] of [drawable]
     * will be set to [bindingAdapterPosition][RecyclerView.ViewHolder.getBindingAdapterPosition]
     * of the previous item,
     * or to `-1 - [RecyclerView.ViewHolder.getLayoutPosition]`, if the former is invalid.
     * The [state][Drawable.setState] of [drawable] will be set to view's [drawableState][View.getDrawableState].
     * [Alpha][Drawable.setAlpha] will match [View's one][View.getAlpha].
     */
    fun between(
        both: DelegatePredicate,
        size: Int = 0, @ComplexDimensionUnit unit: Int = TypedValue.COMPLEX_UNIT_DIP,
        drawable: Drawable? = null, drawableBounds: ViewBounds = ViewBounds.Padded,
        viewBoundsNegotiation: BoundsNegotiation = BoundsNegotiation.Average, drawableGravity: Int = Gravity.FILL,
    ): Unit = addDecor(both, both, size, unit, drawable, drawableBounds, viewBoundsNegotiation, drawableGravity)

    /**
     * Add decoration between [prev] and [next] delegates.
     * Either [size] or [drawable] must be specified.
     * If the [drawable] has no intrinsic bounds, [size] is still required,
     * and [drawableGravity] for [orientation] must be [Gravity.FILL].
     * The [level][Drawable.setLevel] of [drawable]
     * will be set to [bindingAdapterPosition][RecyclerView.ViewHolder.getBindingAdapterPosition]
     * of the [previous][prev] item,
     * or to `-1 - [RecyclerView.ViewHolder.getLayoutPosition]`, if the former is invalid.
     * The [state][Drawable.setState] of [drawable] will be set to view's [drawableState][View.getDrawableState].
     * [Alpha][Drawable.setAlpha] will match [View's one][View.getAlpha].
     */
    fun between(
        prev: DelegatePredicate, next: DelegatePredicate,
        size: Int = 0, @ComplexDimensionUnit unit: Int = TypedValue.COMPLEX_UNIT_DIP,
        drawable: Drawable? = null, drawableBounds: ViewBounds = ViewBounds.Padded,
        viewBoundsNegotiation: BoundsNegotiation = BoundsNegotiation.Average, drawableGravity: Int = Gravity.FILL,
    ): Unit = addDecor(prev, next, size, unit, drawable, drawableBounds, viewBoundsNegotiation, drawableGravity)

    private val tmpInts1 = IntArray(2)
    private val tmpInts2 = IntArray(2)
    private fun addDecor(
        prev: DelegatePredicate?, next: DelegatePredicate?,
        size: Int, unit: Int,
        drawable: Drawable?, drawableBounds: ViewBounds, viewBoundsNegotiation: BoundsNegotiation, drawableGravity: Int,
    ) {
        checkDimensions(size, drawable, drawableGravity)
        ensureSize(1)
        insert(prev, next, size, unit, drawable, drawableBounds, viewBoundsNegotiation, drawableGravity)
    }

    private fun checkDimensions(size: Int, drawable: Drawable?, drawableGravity: Int) {
        if (drawable != null) {
            val intW = drawable.intrinsicWidth
            val intH = drawable.intrinsicHeight
            val noIntW = intW <= 0
            val noIntH = intH <= 0
            val badW = noIntW && (drawableGravity and FILL_HORIZONTAL) != FILL_HORIZONTAL
            val badH = noIntH && (drawableGravity and FILL_VERTICAL) != FILL_VERTICAL
            if (badW || badH) throw IllegalArgumentException(
                "$drawable's intrinsic dimensions are " +
                    "(${if (noIntW) "none" else intW}, ${if (noIntH) "none" else intH}), thus " +
                    (if (badW && badH) "" else if (badW) "horizontal component of " else "vertical component of ") +
                    "'drawableGravity' must be FILL, got ${GravityCompote.toString(drawableGravity)}"
            )
            tmpInts1[0] = intW
            tmpInts1[1] = intH
        } else {
            tmpInts1[0] = -1
            tmpInts1[1] = -1
        }

        if (size == 0)
            require(tmpInts1[orientation] > 0) {
                (if (drawable == null) "drawable" else "$drawable's 'intrinsic${if (orientation == HORIZONTAL) "Width" else "Height"}'") +
                    " is not specified, thus 'size' is required"
            }
    }

    private fun ensureSize(needed: Int) {
        val used = objs.size
        val newSize = used + 3 * needed
        if (newSize > 64 * 3) {
            throw UnsupportedOperationException("Can't handle >64 different decorations.")
        }
        if (ints.size < newSize) {
            val expanded = IntArray(newSize + 3 * needed) // add 2x needed
            System.arraycopy(ints, 0, expanded, 0, used)
            ints = expanded
        }
    }

    private fun insert(
        prev: DelegatePredicate?, next: DelegatePredicate?, size: Int, unit: Int,
        drawable: Drawable?, drawableBounds: ViewBounds, viewBoundsNegotiation: BoundsNegotiation, drawableGravity: Int,
    ) {
        var len = objs.size
        ints[len++] = if (size == 0) WRAP_CONTENT else ComplexDimension.createComplexDimension(size, unit)
        ints[len++] = (drawableBounds.ordinal shl 16) or viewBoundsNegotiation.ordinal
        ints[len++] = drawableGravity
        objs += prev
        objs += next
        objs += drawable
    }

    @JvmField
    protected val decorations = SparseLongArray()
    final override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val myHolder = parent.getChildViewHolder(view) ?: return outRect.setEmpty()
        if (forAdapter != null && myHolder.bindingAdapter != forAdapter) return outRect.setEmpty()
        val pos = myHolder.bindingAdapterPosition
        var myDecorations = 0L
        val mergedPos = if (pos < 0) {
            val lp = myHolder.layoutPosition
            myDecorations = decorations.get(lp)
            -1 - lp
        } else {
            val myDelegate = delegapter.delegateAt(pos)
            val nextDelegate = delegateAtOrNull(pos + 1) // will just be null if next item is from another adapter

            repeat(objs.size / 3) { index ->
                decorAt(ints, objs, index) { pp, np, _, _, _, _, _ ->
                    if (pp == null) {
                        if (np!!(myDelegate)) myDecorations = myDecorations or (1L shl index)
                    } else if (pp(myDelegate) && (np == null || nextDelegate != null && np(nextDelegate))) {
                        myDecorations = myDecorations or (1L shl index)
                    }
                }
            }

            if (myDecorations == 0L) decorations.delete(pos)
            else decorations.put(pos, myDecorations)
            pos
        }

        if (myDecorations == 0L)
            return outRect.setEmpty()

        val dm = parent.resources.displayMetrics
        var before = 0
        var after = 0
        myDecorations.forEachBit { _, index ->
            decorAt(ints, objs, index) { pp, _, dimension, drawable, _, _, _ ->
                if (pp == null) {
                    before += size(mergedPos, dimension, dm, drawable, view)
                } else {
                    after += size(mergedPos, dimension, dm, drawable, view)
                }
            }
        }

        val h = orientation == HORIZONTAL
        outRect.set(if (h) before else 0, if (!h) before else 0, if (h) after else 0, if (!h) after else 0)
    }

    private fun delegateAtOrNull(position: Int): Delegate<*>? =
        delegapter.takeIf { it.size > position }?.delegateAt(position)

    protected fun size(
        position: Int, dimension: Int, displayMetrics: DisplayMetrics, drawable: Drawable?, forView: View,
    ): Int =
        if (dimension == WRAP_CONTENT) {
            drawable!!.apply {
                level = position
                state = forView.drawableState
            }
            tmpInts1[0] = drawable.intrinsicWidth
            tmpInts1[1] = drawable.intrinsicHeight
            tmpInts1[orientation]
        } else complexToDimensionPixelOffset(dimension, displayMetrics)

    @JvmField protected val rect1 = Rect()
    private val rect2 = Rect()
    @JvmField protected val rectF = RectF()
    final override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val dm = parent.resources.displayMetrics
        for (index in 0 until parent.childCount) {
            drawFor(c, parent.getChildAt(index), parent, dm)
        }
    }

    private fun drawFor(c: Canvas, view: View, parent: RecyclerView, dm: DisplayMetrics) {
        val myHolder = parent.getChildViewHolder(view) ?: return
        if (forAdapter != null && myHolder.bindingAdapter != forAdapter) return
        val pos = myHolder.bindingAdapterPosition
        val lp = myHolder.layoutPosition
        val myDecorations = decorations.get(if (pos >= 0) pos else lp)
        if (myDecorations == 0L) return

        val mergedPos = if (pos >= 0) pos else -1 - lp
        var before = 0
        var after = 0
        myDecorations.forEachBit { _, index ->
            decorAt(ints, objs, index) { pp, np, dimension, drawable, bounds, negotiation, gravity ->
                drawable?.apply {
                    level = mergedPos
                    state = view.drawableState
                    alpha = (view.alpha * 255).toInt()
                    tmpInts1[0] = intrinsicWidth
                    tmpInts1[1] = intrinsicHeight
                }
                val size =
                    if (dimension == WRAP_CONTENT) tmpInts1[orientation]
                    else complexToDimensionPixelOffset(dimension, dm)

                if (drawable != null) {
                    bounds.of(view, rect1, rectF)

                    val dir = view.layoutDirection
                    if (pp == null) {
                        rect1.locateBefore(before, size)
                    } else {
                        rect1.locateAfter(after, size)
                        if (np != null) parent.findViewHolderForLayoutPosition(lp + 1)?.itemView?.let { nextView ->
                            negotiateBounds(negotiation, dir, bounds, nextView)
                        }
                    }

                    Gravity.apply(gravity, tmpInts1[0], tmpInts1[1], rect1, rect2, dir)
                    drawable.bounds = rect2
                    drawable.draw(c)
                }
                if (pp == null) before += size
                else after += size
            }
        }
    }

    private fun Rect.locateBefore(before: Int, size: Int) {
        if (orientation == HORIZONTAL) {
            right = left - before
            left = right - size
        } else {
            bottom = top - before
            top = bottom - size
        }
    }

    private fun Rect.locateAfter(after: Int, size: Int) {
        if (orientation == HORIZONTAL) {
            left = right + after
            right = left + size
        } else {
            top = bottom + after
            bottom = top + size
        }
    }

    private fun negotiateBounds(negotiation: BoundsNegotiation, dir: Int, bounds: ViewBounds, nextView: View) {
        val (tmpIntW, tmpIntH) = tmpInts1
        rect1.negotiableBoundsTo(tmpInts1, negotiation, dir)
        bounds.of(nextView, rect2, rectF)
        rect2.negotiableBoundsTo(tmpInts2, negotiation, dir)
        negotiation.negotiate(tmpInts2, tmpInts1)
        if (orientation == HORIZONTAL) {
            rect1.top = tmpInts2[0]
            rect1.bottom = tmpInts2[1]
        } else {
            if (dir == LAYOUT_DIRECTION_RTL) negotiation.maybeSwap(tmpInts2)
            rect1.left = tmpInts2[0]
            rect1.right = tmpInts2[1]
        }
        tmpInts1[0] = tmpIntW
        tmpInts1[1] = tmpIntH
    }
    private fun Rect.negotiableBoundsTo(dst: IntArray, negotiation: BoundsNegotiation, layoutDirection: Int) {
        if (orientation == HORIZONTAL) {
            dst[0] = top
            dst[1] = bottom
        } else {
            dst[0] = left
            dst[1] = right
            if (layoutDirection == LAYOUT_DIRECTION_RTL) negotiation.maybeSwap(dst)
        }
    }
}
@Suppress("UNCHECKED_CAST")
private inline fun decorAt(
    ints: IntArray, objs: ArrayList<*>,
    index: Int, block: (
        prev: DelegatePredicate?, next: DelegatePredicate?, size: Int,
        drawable: Drawable?, drawableBounds: ViewBounds, viewBoundsNegotiation: BoundsNegotiation, drawableGravity: Int,
    ) -> Unit
) {
    var i = 3 * index
    val size = ints[i]
    val prev = objs[i++] as DelegatePredicate?
    val bounds = ints[i]
    val next = objs[i++] as DelegatePredicate?
    val drawableGravity = ints[i]
    val drawable = objs[i++] as Drawable?
    block(
        prev, next, size,
        drawable, VIEW_BOUNDS_VALUES[bounds ushr 16], BOUNDS_NEGOTIATION_VALUES[bounds and 0xFFFF], drawableGravity,
    )
}
private val VIEW_BOUNDS_VALUES = ViewBounds.values()
private val BOUNDS_NEGOTIATION_VALUES = BoundsNegotiation.values()

@PublishedApi @RequiresApi(18) internal class DebugDecor(
    delegapter: MutableDelegapter,
    orientation: Int,
    forAdapter: RecyclerView.Adapter<*>?,
    private val delegates: Boolean,
    private val spaces: Boolean,
) : Decor(delegapter, orientation, forAdapter) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fm = Paint.FontMetricsInt()
    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val dm = parent.resources.displayMetrics
        paint.strokeWidth = dm.density // 1dp
        for (index in 0 until parent.childCount) {
            drawDebug(c, parent.getChildAt(index), parent, dm)
        }
    }
    private fun drawDebug(c: Canvas, view: View, parent: RecyclerView, dm: DisplayMetrics) {
        val myHolder = parent.getChildViewHolder(view) ?: return
        if (forAdapter != null && myHolder.bindingAdapter != forAdapter) return
        val pos = myHolder.bindingAdapterPosition

        ViewBounds.WithMargins.of(view, rect1, rectF)

        if (delegates && pos >= 0) {
            c.drawDelegate(view, delegapter.delegateAt(pos), rect1.left, rect1.top, dm)
        }

        var myDecorations = 0L
        var lp = 0
        if (spaces &&
            decorations.get(if (pos >= 0) pos else myHolder.layoutPosition.also { lp = it }).also { myDecorations = it } != 0L) {
            val mergedPos = if (pos >= 0) pos else -1 - lp
            var before = 0
            var after = 0
            myDecorations.forEachBit { _, index ->
                decorAt(ints, objs, index) { pp, np, dimension, drawable, _, _, _ ->
                    if (pp == null) {
                        setColorWithAlpha(FOREGROUND_BEFORE, view)
                        before += c.drawSpace(mergedPos, parent, dimension, before, rect1, -1, dm, drawable, view)
                    } else {
                        setColorWithAlpha(if (np == null) FOREGROUND_AFTER else FOREGROUND_BETWEEN, view)
                        after += c.drawSpace(mergedPos, parent, dimension, after, rect1, 1, dm, drawable, view)
                    }
                }
            }
        }
    }

    private fun Canvas.drawDelegate(view: View, myDelegate: Delegate<*>, left: Int, top: Int, dm: DisplayMetrics) {
        dm.guessTextSize(min(view.width, view.height), 12f, 22f)
        var padding = paint.textSize / 4f
        val toS = myDelegate.toString()
        setColorWithAlpha(BACKGROUND, view)
        drawRect(
            left.toFloat(), top.toFloat(),
            left + paint.measureFun(toS) + padding,
            top - fm.ascent + fm.descent + padding,
            paint
        )
        setColorWithAlpha(FOREGROUND_BETWEEN, view)
        padding /= 2f
        drawFun(
            toS,
            left + padding,
            top - fm.ascent + padding,
            paint.also { it.textAlign = Paint.Align.LEFT },
        )
    }
    private fun setColorWithAlpha(color: Int, from: View) {
        paint.color = color
        paint.alpha = (paint.alpha * from.alpha).toInt()
    }
    private fun Canvas.drawSpace(
        pos: Int, parent: RecyclerView, dimension: Int, space: Int, bnds: Rect, direction: Int, dm: DisplayMetrics,
        drawable: Drawable?, forView: View,
    ): Int {
        val dimensionPx = size(pos, dimension, dm, drawable, forView)
        dm.guessTextSize(dimensionPx, 10f, 20f)
        val sx: Int
        val sy: Int
        val ex: Int
        val ey: Int
        val tx: Int
        val ty: Int
        if (orientation == HORIZONTAL) {
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
}
// random bright colors visible both on black and white
private const val FOREGROUND_BEFORE = 0xCC22EE66.toInt()
private const val FOREGROUND_BETWEEN = 0xCC22AAAA.toInt()
private const val FOREGROUND_AFTER = 0xCC2266EE.toInt()
private const val BACKGROUND = 0x33000000

private inline fun Long.forEachBit(func: (setBitIdx: Int, bitIdx: Int) -> Unit) {
    var idx = 0
    var ord = 0
    var bits = this
    while (bits != 0L) {
        if ((bits and 1L) == 1L) {
            func(idx++, ord)
        }

        bits = bits ushr 1
        ord++
    }
}
