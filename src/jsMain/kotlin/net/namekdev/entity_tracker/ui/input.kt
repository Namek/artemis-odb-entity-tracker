package net.namekdev.entity_tracker.ui

import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import snabbdom.*
import snabbdom.modules.Attrs
import snabbdom.modules.On
import snabbdom.modules.Props
import snabbdom.modules.VNodeStyle
import kotlin.js.Json

fun button(label: String, clickHandler: () -> Unit): RNode =
    el("button", attrs(on(click = {
        clickHandler()
    })), text(label))

fun button(attrs: Array<Attribute>, label: String, clickHandler: () -> Unit): RNode =
    el("button", attrs + attrs(on(click = {
        clickHandler()
    })), text(label))

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

    return Unstyled { h("select", VNodeData(on = on, hook = hooks, props = props), options) }
}

inline fun checkbox(value: Boolean, crossinline onChange: (Boolean) -> Unit): RNode =
    nullableCheckbox(value, false) { onChange(it!!) }

inline fun labelledCheckbox(label: String, value: Boolean, crossinline onChange: (Boolean) -> Unit): RNode =
    elAsRow("label", attrs(spacing(3)),
        elems(
            nullableCheckbox(value, false) { onChange(it!!) },
            row(attrs(centerY), text(label))
        )
    )

fun nullableCheckbox(value: Boolean?, allowNull: Boolean, onChange: (Boolean?) -> Unit): RNode {
    val isNull = allowNull && value == null
    val isChecked = value == true

    val attrs: Attrs = j("type" to "checkbox")
    val props: Props = j("checked" to isChecked)

    val nullCheckBox =
        if (allowNull)
            row(attrs(paddingRight(3)),
                nullCheckbox(isNull, onChange = { isNull ->
                    onChange(if (isNull) null else true)
                })
            )
        else null

    val theValue: RNode

    if (!isNull) {
        val on: On = j()
        on["change"] = { evt: dynamic ->
            val isChecked = !!evt.target.checked
            onChange(isChecked)
        }

        theValue = row(
            Unstyled { h("input", VNodeData(attrs = attrs, props = props, on = on)) }
        )
    }
    else {
        theValue = text("<null>")
    }

    return if (nullCheckBox != null)
        row(attrs(),
            nullCheckBox,
            theValue
        )
    else theValue
}

fun nullCheckbox(isNull: Boolean, onChange: (Boolean) -> Unit, view: ((Boolean) -> RNode)? = null): RNode {
    val attrs: Attrs = j("type" to "checkbox")
    val props: Props = j("checked" to !isNull)

    val on: On = j()
    on["change"] = { evt: dynamic ->
        val newValue = !!evt.target.checked
        onChange(!newValue)
    }

    return row(attrs(spacing(3)),
        Unstyled { h("input", VNodeData(attrs = attrs, props = props, on = on)) },
        view?.invoke(isNull) ?: none
    )
}

fun textEdit(
    text: String, inputType: InputType, autoFocus: Boolean,
    onChange: ((InputValue?, String) -> Unit)? = null,
    onEnter: ((InputValue?) -> Unit)? = null,
    onEscape: (() -> Unit)? = null,
    width: Int? = null
) : RNode
{
    val nativeInputType = when (inputType) {
        InputType.Text -> "text"
        else -> "number"
    }
    val width = width ?: when (inputType) {
        InputType.Text -> 150
        else -> 100
    }

    val attrs: Attrs = j("type" to nativeInputType)
    val props: Props = j("value" to text)
    val style: VNodeStyle = j("width" to "${width}px")
    val on: On = j(
        "change" to { evt: dynamic ->
            val text = evt.target.value?.toString() ?: ""
            val output = inputTypeToInputValue(inputType, text)
            onChange?.invoke(output, text)
        },
        "keydown" to { evt: dynamic ->
            if (evt.keyCode == 13 /*ENTER*/) {
                val text = evt.target.value?.toString() ?: ""
                val output = inputTypeToInputValue(inputType, text)
                evt.preventDefault()
                onEnter?.invoke(output)
            }
            else if (evt.keyCode == 27 /*ESCAPE*/) {
                evt.target.value = text
                evt.preventDefault()
                onEscape?.invoke()
            }
            else Unit
        }
    )

    val hooks: Hooks = j()

    if (autoFocus) {
        hooks.insert = { v ->
            val el = v.elm as HTMLInputElement
            el.focus()

            null
        }
    }

    // TODO use <textarea> for Text

    val vnodeData = VNodeData(attrs = attrs, on = on, props = props, style = style, hook = hooks)
    val inputEl = Unstyled { h("input", vnodeData) }

    return row(attrs(), inputEl)
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

private fun inputTypeToInputValue(inputType: InputType, text: String): InputValue =
    when (inputType) {
        InputType.Text ->
            InputValueText(text)
        InputType.Integer ->
            InputValueInteger(text.toLong())
        InputType.FloatingPointNumber ->
            InputValueFloatingPoint(text.toDouble())
    }

data class EditedInputState(val path: List<Int>, var text: String?)
