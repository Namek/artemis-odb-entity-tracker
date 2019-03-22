package snabbdom.modules

import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import org.w3c.dom.set
import snabbdom.*
import kotlin.js.Json

interface Dataset : Json

private val CAPS_REGEX = Regex.fromLiteral("/[A-Z]/g")


fun updateDataset(oldVnode: VNode, vnode: VNode) {
    val elm: HTMLElement = vnode.elm as HTMLElement
    var oldDataset = oldVnode.data?.dataset
    var dataset = vnode.data?.dataset

    if (oldDataset == null && dataset == null) return
    if (oldDataset === dataset) return
    oldDataset = oldDataset ?: newObj().unsafeCast<Dataset>()
    dataset = dataset ?: newObj().unsafeCast<Dataset>()
    val d = elm.dataset

    for (key in jsObjKeys(oldDataset)) {
        if (dataset[key] == null) {
            if (d != undefined) {
                if (key in jsObjKeys(d)) {
                    delete(d[key])
                }
            } else {
                elm.removeAttribute("data-" + key.replace(CAPS_REGEX, "-$&").toLowerCase())
            }
        }
    }
    for (key in jsObjKeys(dataset)) {
        if (oldDataset[key] !== dataset[key]) {
            val value = dataset[key].toString()
            if (d != undefined) {
                d[key] = value
            } else {
                elm.setAttribute("data-" + key.replace(CAPS_REGEX, "-$&").toLowerCase(), value)
            }
        }
    }
}

class DatasetModule : Module(
    create = ::updateDataset,
    update = ::updateDataset
)