package net.namekdev.entity_tracker.ui

import org.w3c.dom.HTMLSelectElement
import snabbdom.*
import snabbdom.modules.Attrs
import snabbdom.modules.On
import kotlin.js.Json

fun dropdown(valueIndex: Int?, valuesTexts: List<String>, allowNull: Boolean, onChange: (Int?) -> Unit): RNode {
    val modifier = if (allowNull) 1 else 0
    val size = valuesTexts.size + modifier
    val options: Array<VNode> = Array<VNode>(size) { idx ->
        val idxMod = idx - modifier
        val textVal: Pair<String, Int?> =
            if (idxMod < 0)
                Pair("<null>", null)
            else
                Pair(valuesTexts[idxMod], idxMod)

        val attrs: Attrs = j("value" to idx.toString())
        h("option", VNodeData(attrs = attrs), textVal.first)
    }

    val on: On = j("change" to { evt: Json ->
        val idx = (evt["target"] as Json)["selectedIndex"] as Int
        val valueIndex: Int? =
            if (allowNull)
                if (idx == 0) null
                else idx - 1
            else idx

        onChange(valueIndex)
    })
    val selectedIdx = (valueIndex ?: -1) + modifier

    val hook: CreateHook = { ev, v ->
        val el = (v.elm!! as HTMLSelectElement)
        el.selectedIndex = selectedIdx
        null
    }

    return RNode(h("select", VNodeData(on = on, hook = j("create" to hook)), options))
}