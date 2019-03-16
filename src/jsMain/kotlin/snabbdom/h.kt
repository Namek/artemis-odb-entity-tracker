package snabbdom

import kotlin.js.json

private fun addNS(data: dynamic, children: Array<VNode>?, sel: String?) {
    data.ns = "http://www.w3.org/2000/svg"
    if (sel != "foreignObject" && children !== null) {
        for (i in 0 until children.size) {
            val childData = children[i].data
            if (childData != null) {
                addNS(childData, (children[i].unsafeCast<VNode>()).children as Array<VNode>, children[i].sel)
            }
        }
    }
}


fun h(sel: String): VNode =
    h_(sel, null, null, null)

fun h(sel: String, data: VNodeData): VNode =
    h_(sel, data, null, null)

fun h(sel: String, text: String): VNode =
    h_(sel, null, text, null)

fun h(sel: String, children: Array<VNode>): VNode =
    h_(sel, null, null, children)

fun h(sel: String, children: Array<String>): VNode =
    h_(sel, null, null, children)

fun h(sel: String, vararg nodes: VNode): VNode =
    h_(sel, null, null, arrayOf(*nodes))

fun h(sel: String, vararg texts: String): VNode =
    h_(sel, null, null, texts)

fun h(sel: String, vararg nodesOrPrimitives: dynamic): VNode =
    h_(sel, null, null, arrayOf(*nodesOrPrimitives))

fun h(sel: String, data: VNodeData, text: String): VNode =
    h_(sel, data, text, null)

fun h(sel: String, data: VNodeData, children: Array<VNode>): VNode =
    h_(sel, data, null, children)

fun h(sel: String, data: VNodeData, children: Array<String>): VNode =
    h_(sel, data, null, children)

fun h(sel: String, data: VNodeData, vararg children: VNode): VNode =
    h_(sel, data, null, children)

fun h(sel: String, data: VNodeData, vararg children: String): VNode =
    h_(sel, data, null, children)


// This does not come from original library, it's for users of this library.
// Helps with easier key-value passing into VNodeData properties.
fun <T> j(vararg pairs: Pair<String, Any?>): T =
    json(*pairs).unsafeCast<T>()


// use those BELOW if you want to pass array mixed of both VNodes and Strings
fun h_(sel: String, children: Array<dynamic>): VNode =
    h_(sel, null, null, children)

fun h_(sel: String, data: VNodeData, children: Array<dynamic>): VNode =
    h_(sel, data, null, children)

fun h_(sel: String, data: VNodeData?, text: String?, children: Array<dynamic>?): VNode {
    val data = if (data != null) data else VNodeData()
    if (children != null) {
        for (i in 0 until children.size) {
            // yes, we modify the input collection!
            if (isPrimitive(children[i]))
                children[i] = vnode(null, null, null, children[i], null)
        }
    }
    if (
        sel[0] == 's' && sel[1] == 'v' && sel[2] == 'g' &&
        (sel.length == 3 || sel[3] == '.' || sel[3] == '#')
    ) {
        addNS(data, children, sel)
    }
    return vnode(sel, data, children, text, null)
}