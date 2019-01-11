// shamelessly inspired by Elm package mdgriffith/elm-ui v1.1.0


package net.namekdev.entity_tracker.ui

import snabbdom.VNode
import snabbdom.h
import snabbdom.j
import net.namekdev.entity_tracker.ui.Attribute.*
import net.namekdev.entity_tracker.ui.LayoutContext.*
import snabbdom.VNodeData

/**
 * Render Context for snabbdom's VNode.
 * Instead of injecting styles to each node we generate
 * a big stylesheet by walking through a whole node tree.
 */
data class RNode(
    val vnode: VNode,
    val stylesheet: Stylesheet? = null
)


fun row(nodes: Array<RNode>): RNode =
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

fun column(nodes: Array<RNode>): RNode =
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


fun el(tag: String, attrs: Array<Attribute>, nodes: Array<RNode>): RNode =
    element(AsEl, ANodeName(tag), attrs, *nodes)

fun el(tag: String, vararg nodes: RNode): RNode =
    element(AsEl, ANodeName(tag), null, *nodes)

fun text(txt: String): RNode =
    RNode(textElement(txt))

internal inline fun textElement(text: String): VNode =
    h("div.${Classes.any}.${Classes.text}.${Classes.widthContent}.${Classes.heightContent}", text)

inline fun table(attrs: Array<Attribute>, header: RNode, vararg rows: RNode) =
    el("table", attrs = attrs, nodes = arrayOf(header, *rows))

inline fun tRow(vararg columns: RNode) =
    el("tr", nodes = *columns)

fun tCell(vararg cellContents: RNode): RNode =
    el("td", attrs(), arrayOf(
        element(LayoutContext.AsRow, Generic, null, *cellContents)
    ))

fun thCell(vararg cellContents: RNode): RNode =
    el("th", attrs(widthShrink), arrayOf(
        element(LayoutContext.AsRow, Generic, null, *cellContents)
    ))

fun tCell(text: String) =
    RNode(h("td", textElement(text)))

fun thCell(text: String) =
    RNode(h("th", textElement(text)))


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
    val stylesheet = nodes.firstOrNull()?.stylesheet ?: mutableMapOf()

    for (node in nodes) {
        node.stylesheet?.let {
            mergeStylesheet(stylesheet, it)
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
    var html = h("$tag$classes", vnodeData, nodes.map { it.vnode }.toTypedArray())

    when(context) {
        AsColumn -> {
            html = when {
                uiFlags and Flag.heightFill != 0 && uiFlags and Flag.heightBetween == 0 ->
                    html

                uiFlags and Flag.centerY != 0 ->
                    h("s.${Classes.any}.${Classes.single}.${Classes.container}.${Classes.alignContainerCenterY}", html)

                uiFlags and Flag.alignBottom != 0 ->
                    h("u.${Classes.any}.${Classes.single}.${Classes.container}.${Classes.alignContainerBottom}", html)

                else -> html
            }
        }

        AsRow -> {
            html =
                if (uiFlags and Flag.widthFill != 0 && uiFlags and Flag.widthBetween == 0) {
                    html
                }
                else if (uiFlags and Flag.alignRight != 0) {
                    h("u.${Classes.any}.${Classes.single}.${Classes.container}.${Classes.contentCenterY}.${Classes.alignContainerRight}", arrayOf(html))
                }
                else if (uiFlags and Flag.centerX != 0) {
                    h("s.${Classes.any}.${Classes.single}.${Classes.container}.${Classes.contentCenterY}.${Classes.alignContainerCenterX}", arrayOf(html))
                }
                else html
        }
    }

    return RNode(html, stylesheet)
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
        is Single -> style.klass
        is Colored -> style.cls
        is SpacingStyle -> style.cls
        is PaddingStyle -> style.cls
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
