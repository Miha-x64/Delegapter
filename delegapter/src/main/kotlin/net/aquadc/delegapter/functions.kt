@file:JvmName("Functions")
package net.aquadc.delegapter

import android.graphics.Canvas
import android.graphics.Paint
import net.aquadc.delegapter1.ViewType
import java.lang.reflect.ParameterizedType
import kotlin.jvm.internal.CallableReference
import kotlin.jvm.internal.FunctionReference
import kotlin.jvm.internal.Lambda
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

@PublishedApi internal abstract class Named(private val name: Any?) {
    final override fun toString(): String = name.toString()
}

// used by DebugDecor

internal fun StringBuilder.appendFun(function: Function<*>): StringBuilder =
    ((function as? PropertyReference)?.owner as? KClass<*>)?.qualifiedName
        ?.let { append(it).append("::").append(function.name) }
        ?: (function as? FunctionReference)?.let {
            if (it.boundReceiver !== CallableReference.NO_RECEIVER)
                append(it.boundReceiver)
            append("::")
            (it.owner as? kotlin.jvm.internal.PackageReference)?.jClass?.`package`?.let { append(it.name).append('.') }
            append(it.name)
        }
        ?: function.toString().let { toS -> append(toS, toS.eatFunctionPrefix, toS.eatUnavailableReflectComplaint) }

internal fun Paint.measureFun(fToString: String) =
    measureText(fToString, fToString.eatFunctionPrefix, fToString.eatUnavailableReflectComplaint)

internal fun Canvas.drawFun(fToString: String, x: Float, y: Float, paint: Paint) =
    drawText(fToString, fToString.eatFunctionPrefix, fToString.eatUnavailableReflectComplaint, x, y, paint)

private val String.eatFunctionPrefix
    get() = if (startsWith("function ")) "function ".length else 0

internal val String.eatUnavailableReflectComplaint
    get() = length -
        if (endsWith(" (Kotlin reflection is not available)")) " (Kotlin reflection is not available)".length else 0

internal fun StringBuilder.appendVHF(factory: ViewType): StringBuilder {
    if (factory is Named) return append('“').append(factory).append('”')
    val rt =
        (factory.takeIf { it.javaClass.superclass == Lambda::class.java }
            ?.javaClass?.genericInterfaces?.getOrNull(0) as? ParameterizedType)
            ?.actualTypeArguments?.getOrNull(1)
    return if (rt == null) appendFun(factory) else append('{').append(' ').append("parent").append(" -> ").append((rt as? Class<*>)?.name ?: rt).append(' ').append('}')
}
