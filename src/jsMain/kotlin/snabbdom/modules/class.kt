package snabbdom.modules

import org.w3c.dom.Element
import snabbdom.*
import kotlin.js.Json

//export type Classes = Record<string, boolean>
interface Classes : Json {
    override fun get(propertyName: String): Boolean {
        val value = this._get(propertyName)
        return value != null && value != "false"
    }
}

private val updateClass = fun(oldVnode: VNode, vnode: VNode): Unit {
    val elm: Element = vnode.elm as Element
    var oldClass = oldVnode.data?.`class`
    var klass = vnode.data?.`class`

    if (oldClass == null && klass == null) return
    if (oldClass === klass) return
    oldClass = oldClass ?: newObj().unsafeCast<Classes>()
    klass = klass ?: newObj().unsafeCast<Classes>()

    for (name in jsObjKeys(oldClass)) {
        if (klass[name]) {
            elm.classList.remove(name)
        }
    }
    for (name in jsObjKeys(klass)) {
        val cur = klass[name]
        if (cur != oldClass[name]) {
            if (cur)
                elm.classList.add(name)
            else
                elm.classList.remove(name)
        }
    }
}

class ClassModule : Module() {
    override val create: CreateHook?
        get() = updateClass

    override val update: UpdateHook?
        get() = updateClass
}