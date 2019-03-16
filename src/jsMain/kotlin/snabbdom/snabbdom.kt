package snabbdom

import org.w3c.dom.*
import snabbdom.modules.Module
import snabbdom.modules.ModuleHooks

typealias Patch = (oldVnode: dynamic /* Element | VNode */, vnode: VNode) -> VNode

object Snabbdom {
    fun init(modules: Array<Module>, domapi: DOMAPI? = null): Patch =
        _init(modules, domapi)
}

//fun isUndef(s: dynamic): Boolean {
//    return s === undefined
//}
//
//fun isDef(s: dynamic): Boolean {
//    return s !== undefined
//}

typealias VNodeQueue = MutableList<VNode>

val emptyNode = vnode("", newObj().unsafeCast<VNodeData>(), arrayOf(), null, null)

fun sameVnode(vnode1: VNode, vnode2: VNode): Boolean {
    return vnode1.key === vnode2.key && vnode1.sel === vnode2.sel
}

fun isVnode(vnode: dynamic): Boolean {
    return vnode.sel != null
}

typealias KeyToIndexMap = Map<String, Int>

fun createKeyToOldIdx(children: Array<VNode?>, beginIdx: Int, endIdx: Int): KeyToIndexMap {
    val map = mutableMapOf<String, Int>()

    for (i in beginIdx..endIdx) {
        val ch = children[i]
        if (ch != null && ch != undefined) {
            val key = ch.key
            if (key != null) map[key] = i
        }
    }
    return map
}

fun _init(modules: Array<Module>, domApi: DOMAPI?): Patch {
    val api: DOMAPI = domApi ?: htmlDomApi

    val cbs = ModuleHooks(
        modules.map { m -> m.pre }.filterNotNull().toTypedArray(),
        modules.map { m -> m.create }.filterNotNull().toTypedArray(),
        modules.map { m -> m.update }.filterNotNull().toTypedArray(),
        modules.map { m -> m.destroy }.filterNotNull().toTypedArray(),
        modules.map { m -> m.remove }.filterNotNull().toTypedArray(),
        modules.map { m -> m.post }.filterNotNull().toTypedArray()
    )

    fun emptyNodeAt(elm: Element): VNode {
        val id = if (elm.id != null && elm.id != "") '#' + elm.id else ""
        val c =
            if (elm.className != null && elm.className != "")
                '.' + elm.className.split(' ').joinToString(".")
            else ""

        return vnode(
            api.tagName(elm).toLowerCase() + id + c,
            newObj().unsafeCast<VNodeData>(),
            arrayOf(),
            undefined,
            elm
        )
    }

    fun createRmCb(childElm: Node, listeners: Int): () -> Unit {
        var leftListeners = listeners
        return fun() {
            if (--leftListeners == 0) {
                val parent = api.parentNode(childElm)!!
                api.removeChild(parent, childElm)
            }
        }
    }

    fun createElm(vnode: VNode, insertedVnodeQueue: VNodeQueue): Node {
        var i: dynamic
        var data = vnode.data
        if (data != null) {
            i = data.hook
            if (i != null) {
                i = i.init
                if (i != null) {
                    i(vnode)
                    data = vnode.data
                }
            }
        }
        val children = vnode.children
        val sel = vnode.sel
        if (sel == "!") {
            if (vnode.text == null) {
                vnode.text = ""
            }
            vnode.elm = api.createComment(vnode.text!!)
        } else if (sel != null) {
            // Parse selector
            val hashIdx = sel.indexOf('#')
            val dotIdx = sel.indexOf('.', hashIdx)
            val hash = if (hashIdx > 0) hashIdx else sel.length
            val dot = if (dotIdx > 0) dotIdx else sel.length
            val tag = if (hashIdx != -1 || dotIdx != -1) sel.substring(0, minOf(hash, dot)) else sel
            val elm =
                {
                    var el: Element? = null
                    if (data != null) {
                        i = (data.unsafeCast<VNodeData>()).ns
                        if (i != null)
                            el = api.createElementNS(i, tag)
                    }
                    if (el == null) api.createElement(tag) else el
                }()
            vnode.elm = elm
            if (hash < dot) elm.setAttribute("id", sel.substring((hash + 1), dot))
            if (dotIdx > 0) elm.setAttribute("class", sel.substring(dot + 1).replace('.', ' '))
            for (i in 0 until cbs.create.size) cbs.create[i](emptyNode, vnode)
            if (children != null && isArray(children)) {
                for (i in 0 until children.size) {
                    val ch = children[i]
                    if (ch != null) {
                        api.appendChild(elm, createElm(ch.unsafeCast<VNode>(), insertedVnodeQueue))
                    }
                }
            } else if (isPrimitive(vnode.text)) {
                api.appendChild(elm, api.createTextNode(vnode.text!!))
            }
            i = (vnode.data.unsafeCast<VNodeData?>())?.hook // Reuse variable
            if (i != null) {
                if (i.create) i.create(emptyNode, vnode)
                if (i.insert) insertedVnodeQueue.add(vnode)
            }
        } else {
            vnode.elm = api.createTextNode(vnode.text!!)
        }
        return vnode.elm!!
    }

    fun addVnodes(
        parentElm: Node,
        before: Node?,
        vnodes: Array<VNode?>,
        startIdx: Int,
        endIdx: Int,
        insertedVnodeQueue: VNodeQueue
    ) {
        for (idx in startIdx..endIdx) {
            val ch = vnodes[idx]
            if (ch != null) {
                api.insertBefore(parentElm, createElm(ch, insertedVnodeQueue), before)
            }
        }
    }

    fun invokeDestroyHook(vnode: VNode) {
        var i: dynamic
        val data = vnode.data
        if (data != null) {
            data.hook?.destroy?.invoke(vnode)
            for (destroyHook in cbs.destroy) destroyHook(vnode)

            if (vnode.children != null) {
                for (j in 0 until vnode.children!!.size) {
                    i = vnode.children!![j]
                    if (i != null && i !is String) {
                        invokeDestroyHook(i)
                    }
                }
            }
        }
    }

    fun removeVnodes(
        parentElm: Node,
        vnodes: Array<VNode?>,
        startIdx: Int,
        endIdx: Int
    ) {
        for (idx in startIdx..endIdx) {
            var listeners: Int
            var rm: () -> Unit
            val ch = vnodes[idx]
            if (ch != null) {
                if (ch.sel != null) {
                    invokeDestroyHook(ch)
                    listeners = cbs.remove.size + 1
                    rm = createRmCb(ch.elm as Node, listeners)
                    for (removeHook in cbs.remove)
                        removeHook(ch, rm)

                    ch.data?.hook?.remove.let { fn ->
                        if (fn != null) fn.invoke(ch, rm)
                        else rm.invoke()
                    }
                } else { // Text node
                    api.removeChild(parentElm, ch.elm as Node)
                }
            }
        }
    }

    var patchVnode: ((oldVnode: VNode, vnode: VNode, insertedVnodeQueue: VNodeQueue) -> Unit)? = null

    fun updateChildren(
        parentElm: Node,
        oldCh: Array<VNode?>,
        newCh: Array<VNode?>,
        insertedVnodeQueue: VNodeQueue
    ) {
        var oldStartIdx = 0
        var newStartIdx = 0
        var oldEndIdx = oldCh.size - 1
        var oldStartVnode = oldCh[0]
        var oldEndVnode = oldCh[oldEndIdx]
        var newEndIdx = newCh.size - 1
        var newStartVnode = newCh[0]
        var newEndVnode = newCh[newEndIdx]
        var oldKeyToIdx: KeyToIndexMap? = null
        var idxInOld: Int? = null
        var elmToMove: VNode

        while (oldStartIdx <= oldEndIdx && newStartIdx <= newEndIdx) {
            if (oldStartVnode == null) {
                oldStartVnode = oldCh[++oldStartIdx] // Vnode might have been moved left
            } else if (oldEndVnode == null) {
                oldEndVnode = oldCh[--oldEndIdx]
            } else if (newStartVnode == null) {
                newStartVnode = newCh[++newStartIdx]
            } else if (newEndVnode == null) {
                newEndVnode = newCh[--newEndIdx]
            } else if (sameVnode(oldStartVnode, newStartVnode)) {
                patchVnode!!(oldStartVnode, newStartVnode, insertedVnodeQueue)
                oldStartVnode = oldCh[++oldStartIdx]
                newStartVnode = newCh[++newStartIdx]
            } else if (sameVnode(oldEndVnode, newEndVnode)) {
                patchVnode!!(oldEndVnode, newEndVnode, insertedVnodeQueue)
                oldEndVnode = oldCh[--oldEndIdx]
                newEndVnode = newCh[--newEndIdx]
            } else if (sameVnode(oldStartVnode, newEndVnode)) { // Vnode moved right
                patchVnode!!(oldStartVnode, newEndVnode, insertedVnodeQueue)
                api.insertBefore(parentElm, oldStartVnode.elm as Node, api.nextSibling(oldEndVnode.elm as Node))
                oldStartVnode = oldCh[++oldStartIdx]
                newEndVnode = newCh[--newEndIdx]
            } else if (sameVnode(oldEndVnode, newStartVnode)) { // Vnode moved left
                patchVnode!!(oldEndVnode, newStartVnode, insertedVnodeQueue)
                api.insertBefore(parentElm, oldEndVnode.elm as Node, oldStartVnode.elm as Node)
                oldEndVnode = oldCh[--oldEndIdx]
                newStartVnode = newCh[++newStartIdx]
            } else {
                if (oldKeyToIdx == null) {
                    oldKeyToIdx = createKeyToOldIdx(oldCh, oldStartIdx, oldEndIdx)
                }
                idxInOld = oldKeyToIdx[newStartVnode.key]
                if (idxInOld == null) { // New element
                    api.insertBefore(parentElm, createElm(newStartVnode, insertedVnodeQueue), oldStartVnode.elm as Node)
                    newStartVnode = newCh[++newStartIdx]
                } else {
                    elmToMove = oldCh[idxInOld]!!
                    if (elmToMove.sel !== newStartVnode.sel) {
                        api.insertBefore(
                            parentElm,
                            createElm(newStartVnode, insertedVnodeQueue),
                            oldStartVnode.elm as Node
                        )
                    } else {
                        patchVnode!!(elmToMove, newStartVnode, insertedVnodeQueue)
                        oldCh[idxInOld] = null
                        api.insertBefore(parentElm, (elmToMove.elm as Node), oldStartVnode.elm as Node)
                    }
                    newStartVnode = newCh[++newStartIdx]
                }
            }
        }
        if (oldStartIdx <= oldEndIdx || newStartIdx <= newEndIdx) {
            if (oldStartIdx > oldEndIdx) {
                val before = newCh[newEndIdx + 1]?.elm
                addVnodes(parentElm, before, newCh, newStartIdx, newEndIdx, insertedVnodeQueue)
            } else {
                removeVnodes(parentElm, oldCh, oldStartIdx, oldEndIdx)
            }
        }
    }

    patchVnode = fun(oldVnode: VNode, vnode: VNode, insertedVnodeQueue: VNodeQueue) {
        vnode.data?.hook?.prepatch?.invoke(oldVnode, vnode)

        val elm = oldVnode.elm!!
        vnode.elm = elm
        val oldCh = oldVnode.children
        val ch = vnode.children
        if (oldVnode === vnode) return

        if (vnode.data != null) {
            for (updateHook in cbs.update)
                updateHook(oldVnode, vnode)

            vnode.data?.hook?.update?.invoke(oldVnode, vnode)
        }
        if (vnode.text == null) {
            if (oldCh != null && ch != null) {
                if (oldCh !== ch) updateChildren(elm, oldCh as Array<VNode?>, ch as Array<VNode?>, insertedVnodeQueue)
            } else if (ch != null) {
                if (oldVnode.text != null) api.setTextContent(elm, "")
                addVnodes(elm, null, ch as Array<VNode?>, 0, (ch as Array<VNode?>).size - 1, insertedVnodeQueue)
            } else if (oldCh != null) {
                removeVnodes(elm, oldCh as Array<VNode?>, 0, (oldCh as Array<VNode?>).size - 1)
            } else if (oldVnode.text != null) {
                api.setTextContent(elm, "")
            }
        } else if (oldVnode.text !== vnode.text) {
            if (oldCh != null) {
                removeVnodes(elm, oldCh as Array<VNode?>, 0, (oldCh as Array<VNode?>).size - 1)
            }
            api.setTextContent(elm, vnode.text)
        }
        vnode.data?.hook?.postpatch?.invoke(oldVnode, vnode)
    }

    // patch function
    return fun(oldVnodeOrElement: dynamic /*VNode | Element*/, vnode: VNode): VNode {
        val insertedVnodeQueue: VNodeQueue = mutableListOf()
        for (preHook in cbs.pre) preHook()

        val oldVnode: VNode =
            if (!isVnode(oldVnodeOrElement))
                emptyNodeAt(oldVnodeOrElement as Element)
            else
                oldVnodeOrElement.unsafeCast<VNode>()

        if (sameVnode(oldVnode, vnode)) {
            patchVnode(oldVnode, vnode, insertedVnodeQueue)
        } else {
            val elm = oldVnode.elm as Node
            val parent = api.parentNode(elm)

            createElm(vnode, insertedVnodeQueue)

            if (parent !== null) {
                api.insertBefore(parent, vnode.elm as Node, api.nextSibling(elm))
                removeVnodes(parent, arrayOf(oldVnode), 0, 0)
            }
        }

        for (insertedVnode in insertedVnodeQueue) {
            insertedVnode.data?.hook?.insert?.invoke(insertedVnode)
        }
        for (postHook in cbs.post) postHook()
        return vnode
    }
}