package snabbdom.modules

import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.css.CSSStyleDeclaration
import snabbdom.*
import kotlin.browser.document
import kotlin.browser.window
import kotlin.js.Json

//export type VNodeStyle = Record<string, string> & {
//    delayed?: Record<string, string>
//    remove?: Record<string, string>
//}
interface VNodeStyle : Json {
    var delayed: Delayed?
    var remove: Remove?
    var destroy: Destroy?

    interface Delayed : Json
    interface Remove : Json
    interface Destroy : Json
}


class StyleModule : Module() {
    private var reflowForced = false

    override val pre: PreHook?
        get() = ::forceReflow

    override val create: CreateHook?
        get() = ::updateStyle

    override val update: UpdateHook?
        get() = ::updateStyle

    override val destroy: DestroyHook?
        get() = ::applyDestroyStyle

    override val remove: RemoveHook?
        get() = ::applyRemoveStyle


    private fun updateStyle(oldVnode: VNode, vnode: VNode) {
        val elm = vnode.elm!! as HTMLElement
        var oldStyle = oldVnode.data?.style
        var style = vnode.data?.style

        if (oldStyle == null && style == null) return
        if (oldStyle === style) return
        oldStyle = (oldStyle ?: newObj()).unsafeCast<VNodeStyle>()
        style = (style ?: newObj()).unsafeCast<VNodeStyle>()
        val oldHasDel = oldStyle.delayed != null

        for (name in jsObjKeys(oldStyle)) {
            if (style[name] == null) {
                if (name[0] == '-' && name[1] == '-') {
                    elm.asDynamic().style.removeProperty(name)
                } else {
                    elm.asDynamic().style[name] = ""
                }
            }
        }
        for (name in jsObjKeys(style)) {
            var cur = style[name]!!
            if (name == "delayed" && style.delayed != null) {
                for (name2 in jsObjKeys(style.delayed)) {
                    cur = (style.delayed!!)[name2]!!
                    if (!oldHasDel || cur !== (oldStyle.delayed!!)[name2]) {
                        setNextFrame(elm.asDynamic().style, name2, cur)
                    }
                }
            } else if (name !== "remove" && cur !== oldStyle[name]) {
                if (name[0] == '-' && name[1] == '-') {
                    elm.style.setProperty(name, cur.toString())
                } else {
                    elm.style.asDynamic()[name] = cur
                }
            }
        }
    }

    private fun applyDestroyStyle(vnode: VNode): Unit {
        val elm = vnode.elm as HTMLElement
        val s = vnode.data?.style

        if (s != null) {
            val style = s.destroy

            if (style != null) {
                for (name in jsObjKeys(style)) {
                    elm.style.setProperty(name, style[name] as String)
                }
            }
        }
    }

    private fun applyRemoveStyle(vnode: VNode, rm: () -> Unit): Unit {
        val s = vnode.data?.style
        if (s?.remove == null) {
            rm()
            return
        }
        if(!reflowForced) {
            document.body?.let { body ->
                window.getComputedStyle(body).transform
                reflowForced = true
            }
        }
        val elm = vnode.elm as HTMLElement
        val style = s.remove!!
        var amount = 0
        val applied = mutableListOf<String>()
        for (name in jsObjKeys(style)) {
            applied.add(name)
            elm.style.setProperty(name, style[name] as String)
        }
        val compStyle = window.getComputedStyle(elm as Element)
        val props = compStyle.transitionProperty.split(", ")
        for (i in 0 until props.size) {
            if(applied.indexOf(props[i]) != -1) amount++
        }
        (elm as Element).addEventListener("transitionend", fun (ev: dynamic/*TransitionEvent*/) {
            if (ev.target === elm) --amount
            if (amount == 0) rm()
        })
    }

    private fun forceReflow() {
        reflowForced = false
    }
}