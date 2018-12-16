package snabbdom.helpers

import kotlin.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import snabbdom.*
import kotlin.js.Json


interface AttachData : Json {
    var placeholder: dynamic
    var real: Node?
    var target: Node

    override operator fun get(propertyName: String): dynamic = this._get(propertyName)
    override operator fun set(propertyName: String, value: dynamic) { this._set(propertyName, value) }
    operator fun get(key: Number): dynamic = this._get(key)
    operator fun set(key: Number, value: dynamic) { this._set(key, value) }
}

fun pre(vnode: VNode, newVnode: VNode) {
    val attachData = vnode.data!!.attachData!!
    val newVnodeAttachData = newVnode.data!!.attachData!!

    // Copy created placeholder and real element from old vnode
    newVnodeAttachData.placeholder = attachData.placeholder
    newVnodeAttachData.real = attachData.real
    // Mount real element in vnode so the patch process operates on it
    vnode.elm = attachData.real
}

fun post(ignoredArg: Any?, vnode: VNode) {
    // Mount dummy placeholder in vnode so potential reorders use it
    vnode.elm = vnode.data!!.attachData!!.placeholder
}

fun destroy(vnode: VNode) {
    // Remove placeholder
    if (vnode.elm != null) {
        (vnode.elm!!.parentNode as HTMLElement).removeChild(vnode.elm!!)
    }
    // Remove real element from where it was inserted
    vnode.elm = vnode.data!!.attachData!!.real
}

fun create(ignoredArg: Any?, vnode: VNode) {
    val real = vnode.elm!!
    var attachData = vnode.data!!.attachData!!
    val placeholder = document.createElement("span")
    // Replace actual element with dummy placeholder
    // Snabbdom will then insert placeholder instead
    vnode.elm = placeholder
    attachData.target.appendChild(real)
    attachData.real = real
    attachData.placeholder = placeholder
}

fun attachTo(target: Element, vnode: VNode): VNode {
    (vnode.data ?: newObj() as VNodeData).let { data ->
        vnode.data = data
        (data.hook ?: newObj() as Hooks).let { hook ->
            data.hook = hook
            val attachData = newObj() as AttachData
            attachData.target = target
            data.attachData = attachData
            hook.create = ::create
            hook.prepatch = ::pre
            hook.postpatch = ::post
            hook.destroy = ::destroy
        }
    }
    return vnode
}