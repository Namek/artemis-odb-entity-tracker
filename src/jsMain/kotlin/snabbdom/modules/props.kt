package snabbdom.modules

import snabbdom.*
import kotlin.js.Json

//export type Props = Record<string, any>
interface Props : Json
//operator fun Props.get(key: String): dynamic = this._get(key)
//operator fun Props.set(key: String, value: dynamic): Unit { this._set(key, value) }

external fun delete(p: dynamic): Boolean = definedExternally

private fun updateProps(oldVnode: VNode, vnode: VNode) {
    var cur: dynamic
    var old: dynamic
    val elm = vnode.elm
    var oldProps = (oldVnode.data?.unsafeCast<VNodeData?>())?.props
    var props = (vnode.data?.unsafeCast<VNodeData>())?.props

    if (oldProps == null && props == null) return;
    if (oldProps === props) return;
    oldProps = oldProps ?: newObj().unsafeCast<Props>()
    props = props ?: newObj().unsafeCast<Props>()

    for (key in jsObjKeys(oldProps.asDynamic())) {
        if (props[key] == null) {
            delete(elm.asDynamic()[key])
        }
    }
    for (key in jsObjKeys(props.asDynamic())) {
        cur = props[key]
        old = oldProps[key]
        if (old !== cur && (key !== "value" || (elm.asDynamic())[key] !== cur)) {
            (elm.asDynamic())[key] = cur
        }
    }
}

class PropsModule : Module(
    create = ::updateProps,
    update = ::updateProps
)