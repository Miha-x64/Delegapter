package net.aquadc.delegapter

import androidx.recyclerview.widget.ListUpdateCallback

/**
 * No-op implementation of [ListUpdateCallback].
 * Intended for use with parent [Delegapter].
 * @author Mike Gorünóv
 */
object NullListUpdateCallback : ListUpdateCallback {
    override fun onInserted(position: Int, count: Int) {}
    override fun onRemoved(position: Int, count: Int) {}
    override fun onMoved(fromPosition: Int, toPosition: Int) {}
    override fun onChanged(position: Int, count: Int, payload: Any?) {}
}