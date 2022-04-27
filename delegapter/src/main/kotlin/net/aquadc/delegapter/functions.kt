package net.aquadc.delegapter

import android.graphics.Canvas
import android.graphics.Paint

internal fun StringBuilder.appendFun(function: Function<*>): StringBuilder =
    function.toString().let { toS -> append(toS, toS.eatFunctionPrefix, toS.eatFunctionPostfix) }

internal fun Paint.measureFun(fToString: String) =
    measureText(fToString, fToString.eatFunctionPrefix, fToString.eatFunctionPostfix)

internal fun Canvas.drawFun(fToString: String, x: Float, y: Float, paint: Paint) =
    drawText(fToString, fToString.eatFunctionPrefix, fToString.eatFunctionPostfix, x, y, paint)

private val String.eatFunctionPrefix
    get() = if (startsWith("function ")) "function ".length else 0

private val String.eatFunctionPostfix
    get() = length -
        if (endsWith(" (Kotlin reflection is not available)")) " (Kotlin reflection is not available)".length else 0
