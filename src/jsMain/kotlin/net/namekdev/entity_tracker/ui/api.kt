package net.namekdev.entity_tracker.ui

import snabbdom.VNode
import snabbdom.h

inline fun table(attrs: Array<Attribute>, header: VNode, vararg rows: VNode) =
    el("table", attrs = attrs, nodes = arrayOf(header, *rows))

inline fun tRow(vararg columns: VNode) =
    h("tr", nodes = *columns)

inline fun tCell(vararg cellContents: VNode) =
    h("td", nodes = *cellContents)

inline fun tCell(text: String) =
    h("td", text)

inline fun thCell(vararg cellContents: VNode) =
    h("td", nodes = *cellContents)

inline fun thCell(text: String) =
    h("td", text)

inline fun span(txt: String): VNode =
    h("span", txt)

inline fun attrs(vararg attrs: Attribute): Array<Attribute> =
    arrayOf(*attrs)

inline fun attrWhen(predicate: Boolean, attr: Attribute): Attribute =
    if (predicate)
        attr
    else Attribute.NoAttribute


val fill = Length.Fill(1)
val shrink = Length.Content
fun min(length: Length) = Length.Min(length)
fun max(length: Length) = Length.Max(length)

val widthFill = Attribute.Width.Fill(1)
val widthShrink = Attribute.Width.Content
val heightFill = Attribute.Height.Fill(1)
val heightShrink = Attribute.Height.Content

// note that the internal flag is 0 so it can be easily overwritten by element()
fun style(prop: String, value: String) = Attribute.StyleClass(0, AStyle(prop, arrayOf(prop to value)))

//fun backgroundColor(color: String) =


fun padding(t: Int, r: Int, b: Int, l: Int) =
        PaddingStyle()