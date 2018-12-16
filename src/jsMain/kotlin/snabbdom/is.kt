package snabbdom

fun isArray(arg: dynamic) =
    arg is Array<*>

fun isPrimitive(s: dynamic) =
    s is String || s is Int