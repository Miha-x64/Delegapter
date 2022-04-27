package net.aquadc.delegapter

import android.graphics.Rect
import android.util.SparseIntArray
import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.RecyclerView


inline fun MutableDelegapter.decor(
    @RecyclerView.Orientation orientation: Int,
    configure: Decor.() -> Unit
): RecyclerView.ItemDecoration =
    Decor(this, orientation).apply(configure)

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

        val nextDelegate = delegapter.takeIf { it.size > pos + 1 }?.delegateAt(pos + 1) ?: return outRect.setEmpty()
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

    private fun Rect.setAfter(space: Int): Unit = set(
        0,
        0,
        if (orientation == RecyclerView.HORIZONTAL) space else 0,
        if (orientation == RecyclerView.VERTICAL) space else 0,
    )
}
