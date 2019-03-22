package snabbdom.modules

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.events.Event
import snabbdom.*
import kotlin.js.Json

//export type On = {
//    [N in keyof HTMLElementEventMap]?: (ev: HTMLElementEventMap[N]) => void
//} & {
//    [event: string]: EventListener
//}
interface On : Json

private fun invokeHandler(handler: dynamic, vnode: VNode?, event: Event?) {
    if (jsTypeOf(handler) === "function") {
        // call fun handler
        handler.call(vnode, event, vnode)
    } else if (jsTypeOf(handler) === "object") {
        // call handler with arguments
        if (jsTypeOf(handler[0]) === "function") {
            // special case for single argument for performance
            if (handler.length === 2) {
                handler[0].call(vnode, handler[1], event, vnode)
            } else {
                val args = handler.slice(1)
                args.push(event)
                args.push(vnode)
                handler[0].apply(vnode, args)
            }
        } else {
            // call multiple handlers
            for (i in 0 until handler.length) {
                invokeHandler(handler[i], null, null)
            }
        }
    }
}

private fun handleEvent(event: Event, vnode: VNode) {
    val name = event.type
    val on = vnode.data?.on

    // call event handler(s) if exists
    on?.get(name)?.let {
        invokeHandler(it, vnode, event)
    }
}

private fun createListener(): ((Event) -> Unit)? {
    var handler: ((Event) -> Unit)? = null

    handler =
        { event: Event ->
            handleEvent(event, handler.asDynamic().vnode.unsafeCast<VNode>())
        }

    return handler
}

private fun updateEventListeners(oldVnode: VNode, vnode: VNode?) {
    val oldOn = oldVnode.data?.on
    val oldListener = (oldVnode.asDynamic()).listener as ((Event) -> Unit)?
    val oldElm = oldVnode.elm as? Element
    val on = vnode?.data?.on
    val elm: Node? = vnode?.elm


    // optimization for reused immutable handlers
    if (oldOn === on) {
        return
    }

    // remove existing listeners which no longer used
    if (oldOn != null && oldListener != null && oldElm != null) {
        // if element changed or deleted we remove all existing listeners unconditionally
        if (on == null) {
            for (name in jsObjKeys(oldOn)) {
                // remove listener if element was changed or existing listeners removed
                oldElm.removeEventListener(name, oldListener, false)
            }
        } else {
            for (name in jsObjKeys(oldOn)) {
                // remove listener if existing listener removed
                if (on[name] == null) {
                    oldElm.removeEventListener(name, oldListener, false)
                }
            }
        }
    }

    // add new listeners which has not already attached
    if (on != null && elm != null) {
        // reuse existing listener or create new
        val listener = (oldVnode.asDynamic()).listener as ((Event) -> Unit)? ?: createListener()
        (vnode.asDynamic()).listener = listener

        // update vnode for listener
        listener.asDynamic().vnode = vnode

        // if element changed or added we add all needed listeners unconditionally
        if (oldOn == null) {
            for (name in jsObjKeys(on)) {
                // add listener if element was changed or new listeners added
                elm.addEventListener(name, listener, false)
            }
        } else {
            for (name in jsObjKeys(on)) {
                // add listener if new listener added
                if (oldOn[name] == null) {
                    elm.addEventListener(name, listener, false)
                }
            }
        }
    }
}

class EventListenersModule : Module(
    create = ::updateEventListeners,
    update = ::updateEventListeners,
    destroy = { vnode -> updateEventListeners(vnode, null) }
)