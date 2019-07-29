// shamelessly inspired by Elm package mdgriffith/elm-ui v1.1.0


package net.namekdev.entity_tracker.ui

import net.namekdev.entity_tracker.ui.Attribute.*
import net.namekdev.entity_tracker.ui.LayoutContext.*
import snabbdom.*

/**
 * Render Context for snabbdom's VNode.
 * Instead of injecting styles to each node we generate
 * a big stylesheet by walking through a whole node tree.
 */
//data class RNode(
//    val vnode: VNode,
//    val stylesheet: Stylesheet? = null
//)
sealed class RNode
class Unstyled(val html: (LayoutContext) -> VNode) : RNode()
class Styled(val html: (LayoutContext) -> VNode, val styles : Stylesheet) : RNode()
class Text(val text: String) : RNode()
object Empty : RNode()


fun renderRoot(child: RNode): VNode =
    toHtml(element(AsEl, Generic, null, child))

fun renderRoot(attrs: Array<Attribute>, child: RNode): VNode =
    toHtml(element(AsEl, Generic, attrs, child))


private fun toHtml(el: RNode): VNode =
    when (el) {
        is Unstyled ->
            el.html(AsEl)

        is Styled ->
            // TODO embedMode
            el.html(AsEl)

        is Text ->
            textElement(el.text)

        is Empty ->
            textElement("")
    }


val none = Empty

fun row(vararg nodes: RNode): RNode =
    row(arrayOf(), *nodes)

fun row(attrs: Array<Attribute>, nodes: Array<RNode>): RNode =
    row(attrs, *nodes)

fun row(attrs: Array<Attribute>, vararg nodes: RNode): RNode =
    element(LayoutContext.AsRow, Generic,
        attrs(
            Attribute.Class(0, "${Classes.contentLeft} ${Classes.contentCenterY}"),
            Attribute.Width.Content, Attribute.Height.Content
        ) + attrs,
        *nodes
    )

// TODO: wrappedRow(), paragraph()

fun column(vararg nodes: RNode): RNode =
    column(arrayOf(), *nodes)

fun column(attrs: Array<Attribute>, nodes: Array<RNode>): RNode =
    column(attrs, *nodes)

fun column(attrs: Array<Attribute>, vararg nodes: RNode): RNode =
    element(LayoutContext.AsColumn, Generic,
        arrayOf(
            Attribute.Class(0, "${Classes.contentTop} ${Classes.contentLeft}"),
            Attribute.Width.Content, Attribute.Height.Content
        ) + attrs,
        *nodes
    )

inline fun el(attrs: Array<Attribute>, vararg nodes: RNode): RNode =
    el("div", attrs, *nodes)

fun el(vararg nodes: RNode): RNode =
    element(AsEl, ANodeName("div"), null, *nodes)

fun el(tag: String, attrs: Array<Attribute>, vararg nodes: RNode): RNode =
    element(AsEl, ANodeName(tag), attrs, *nodes)

fun el(tag: String, vararg nodes: RNode): RNode =
    element(AsEl, ANodeName(tag), null, *nodes)

fun elAsRow(tag: String, attrs: Array<Attribute>, nodes: Array<RNode>): RNode =
    element(AsRow, ANodeName(tag), attrs, *nodes)

fun elAsRow(tag: String, vararg nodes: RNode): RNode =
    element(AsRow, ANodeName(tag), null, *nodes)

fun text(text: String): Text =
    Text(text)


internal inline fun textElement(text: String): VNode =
    h("div.${Classes.any}.${Classes.text}.${Classes.widthContent}.${Classes.heightContent}", text)

internal inline fun textElementFill(text: String): VNode =
    h("div.${Classes.any}.${Classes.text}.${Classes.widthFill}.${Classes.heightFill}", text)

inline fun table(attrs: Array<Attribute>, header: RNode, vararg rows: RNode) =
    el("table", attrs, header, *rows)

inline fun table(attrs: Array<Attribute>, header: RNode, body: RNode) =
    el("table", attrs, header, body)

inline fun tHead(attrs: Array<Attribute>, row: RNode) =
    el("thead", attrs, row)

inline fun tHead(row: RNode) =
    el("thead", attrs(), row)

inline fun tBody(attrs: Array<Attribute>, vararg rows: RNode) =
    el("tbody", attrs, *rows)

inline fun tRow(vararg columns: RNode) =
    el("tr", *columns)

inline fun tRow(attrs: Array<Attribute>, vararg columns: RNode) =
    el("tr", attrs, *columns)

fun tCell(vararg cellContents: RNode): RNode =
    el("td", attrs(),
        element(LayoutContext.AsRow, Generic, null, *cellContents))

fun thCell(attrs: Array<Attribute>, vararg cellContents: RNode): RNode =
    el("th", attrs(widthShrink) + attrs,
        element(LayoutContext.AsRow, Generic, null, *cellContents))

fun thCell(vararg cellContents: RNode): RNode =
    el("th", attrs(widthShrink),
        element(LayoutContext.AsRow, Generic, null, *cellContents))


private fun element(
    context: LayoutContext,
    nodeName: NodeName,
    attrs: Array<Attribute>? = null,
    vararg nodes: RNode
): RNode {
    var classes = '.' + contextClasses(context).split(' ').joinToString(".")
    var uiFlags = 0
    val vnodeData = VNodeData()

    // we won't create a new stylesheet object until we don't have to.
    // There's supposed to be a one global stylesheet passed through whole node tree!
    val stylesheet = (nodes.firstOrNull { it is Styled } as? Styled)?.styles ?: mutableMapOf()

    val vnodes = mutableListOf<VNode>()
    for (node in nodes) {
        when (node) {
            is Unstyled -> {
                vnodes.add(node.html(context))
            }
            is Styled -> {
                vnodes.add(node.html(context))
                mergeStylesheet(stylesheet, node.styles)
            }
            is Text -> {
                vnodes.add(
                    if (context == AsEl) {
                        textElementFill(node.text)
                    }
                    else
                        textElement(node.text)
                )
            }
            is Empty -> { }
        }
    }

    if (attrs != null) {
        for (i in attrs.size-1 downTo 0) {
            val attr = attrs[i]
            val r: SizingRender? = when(attr) {
                is NoAttribute ->
                    null

                is Events -> {
                    vnodeData.on = attr.on
                    null
                }

                is Width ->
                    if (uiFlags and Flag.width == 0)
                        renderWidth(attr)
                    else null

                is Height ->
                    if (uiFlags and Flag.height == 0)
                        renderHeight(attr)
                    else null

                is Class -> {
                    if (uiFlags and attr.flag == 0)
                        SizingRender(attr.flag, attr.exactClassName)
                    else null
                }

                is StyleClass -> {
                    if (uiFlags and attr.flag == 0)
                        SizingRender(attr.flag, getStyleName(attr.style), arrayOf(attr.style))
                    else null
                }

                is AlignX -> {
                    if (uiFlags and Flag.xAlign == 0)
                        renderAlignX(attr)
                    else null
                }

                is AlignY -> {
                    if (uiFlags and Flag.yAlign == 0)
                        renderAlignY(attr)
                    else null
                }
            }

            if (r != null) {
                classes += '.' + r.classes.split(' ').joinToString(".")
                uiFlags = uiFlags or r.flags

                // dp not put styles properties into elements
                // instead generate <style> element containing everything
                r.styles?.let { styles ->
                    if (vnodeData.style == null)
                        vnodeData.style = j()

                    for (s in styles) {
                        val styleName = getStyleName(s)

                        if (stylesheet.containsKey(styleName))
                            continue

                        stylesheet.put(styleName, s)
                    }
                }
            }
        }
    }

    val tag = when(nodeName) {
        Generic -> "div"
        else -> nodeName.nodeName
    }

    inline fun flagPresent(flag: Int): Boolean = (uiFlags and flag) != 0

    val getHtml: (LayoutContext) -> VNode = { context ->
        val html: VNode = h("$tag$classes", vnodeData, vnodes.toTypedArray())

        when (context) {
            AsColumn -> {
                when {
                    flagPresent(Flag.heightFill) && !flagPresent(Flag.heightBetween) ->
                        html

                    flagPresent(Flag.centerY) ->
                        h("s.${Classes.any}.${Classes.single}.${Classes.container}.${Classes.alignContainerCenterY}", html)

                    flagPresent(Flag.alignBottom) ->
                        h("u.${Classes.any}.${Classes.single}.${Classes.container}.${Classes.alignContainerBottom}", html)

                    else -> html
                }
            }

            AsRow -> {
                if (flagPresent(Flag.widthFill) && !flagPresent(Flag.widthBetween)) {
                    html
                }
                else if (flagPresent(Flag.alignRight)) {
                    h("u.${Classes.any}.${Classes.single}.${Classes.container}.${Classes.contentCenterY}.${Classes.alignContainerRight}", arrayOf(html))
                }
                else if (flagPresent(Flag.centerX)) {
                    h("s.${Classes.any}.${Classes.single}.${Classes.container}.${Classes.contentCenterY}.${Classes.alignContainerCenterX}", arrayOf(html))
                }
                else html
            }

            else -> html
        }
    }

    return if (stylesheet.isEmpty())
        Unstyled(getHtml)
    else
        Styled(getHtml, stylesheet)
}

fun width(length: Length): Attribute.Width =
    when(length) {
        is Length.Px ->
            Attribute.Width.Px(length.px)
        is Length.Fill ->
            Attribute.Width.Fill(length.portion)
        is Length.Content ->
            Attribute.Width.Content
        is Length.Min ->
            TODO()
        is Length.Max ->
            TODO()
    }

fun height(length: Length): Attribute.Height =
    when(length) {
        is Length.Px ->
            Attribute.Height.Px(length.px)
        is Length.Fill ->
            Attribute.Height.Fill(length.portion)
        is Length.Content ->
            Attribute.Height.Content
        is Length.Min ->
            TODO()
        is Length.Max ->
            TODO()
    }

private fun renderWidth(w: Attribute.Width): SizingRender =
    when (w) {
        is Width.Px ->
            SizingRender(Flag.width, Classes.widthExact + " width-px-${w.px}",
                arrayOf(Single("width-px-${w.px}", "width", "${w.px}px")))

        is Width.Content ->
            SizingRender(Flag.width or Flag.widthContent, Classes.widthContent)

        is Width.Fill ->
            if (w.portion == 1) {
                SizingRender(Flag.width or Flag.widthFill, Classes.widthFill)
            }
            else {
                SizingRender(Flag.width or Flag.widthFill, "${Classes.widthFillPortion} width-fill-${w.portion}",
                    arrayOf(Single("${Classes.any}.${Classes.row} > .width-fill-${w.portion}", "flex-grow", (w.portion * 100_000).toString())))
            }

        is Width.Min -> TODO()
        is Width.Max -> TODO()
    }

private fun renderHeight(h: Height): SizingRender =
    when (h) {
        is Height.Px -> {
            val name = "height-px-${h.px}"
            SizingRender(Flag.height, name, arrayOf(Single(name, "height", "${h.px}px")))
        }

        is Height.Content ->
            SizingRender(Flag.height or Flag.heightContent, Classes.heightContent)

        is Height.Fill ->
            if (h.portion == 1) {
                SizingRender(Flag.height or Flag.heightFill, Classes.heightFill)
            }
            else {
                SizingRender(Flag.height or Flag.heightFill, "${Classes.heightFillPortion} height-fill-${h.portion}",
                    arrayOf(Single("${Classes.any}.${Classes.row} > .height-fill-${h.portion}", "flex-grow", (h.portion * 100_000).toString())))
            }

        is Height.Min -> TODO()
        is Height.Max -> TODO()
    }

private fun renderAlignX(ax: AlignX): SizingRender {
    val flag = when(ax.hAlign) {
        HAlign.CenterX -> Flag.centerX
        HAlign.Right -> Flag.alignRight
        else -> 0
    }
    return SizingRender(Flag.xAlign or flag, alignXName(ax.hAlign))
}

private fun renderAlignY(ay: AlignY): SizingRender {
    val flag = when(ay.vAlign) {
        VAlign.CenterY -> Flag.centerY
        VAlign.Bottom -> Flag.alignBottom
        else -> 0
    }
    return SizingRender(Flag.xAlign or flag, alignYName(ay.vAlign))
}

private fun alignXName(x: HAlign): String = when(x) {
    HAlign.Left -> "${Classes.alignedHorizontally} ${Classes.alignLeft}"
    HAlign.Right -> "${Classes.alignedHorizontally} ${Classes.alignRight}"
    HAlign.CenterX -> "${Classes.alignedHorizontally} ${Classes.alignCenterX}"
}

private fun alignYName(y: VAlign): String = when(y) {
    VAlign.Top -> "${Classes.alignedVertically} ${Classes.alignTop}"
    VAlign.Bottom -> "${Classes.alignedVertically} ${Classes.alignBottom}"
    VAlign.CenterY -> "${Classes.alignedVertically} ${Classes.alignCenterY}"
}

private fun getStyleName(style: Style): String =
    when(style) {
        is Transparency -> style.name
        is AStyle -> style.selector
        is FontSize -> "font-size-${style.size}"
        is Single -> style.klass
        is Colored -> style.cls
        is SpacingStyle -> style.cls
        is PaddingStyle -> style.cls
        is BorderWidth -> style.cls
        is PseudoSelector -> {
            val name = when (style.cls) {
                PseudoClass.Focus -> "fs"
                PseudoClass.Hover -> "hv"
                PseudoClass.Active -> "act"
            }
            style.styles.joinToString(" ") {
                val styleName = getStyleName(it)
                when (styleName) {
                    "" -> ""
                    else -> "$styleName-$name"
                }
            }
        }
        is Transform ->
            transformClass(style.transformation) ?: ""
    }

internal object Flag {
    const val heightFill = 1 shl 0
    const val heightBetween = 1 shl 1
    const val centerY = 1 shl 2
    const val centerX = 1 shl 3
    const val alignBottom = 1 shl 4
    const val alignTop = 1 shl 5
    const val widthFill = 1 shl 6
    const val widthBetween = 1 shl 7
    const val alignRight = 1 shl 8
    const val alignLeft = 1 shl 9
    const val width = 1 shl 10
    const val height = 1 shl 11
    const val widthContent = 1 shl 12
    const val heightContent = 1 shl 13
    const val contentLeft = 1 shl 14
    const val contentTop = 1 shl 14
    const val xAlign = 1 shl 15
    const val yAlign = 1 shl 16
    const val padding = 1 shl 17
    const val bgColor = 1 shl 18
    const val fontColor = 1 shl 19
    const val spacing = 1 shl 20
    const val cursor = 1 shl 21
    const val transparency = 1 shl 22
    const val borderWidth = 1 shl 23
    const val fontAlignment = 1 shl 24
    const val fontSize = 1 shl 25
    const val hover = 1 shl 26
    const val active = 1 shl 27
    const val focus = 1 shl 28
}

enum class LayoutContext {
    AsRow,
    AsColumn,
    AsEl,
    //    AsGrid,
    AsParagraph,
//    AsTextColumn //page
}

private sealed class NodeName(val nodeName: String)
private class ANodeName(nodeName: String) : NodeName(nodeName)
private object Generic : NodeName("div")
//private class Embedded(nodeName: String, val internal: String) : NodeName(nodeName)

private fun contextClasses(context: LayoutContext): String =
    when (context) {
        AsRow -> "${Classes.any} ${Classes.row}"
        AsColumn -> "${Classes.any} ${Classes.column}"
        AsEl -> "${Classes.any} ${Classes.single}"
//        AsGrid -> "${Classes.any} $grid"
        AsParagraph -> "${Classes.any} ${Classes.paragraph}"
//        AsTextColumn -> "${Classes.any} $page"
    }

object Classes {
    const val alignContainerRight = "acr"
    const val alignContainerBottom = "acb"
    const val alignContainerCenterX = "accx"
    const val alignContainerCenterY = "accy"
    const val alignCenterX = "acx"
    const val alignCenterY = "acy"
    const val alignTop = "atop"
    const val alignBottom = "abottom"
    const val alignRight = "ar"
    const val alignLeft = "al"
    const val alignedHorizontally = "ahorz"
    const val alignedVertically = "avert"
    const val contentCenterX = "ccx"
    const val contentCenterY = "ccy"
    const val contentTop = "ct"
    const val contentBottom = "cb"
    const val contentRight = "cr"
    const val contentLeft = "cl"
    const val container = "c"
    const val single = "s"
    const val any = "any"
    const val wrapped = "w"
    const val row = "row"
    const val column = "col"
    const val paragraph = "par"
    const val widthExact = "we"
    const val heightContent = "hc"
    const val heightFill = "hf"
    const val heightFillPortion = "hfp"
    const val widthFill = "wf"
    const val widthFillPortion = "wfp"
    const val widthContent = "wc"
    const val spaceEvenly = "se"
    const val imageContainer = "ic"
    const val root = "root"
    const val borderNone = "bn"
    const val textJustify = "tj"
    const val textJustifyAll = "tja"
    const val textCenter = "tc"
    const val textRight = "tr"
    const val textLeft = "tl"
    const val hidden = "hidden"
    const val text = "txt"
    const val noTextSelection = "nts"
    const val cursorPointer = "cptr"
    const val cursorText = "ctxt"
}
