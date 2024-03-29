@file:JvmName("Functions")
package net.aquadc.delegapter

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.jvm.internal.PropertyReference
import kotlin.reflect.KClass

/**
 * Makes [function] named by overriding its [Any.toString].
 */
@Suppress("UNCHECKED_CAST") @JvmName("named")
inline operator fun <T, R> String.invoke(crossinline function: (T) -> R): (T) -> R =
    object : Named(this), (Any?) -> Any? {
        override fun invoke(p1: Any?): Any? = function(p1 as T)
    } as (T) -> R

@PublishedApi internal abstract class Named(private val name: String) {
    final override fun toString(): String = name
}

// used by DebugDecor

internal fun StringBuilder.appendFun(function: Function<*>): StringBuilder =
    ((function as? PropertyReference)?.owner as? KClass<*>)?.qualifiedName
        ?.let { append(it).append("::").append(function.name) }
        ?: function.toString().let { toS -> append(toS, toS.eatFunctionPrefix, toS.eatUnavailableReflectComplaint) }

internal fun Paint.measureFun(fToString: String) =
    measureText(fToString, fToString.eatFunctionPrefix, fToString.eatUnavailableReflectComplaint)

internal fun Canvas.drawFun(fToString: String, x: Float, y: Float, paint: Paint) =
    drawText(fToString, fToString.eatFunctionPrefix, fToString.eatUnavailableReflectComplaint, x, y, paint)

private val String.eatFunctionPrefix
    get() = if (startsWith("function ")) "function ".length else 0

private val String.eatUnavailableReflectComplaint
    get() = length -
        if (endsWith(" (Kotlin reflection is not available)")) " (Kotlin reflection is not available)".length else 0
