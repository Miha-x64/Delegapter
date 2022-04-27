package net.aquadc.delegapter

internal fun StringBuilder.appendFun(function: Function<*>): StringBuilder =
    function.toString().let { toS -> append(toS, toS.eatFunctionPrefix, toS.eatFunctionPostfix) }

private val String.eatFunctionPrefix
    get() = if (startsWith("function ")) "function ".length else 0

private val String.eatFunctionPostfix
    get() = length -
        if (endsWith(" (Kotlin reflection is not available)")) " (Kotlin reflection is not available)".length else 0
