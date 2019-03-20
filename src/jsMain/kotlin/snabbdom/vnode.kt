package snabbdom

import org.w3c.dom.Element
import org.w3c.dom.Node
import snabbdom.helpers.AttachData
import snabbdom.modules.*
import kotlin.js.Json

interface VNode : Json {
    var sel: String?
    var data: VNodeData?
    var children: Array<dynamic /* VNode | String */>?
    var text: String?
    var elm: Node? // Element | Comment
    var key: dynamic /* String | Number */
}

open class VNodeData(
    var props: Props? = null,
    var attrs: Attrs? = null,
    var `class`: Classes? = null,
    var style: VNodeStyle? = null,
    var dataset: Dataset? = null,
    var on: On? = null,
    var hero: Hero? = null,
    var attachData: AttachData? = null,
    var hook: Hooks? = null,
    var key: dynamic = null,
    var ns: String? = null,
    var fn: (() -> VNode)? = null,
    var args: Array<dynamic>? = null
) : Json {
    override fun get(propertyName: String): Any? = this._get(propertyName)
    override fun set(propertyName: String, value: Any?) = this._set(propertyName, value)
}


fun vnode(sel: String?, data: VNodeData?, children: Array<VNode>?, text: String?, elm: Element?): VNode {
    val key = data?.key ?: null
    val ret = newObj().unsafeCast<VNode>()
    ret.sel = sel
    ret.data = data
    ret.children = children
    ret.text = text
    ret.elm = elm
    ret.key = key

    return ret
}
