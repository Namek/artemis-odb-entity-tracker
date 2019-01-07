package net.namekdev.entity_tracker.ui

import snabbdom.h


inline fun table(attrs: Array<Attribute>, header: RNode, vararg rows: RNode) =
    el("table", attrs = attrs, nodes = arrayOf(header, *rows))

inline fun tRow(vararg columns: RNode) =
    el("tr", nodes = *columns)

inline fun tCell(vararg cellContents: RNode) =
    el("td", nodes = *cellContents)

inline fun tCell(text: String) =
    RNode(h("td", text))

inline fun thCell(vararg cellContents: RNode) =
    el("td", nodes = *cellContents)

inline fun thCell(text: String) =
    RNode(h("td", text))

inline fun span(txt: String): RNode =
    RNode(h("span", txt))

inline fun attrs(vararg attrs: Attribute): Array<Attribute> =
    arrayOf(*attrs)

inline fun attrWhen(predicate: Boolean, attr: Attribute): Attribute =
    if (predicate)
        attr
    else Attribute.NoAttribute

fun px(size: Int) = Length.Px(size)
val fill = Length.Fill(1)
fun fillPortion(portion: Int) = Length.Fill(portion)
val shrink = Length.Content
fun min(length: Length) = Length.Min(length)
fun max(length: Length) = Length.Max(length)

val widthFill = Attribute.Width.Fill(1)
val widthShrink = Attribute.Width.Content
val heightFill = Attribute.Height.Fill(1)
val heightShrink = Attribute.Height.Content

/**
 * note that when the internal flag is 0 so it can be easily overwritten by element()
 */
fun style(prop: String, value: String, customFlag: Int = 0) =
    Attribute.StyleClass(customFlag, AStyle(prop, arrayOf(prop to value)))

fun padding(around: Int): Attribute.StyleClass =
    Attribute.StyleClass(Flag.padding, PaddingStyle("p-$around", around, around, around, around))

fun padding(t: Int, r: Int, b: Int, l: Int): Attribute.StyleClass =
    Attribute.StyleClass(Flag.padding, PaddingStyle("p-$t-$r-$b-$l", t, r, b, l))

fun paddingXY(x: Int, y: Int) =
    Attribute.StyleClass(Flag.padding, PaddingStyle("p-$x-$y", y, x, y, x))

val centerX = Attribute.AlignX(HAlign.CenterX)
val centerY = Attribute.AlignY(VAlign.CenterY)

//fun backgroundColor(color: String) =
