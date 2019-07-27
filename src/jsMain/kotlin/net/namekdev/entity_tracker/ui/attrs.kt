package net.namekdev.entity_tracker.ui

import org.w3c.dom.Element
import snabbdom.j
import kotlin.math.max
import kotlin.math.min


inline fun attrs(vararg attrs: Attribute): Array<Attribute> =
    arrayOf(*attrs)

inline fun elems(vararg elements: RNode): Array<RNode> =
    arrayOf(*elements)

inline fun attrWhen(predicate: Boolean, attr: Attribute): Attribute =
    if (predicate)
        attr
    else Attribute.NoAttribute

/**
 * note that when the internal flag is 0 so it can be easily overwritten by element()
 */
fun style(prop: String, value: String, customFlag: Int = 0) =
    Attribute.StyleClass(customFlag, AStyle(prop, arrayOf(prop to value)))

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

val alignTop = Attribute.AlignY(VAlign.Top)
val alignBottom = Attribute.AlignY(VAlign.Bottom)
val alignLeft = Attribute.AlignX(HAlign.Left)
val alignRight = Attribute.AlignX(HAlign.Left)
val centerX = Attribute.AlignX(HAlign.CenterX)
val centerY = Attribute.AlignY(VAlign.CenterY)

val spaceEvenly = Attribute.Class(Flag.spacing, Classes.spaceEvenly)
fun spacing(x: Int) = Attribute.StyleClass(Flag.spacing, SpacingStyle(spacingName(x, x), x, x))
fun spacingXY(x: Int, y: Int) = Attribute.StyleClass(Flag.spacing, SpacingStyle(spacingName(x, y), x, y))

/**
 * Make an element transparent and have it ignore any mouse or touch events, though it will stil take up space.
 */
fun transparent(on: Boolean) =
    if (on)
        Attribute.StyleClass(Flag.transparency, Transparency("transparent", 1f))
    else
        Attribute.StyleClass(Flag.transparency, Transparency("visible", 0f))

fun opacity(o: Float): Attribute.StyleClass {
    val transparency = 1f - min(1f, max(o, 0f))
    val byteVal = min(255, (transparency * 255f).toInt())
    return Attribute.StyleClass(Flag.transparency, Transparency("transparency-$byteVal", transparency))
}

fun padding(around: Int): Attribute.StyleClass =
    Attribute.StyleClass(Flag.padding, PaddingStyle("p-$around", around, around, around, around))

fun padding(t: Int, r: Int, b: Int, l: Int): Attribute.StyleClass =
    Attribute.StyleClass(Flag.padding, PaddingStyle("p-$t-$r-$b-$l", t, r, b, l))

fun paddingXY(x: Int, y: Int) =
    Attribute.StyleClass(Flag.padding, PaddingStyle("p-$x-$y", y, x, y, x))

inline fun paddingTop(top: Int) = padding(top, 0, 0, 0)
inline fun paddingBottom(bottom: Int) = padding(0, 0, bottom, 0)
inline fun paddingRight(right: Int) = padding(0, right, 0, 0)
inline fun paddingLeft(left: Int) = padding(0, 0, 0, left)

fun border(around: Int): Attribute.StyleClass =
    Attribute.StyleClass(Flag.borderWidth, BorderWidth("b-$around", around, around, around, around))

fun border(t: Int, r: Int, b: Int, l: Int): Attribute.StyleClass =
    Attribute.StyleClass(Flag.borderWidth, BorderWidth("b-$t-$r-$b-$l", t, r, b, l))

fun borderXY(x: Int, y: Int) =
    Attribute.StyleClass(Flag.borderWidth, BorderWidth("b-$x-$y", y, x, y, x))

inline fun borderTop(top: Int) = border(top, 0, 0, 0)
inline fun borderBottom(bottom: Int) = border(0, 0, bottom, 0)
inline fun borderRight(right: Int) = border(0, right, 0, 0)
inline fun borderLeft(left: Int) = border(0, 0, 0, left)

val pointer = Attribute.Class(Flag.cursor, Classes.cursorPointer)

fun backgroundColor_style(color: Color) = Colored("bg-color-${color.formatWithDashes()}", "background-color", color)
fun backgroundColor(color: Color) =
    Attribute.StyleClass(Flag.bgColor, backgroundColor_style(color))

fun fontColor(color: Color) =
    Attribute.StyleClass(Flag.fontColor, Colored("font-color-${color.formatWithDashes()}", "color", color))

fun onClick(handler: (Element) -> Unit) = Attribute.Events(j("click" to handler))