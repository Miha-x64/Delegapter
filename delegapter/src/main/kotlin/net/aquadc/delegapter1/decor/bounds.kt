package net.aquadc.delegapter1.decor

import android.graphics.Rect
import android.view.View


interface ViewBounds {
    fun ofView(child: View, axes: Int, into: Rect, inParent: Boolean)
    companion object {
        @JvmField val DecoratedWithMargins: ViewBounds = net.aquadc.delegapter.decor.ViewBounds.DecoratedWithMargins
        @JvmField val WithMargins: ViewBounds = net.aquadc.delegapter.decor.ViewBounds.WithMargins
        @JvmField val Padded: ViewBounds = net.aquadc.delegapter.decor.ViewBounds.Padded
        @JvmField val Dispadded: ViewBounds = net.aquadc.delegapter.decor.ViewBounds.Unpadded
    }
}
