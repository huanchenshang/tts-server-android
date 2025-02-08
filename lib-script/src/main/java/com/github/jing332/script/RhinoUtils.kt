package com.github.jing332.script

import com.github.jing332.script.rhino.RhinoContextFactory
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.Undefined
import kotlin.time.measureTimedValue

fun <R> withRhinoContext(block: (Context) -> R): R {
    val cx = RhinoContextFactory.enterContext()
    try {
        return block(cx)
    } finally {
        Context.exit()
    }
}

fun <R> ensureArgumentsLength(
    args: Array<out Any?>?,
    count: Int,
    block: (args: Array<out Any?>) -> R
): R = ensureArgumentsLength(args, 1..count, block)

fun <R> ensureArgumentsLength(
    args: Array<out Any?>?,
    range: IntRange,
    block: (args: Array<out Any?>) -> R
): R {
    checkNotNull(args)

    if (range.contains(args.size))
        return block(args)

    throw IllegalArgumentException("Method argument count error, need $range arguments, actual $args")
}


fun Scriptable.invokeMethod(scope: Scriptable, name: String, args: Array<Any?>?): Any? {
    val method = get(name, this) as? Function ?: throw NoSuchMethodException(name)
    return withRhinoContext { cx ->
        method.call(cx, scope, this, args)
    }.run { if (this is Undefined) null else this }
}

object RhinoUtils {
}