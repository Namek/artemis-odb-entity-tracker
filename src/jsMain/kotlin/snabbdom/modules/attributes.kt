package snabbdom.modules

import org.w3c.dom.Element
import snabbdom.*
import kotlin.js.Json

// export type Attrs = Record<string, string | number | boolean>
interface Attrs : Json
//operator fun Attrs.get(key: String): dynamic = this._get(key)
//operator fun Attrs.set(key: String, value: dynamic): Unit { this._set(key, value) }


val xlinkNS = "http://www.w3.org/1999/xlink"
val xmlNS = "http://www.w3.org/XML/1998/namespace"
val colonChar = 58
val xChar = 120

private fun updateAttrs(oldVnode: VNode, vnode: VNode): Unit {
    val elm: Element = vnode.elm as Element
    var oldAttrs = oldVnode.data?.attrs
    var attrs = vnode.data?.attrs

    if (oldAttrs == null && attrs == null) return
    if (oldAttrs === attrs) return
    oldAttrs = oldAttrs ?: newObj().unsafeCast<Attrs>()
    attrs = attrs ?: newObj().unsafeCast<Attrs>()

    // update modified attributes, add new attributes
    for (key: dynamic in jsObjKeys(attrs)) {
        val cur = attrs[key]!!
        val old = oldAttrs[key]
        if (old !== cur) {
            if (cur == true) {
                elm.setAttribute(key, "")
            } else if (cur == false) {
                elm.removeAttribute(key)
            } else {
                if (key.charCodeAt(0) != xChar) {
                    elm.setAttribute(key, cur.toString())
                } else if (key.charCodeAt(3) == colonChar) {
                    // Assume xml namespace
                    elm.setAttributeNS(xmlNS, key, cur.toString())
                } else if (key.charCodeAt(5) == colonChar) {
                    // Assume xlink namespace
                    elm.setAttributeNS(xlinkNS, key, cur.toString())
                } else {
                    elm.setAttribute(key, cur.toString())
                }
            }
        }
    }
    // remove removed attributes
    // use `in` operator since the previous `for` iteration uses it (.i.e. add even attributes with undefined value)
    // the other option is to remove all attributes with value == undefined
    for (key in jsObjKeys(oldAttrs)) {
        val value = attrs[key]
        if (value == null) {
            elm.removeAttribute(key)
        }
    }
}

class AttributesModule : Module() {
    override val create: CreateHook?
        get() = ::updateAttrs

    override val update: UpdateHook?
        get() = ::updateAttrs
}