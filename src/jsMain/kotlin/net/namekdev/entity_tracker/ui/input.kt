package net.namekdev.entity_tracker.ui

import org.w3c.dom.HTMLSelectElement
import snabbdom.*
import snabbdom.modules.Attrs
import snabbdom.modules.On
import snabbdom.modules.Props
import snabbdom.modules.VNodeStyle
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

    val hooks: Hooks = j()
    hooks.create = { ev, v ->
        val el = (v.elm!! as HTMLSelectElement)
        el.selectedIndex = selectedIdx
        null
    }
    hooks.update = { ev, v ->
        val el = (v.elm!! as HTMLSelectElement)
        el.selectedIndex = el.asDynamic().selectedIndex__
        null
    }

    val props: Props = j("selectedIndex__" to selectedIdx)

    return RNode(h("select", VNodeData(on = on, hook = hooks, props = props), options))
}

fun checkbox(value: Boolean?, allowNull: Boolean, onChange: (Boolean?) -> Unit): RNode {
    val isNull = allowNull && value == null
    val isChecked = value == true

    val attrs: Attrs = j("type" to "checkbox")
    if (isChecked)
        attrs["checked"] = "checked"

    val nullCheckBox =
        if (allowNull)
            row(attrs(paddingRight(3)), nullCheckbox(!isNull) {
                if (!it)
                    onChange(null)
                else
                    onChange(true)
            })
        else dummyEl

    val theValue: RNode

    if (!isNull) {
        val on: On = j()
        on["change"] = { evt: dynamic ->
            val newValue = !!evt.target.checked
            onChange(newValue)
        }

        theValue = row(
            elems(
                text("⅟"),
                RNode(h("input", VNodeData(attrs = attrs, on = on)))
            )
        )
    }
    else {
        theValue = text("<null>")
    }

    return row(attrs(),
        nullCheckBox,
        theValue
    )
}

/* TODO consider refactoring this feature to be more general than just checkboxes:
    All it needs is to be a composing container for checkbox() or input(), it would be public then.
*/
internal fun nullCheckbox(value: Boolean, onChange: (Boolean) -> Unit): RNode {
    val attrs: Attrs = j("type" to "checkbox")
    if (value)
        attrs["checked"] = "checked"

    val on: On = j()
    on["change"] = { evt: dynamic ->
        val newValue = !!evt.target.checked
        onChange(newValue)
    }

    return row(attrs(spacing(2)),
        // TODO nullability icon
        RNode(h("input", VNodeData(attrs = attrs, on = on)))
    )
}

fun input(text: String, inputType: InputType, allowNull: Boolean, onChange: (InputValue?) -> Unit): RNode {
    val nativeInputType = when (inputType) {
        InputType.Text -> "text"
        else -> "number"
    }
    val width = when (inputType) {
        InputType.Text -> 150
        else -> 100
    }

    val attrs: Attrs = j("type" to nativeInputType)
    val props: Props = j("value" to text)
    val style: VNodeStyle = j("width" to "${width}px")
    val on: On = j(
        "change" to { evt: dynamic ->
            val text = evt.target.value?.toString() ?: ""
            val output = when (inputType) {
                InputType.Text ->
                    InputValueText(text)
                InputType.Integer ->
                    InputValueInteger(text.toLong())
                InputType.FloatingPointNumber ->
                    InputValueFloatingPoint(text.toDouble())
            }
            onChange(output)
        },
        "blur" to {}
    )
    // TODO handle ESCAPE to cancel edit, bring back old value to input

    // TODO use <textarea> for Text

    val inputEl = RNode(h("input", VNodeData(attrs = attrs, on = on, props = props, style = style)))

    if (allowNull) {
        row(attrs(),
            inputEl
        )
    }

    return inputEl
}

sealed class InputValue(val type: InputType)
class InputValueText(val text: String): InputValue(InputType.Text)
class InputValueFloatingPoint(val number: Double): InputValue(InputType.FloatingPointNumber)
class InputValueInteger(val number: Long): InputValue(InputType.Integer)


enum class InputType {
    Text,
    Integer,
    FloatingPointNumber
}