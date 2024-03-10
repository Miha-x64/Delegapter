@file:JvmName("ItemDecorations")
@file:Suppress("KDocMissingDocumentation", "MemberVisibilityCanBePrivate") // FIXME temporary
package net.aquadc.delegapter1.decor

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.State
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import net.aquadc.delegapter.decor.ComplexDimension.ComplexDimensionUnit
import net.aquadc.delegapter.decor.GravityCompote
import net.aquadc.delegapter.decor.GravityCompote.GravityFlags
import net.aquadc.delegapter.decor.GravityCompote.SideGravity


/**
 * Constructs item decoration and [configure]s it
 */
inline fun ItemDecoration(
    noinline selectAdapter: (RecyclerView.Adapter<*>) -> Boolean = { true },
    debugOffsets: Boolean = false,
    configure: ItemDecorationBuilder.() -> Unit,
): RecyclerView.ItemDecoration {
    // TODO debugOffsets
    return ItemDecorationBuilder().apply(configure).build(selectAdapter)
}

private const val WRAP_CONTENT = Int.MIN_VALUE

/**
 * Provides interface for configuring item decoration
 */
class ItemDecorationBuilder @PublishedApi internal constructor() {

    private val offsetEls = ArrayList<DecorationElement>()
    private val drawUnderEls = ArrayList<DecorationElement>()
    private val drawOverEls = ArrayList<DecorationElement>()

    /*@PublishedApi @JvmField internal var groups: Int = 0
    inline fun group(
        name: String = "group#${groups+1}",
        noinline isGroupStart: (ViewHolder) -> Boolean = defaultGroupStart,
        noinline inGroup: (ViewHolder) -> Boolean = defaultInGroup,
        noinline isGroupEnd: (ViewHolder) -> Boolean = defaultGroupEnd(isGroupStart),
        absentStartOffset: Int = 0,
        absentEndOffset: Int = 0,
        configure: ItemDecorationBuilder.() -> Unit,
    ): DecorationPlacement {
        val g = group(name, isGroupStart, inGroup, isGroupEnd, absentStartOffset, absentEndOffset)
        ItemDecorationBuilder().apply(configure).let { TODO("orientation, forAdapter, debugOffsets") }
        // TODO and attach group contents
        return g
    }
    fun group(
        name: String = "group#${groups+1}",
        isGroupStart: (ViewHolder) -> Boolean = defaultGroupStart,
        inGroup: (ViewHolder) -> Boolean = defaultInGroup,
        isGroupEnd: (ViewHolder) -> Boolean = defaultGroupEnd(isGroupStart),
        absentStartOffset: Int = 0,
        absentEndOffset: Int = 0,
    ): DecorationPlacement {
        groups++

        return { _, _, _ -> TODO() }
    }

    @PublishedApi internal companion object {
        @PublishedApi @JvmField internal val defaultGroupStart: (ViewHolder) -> Boolean = {
            (it.itemView.parent as RecyclerView).indexOfChild(it.itemView) == 0
        }
        @PublishedApi @JvmField internal val defaultInGroup: (ViewHolder) -> Boolean = {
            true
        }
        @PublishedApi internal fun defaultGroupEnd(isGroupStart: (ViewHolder) -> Boolean): (ViewHolder) -> Boolean = {
            val parent = it.itemView.parent as RecyclerView
            val myIndex = parent.indexOfChild(it.itemView)
            myIndex == parent.childCount - 1 || isGroupStart(parent.getChildViewHolder(parent.getChildAt(myIndex + 1)))
        }
    }*/

    fun size(
        size: Int, @ComplexDimensionUnit unit: Int = TypedValue.COMPLEX_UNIT_DIP,
    ): SizedDecorationItem = // any size is OK, maybe you wanna draw out of bounds or reset some offset
        SizedDecorationItem(size, unit)
    fun draw(
        drawable: Drawable, drawableBounds: ViewBounds = ViewBounds.Padded, drawOver: Boolean = false,
    ): DrawableDecorationItem =
        DrawableDecorationItem(drawable, Gravity.FILL, drawableBounds, drawOver)
    fun draw(
        drawables: Drawable.ConstantState, drawableBounds: ViewBounds = ViewBounds.Padded, drawOver: Boolean = false,
    ): DrawableDecorationItem =
        DrawableDecorationItem(drawables, Gravity.FILL, drawableBounds, drawOver)

    open inner class DecorationItem internal constructor(
        @JvmField internal val size: Int,
        @JvmField @ComplexDimensionUnit internal val unit: Int,

        @JvmField internal val drawableOrFactory: Any?, // Drawable | Drawable.ConstantState | null
        @JvmField @GravityFlags internal val drawableGravity: Int,
        @JvmField internal val drawableBounds: ViewBounds,
        @JvmField internal val drawOver: Boolean,
    )
    inner class SizedDecorationItem internal constructor(
        size: Int,
        @ComplexDimensionUnit unit: Int = TypedValue.COMPLEX_UNIT_DIP,
    ): DecorationItem(size, unit, null, Gravity.FILL, NoBounds, false) {
        fun draw(
            drawable: Drawable, drawableBounds: ViewBounds = ViewBounds.Padded, @GravityFlags drawableGravity: Int = Gravity.FILL, drawOver: Boolean = false, // TODO affineToView: Boolean
        ): DecorationItem =
            DecorationItem(size, unit, drawable, drawableGravity, drawableBounds, drawOver)
        fun draw(
            drawables: Drawable.ConstantState, drawableBounds: ViewBounds = ViewBounds.Padded, @GravityFlags drawableGravity: Int = Gravity.FILL, drawOver: Boolean = false, // TODO affineToView: Boolean
        ): DecorationItem =
            DecorationItem(size, unit, drawables, drawableGravity, drawableBounds, drawOver)
    }
    inner class DrawableDecorationItem internal constructor( // TODO implement Drawable callback
        drawableOrFactory: Any, // Drawable | Drawable.ConstantState
        @GravityFlags drawableGravity: Int,
        drawableBounds: ViewBounds,
        drawOver: Boolean,
    ): DecorationItem(WRAP_CONTENT, TypedValue.COMPLEX_UNIT_DIP, drawableOrFactory, drawableGravity, drawableBounds, drawOver) {
        fun size(size: Int, @ComplexDimensionUnit unit: Int = TypedValue.COMPLEX_UNIT_DIP, @GravityFlags drawableGravity: Int = Gravity.FILL): DecorationItem =
            DecorationItem(size, unit, drawableOrFactory, drawableGravity, drawableBounds, drawOver)
    }

    @RequiresApi(18) // don't wanna version-check only because of getLayoutDirection for dead Android versions
    fun <D : DecorationItem> D.aside(what: DecorationPlacement = anyViewHolder /* TODO or group */, @SideGravity gravity: Int): D {
        require(when (gravity) {
            Gravity.START, Gravity.END, Gravity.LEFT, Gravity.RIGHT, Gravity.TOP, Gravity.BOTTOM -> true
            // TODO maybe support LEFT|RIGHT, TOP|BOTTOM
            else -> false
        }) {
            "Side gravity must be one of: START, END, LEFT, RIGHT, TOP, BOTTOM. " +
                "Got ${GravityCompote.toString(gravity)} (0x${gravity.toString(16)})"
        }

        val el = object : DecorationElement(drawableOrFactory) {
            override fun addItemOffsets(
                outRect: Rect, parent: RecyclerView, state: State,
                current: ViewHolder,
            ) {
                if (what(current)) {
                    val view = current.itemView
                    addAside(
                        Gravity.getAbsoluteGravity(gravity, view.layoutDirection),
                        outRect,
                        sizeFor(drawableFor(view), view.resources.displayMetrics)
                    )
                }
            }
            override fun onDraw(
                c: Canvas, parent: RecyclerView, state: State, isOver: Boolean,
                current: ViewHolder,
            ) {
                val itemView = current.itemView
                if (isOver == drawOver && what(current))
                    drawableFor(itemView)?.let { locateAndDraw(c, itemView, it) }
            }

            private val rect1 = Rect()
            private val rect2 = Rect()
            private fun locateAndDraw(c: Canvas, view: View, drawable: Drawable) {
                drawableBounds.ofView(view, 3, rect1, false)

                val absGravity = Gravity.getAbsoluteGravity(gravity, view.layoutDirection)
                rect1.locateAside(absGravity, sizeFor(drawable, view.resources.displayMetrics))

                Gravity.apply(drawableGravity, drawable.intrinsicWidth, drawable.intrinsicHeight, rect1, rect2, view.layoutDirection)

                draw(c, view, rect2, drawable)
            }
            private fun sizeFor(drawable: Drawable?, dm: DisplayMetrics) = when {
                size == WRAP_CONTENT && drawable != null ->
                    if (gravity and (Gravity.AXIS_SPECIFIED shl Gravity.AXIS_X_SHIFT) != 0) drawable.intrinsicWidth
                    else drawable.intrinsicHeight
                size == WRAP_CONTENT ->
                    throw AssertionError()
                else ->
                    TypedValue.applyDimension(unit, size.toFloat(), dm).toInt()
            }
            private fun addAside(absSideGravity: Int, outRect: Rect, size: Int) {
                when (absSideGravity) {
                    Gravity.LEFT -> outRect.left += size
                    Gravity.TOP -> outRect.top += size
                    Gravity.RIGHT -> outRect.right += size
                    Gravity.BOTTOM -> outRect.bottom += size
                    else -> throw AssertionError()
                }
            }
            private fun Rect.locateAside(@SideGravity absSide: Int, size: Int) {
                when (absSide) {
                    Gravity.LEFT -> { right = left; left -= size }
                    Gravity.TOP -> { bottom = top; top -= size }
                    Gravity.RIGHT -> { left = right; right += size }
                    Gravity.BOTTOM -> { top = bottom; bottom += size }
                    else -> throw AssertionError()
                }
            }
        }
        if (size != 0) offsetEls += el
        if (drawableOrFactory != null)
            (if (drawOver) drawOverEls else drawUnderEls) += el
        // FIXME yea we temporary split both to lists and by checking drawOver input variable

        return this
    }
    @RequiresApi(18)
    fun DrawableDecorationItem.on(what: DecorationPlacement = anyViewHolder /* TODO or group */): DrawableDecorationItem {
        val el = object : DecorationElement(drawableOrFactory) {
            override fun onDraw(
                c: Canvas, parent: RecyclerView, state: State, isOver: Boolean,
                current: ViewHolder,
            ) {
                val itemView = current.itemView
                if (isOver == drawOver && what(current))
                    locateAndDraw(c, itemView, drawableFor(itemView)!!)
            }

            private val rect1 = Rect()
            private val rect2 = Rect()
            private fun locateAndDraw(c: Canvas, view: View, drawable: Drawable) {
                drawableBounds.ofView(view, 3, rect1, false)
                val absGravity = Gravity.getAbsoluteGravity(drawableGravity, view.layoutDirection)
                Gravity.apply(absGravity, drawable.intrinsicWidth, drawable.intrinsicHeight, rect1, rect2)
                draw(c, view, rect2, drawable)
            }
        }
        if (drawableOrFactory != null)
            (if (drawOver) drawOverEls else drawUnderEls) += el
        return this
    }

    @PublishedApi internal fun build(selectAdapter: (RecyclerView.Adapter<*>) -> Boolean): RecyclerView.ItemDecoration {
        val offsetEls = offsetEls.takeIf(List<*>::isNotEmpty)?.toTypedArray()
        val drawUnderEls = drawUnderEls.takeIf(List<*>::isNotEmpty)?.toTypedArray()
        val drawOverEls = drawOverEls.takeIf(List<*>::isNotEmpty)?.toTypedArray()
        return object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
                outRect.setEmpty()
                if (offsetEls != null)
                    parent.applicableVHFor(view)?.let { current ->
                        offsetEls.forEach { it.addItemOffsets(outRect, parent, state, current) }
                    }
            }
            override fun onDraw(c: Canvas, parent: RecyclerView, state: State) {
                if (drawUnderEls != null)
                    draw(c, parent, state, false, drawUnderEls)
            }
            override fun onDrawOver(c: Canvas, parent: RecyclerView, state: State) {
                if (drawOverEls != null)
                    draw(c, parent, state, true, drawOverEls)
            }
            private fun draw(c: Canvas, parent: RecyclerView, state: State, isOver: Boolean, els: Array<out DecorationElement>) {
                repeat(parent.childCount) { i ->
                    parent.applicableVHFor(parent.getChildAt(i))?.let { current ->
                        els.forEach { it.onDraw(c, parent, state, isOver, current) }
                    }
                }
            }
            private fun RecyclerView.applicableVHFor(view: View): ViewHolder? =
                getChildViewHolder(view).takeIf { selectAdapter(it.bindingAdapter!!) }
        }
    }
}

private val NoBounds = object : ViewBounds/*, BoundsDelimitation*/ {
    override fun ofView(child: View, axes: Int, into: Rect, inParent: Boolean): Unit =
        throw AssertionError()
    /*override fun negotiate(first: Rect, second: Rect, axes: Int, layoutDirection: Int, into: Rect): Unit =
        throw AssertionError()*/
}

private abstract class DecorationElement(
    private val drawableOrFactory: Any?,
) {
    open fun addItemOffsets(
        outRect: Rect, parent: RecyclerView, state: State,
        current: ViewHolder,
    ) {}
    open fun onDraw(
        c: Canvas, parent: RecyclerView, state: State,
        isOver: Boolean, current: ViewHolder,
    ) {}

    protected fun drawableFor(view: View): Drawable? = when (drawableOrFactory) {
        is Drawable -> drawableOrFactory
        is Drawable.ConstantState -> TODO()
        null -> null
        else -> throw AssertionError()
    }

    protected fun draw(c: Canvas, affineTo: View, relativeBounds: Rect, what: Drawable) {
        what.state = affineTo.drawableState
        what.alpha = (affineTo.alpha * 255).toInt()
        what.bounds = relativeBounds
        val sc = c.save()
        c.translate(affineTo.left.toFloat(), affineTo.top.toFloat())
        c.concat(affineTo.matrix)
        what.draw(c)
        c.restoreToCount(sc)
    }
}

class DebugDelegates(
    private val showIndex: Boolean,
    private val showDelegate: Boolean,
    private val showData: Boolean,
) : Drawable.ConstantState() {
    override fun newDrawable(): Drawable = object : Drawable() {
        override fun draw(canvas: Canvas) {
            TODO("Not yet implemented")
        }
        override fun setAlpha(alpha: Int) {
            TODO("Not yet implemented")
        }
        override fun setColorFilter(colorFilter: ColorFilter?) {
            TODO("Not yet implemented")
        }
        override fun getOpacity(): Int {
            TODO("Not yet implemented")
        }

        override fun getConstantState(): ConstantState =
            this@DebugDelegates
    }

    override fun getChangingConfigurations(): Int =
        0
}
