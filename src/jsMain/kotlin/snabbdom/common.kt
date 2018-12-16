package snabbdom

import kotlin.browser.window
import kotlin.js.Json

external fun delete(p: dynamic): Boolean = definedExternally

fun Any._get(key: dynamic): dynamic {
    val self: dynamic = this
    return self[key]
}

fun Any._set(key: dynamic, value: dynamic){
    val self: dynamic = this
    self[key] = value
}

fun newObj(): Json {
    val o: dynamic = js("{}")
    return o
}

fun jsObjKeys(obj: dynamic): Array<String> {
    return js("Object").keys(obj)
}

val raf = fun(fn: dynamic) { window.requestAnimationFrame(fn) }
val nextFrame = fun(fn: dynamic) { raf.invoke(fun() { raf(fn); }) }

fun setNextFrame(obj: dynamic, prop: String, value: dynamic): Unit {
    nextFrame(fun() { obj[prop] = value })
}
