@file:JvmName("Spans")
package net.aquadc.delegapter

import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup


/**
 * Creates [span size lookup][SpanSizeLookup] determining span size
 * based on position, an according item, and its delegate.
 *
 * Caches [span indices][SpanSizeLookup.setSpanIndexCacheEnabled]
 * and [group indices][SpanSizeLookup.setSpanGroupIndexCacheEnabled].
 */
inline fun MutableDelegapter.spanSizeLookup(
    crossinline getSpanSize: (position: Int, item: Any?, delegate: Delegate<*>) -> Int,
): SpanSizeLookup = object : SpanSizeLookup() {
    init {
        isSpanIndexCacheEnabled = true
        isSpanGroupIndexCacheEnabled = true
    }
    override fun getSpanSize(position: Int): Int =
        getSpanSize.invoke(position, itemAt(position), delegateAt(position))
}
