// shamelessly inspired by Elm package mdgriffith/elm-ui v1.1.0


package net.namekdev.entity_tracker.ui

import snabbdom.VNode
import snabbdom.h

fun row(nodes: Array<VNode>): VNode =
    row(0, *nodes)

fun row(uiFlags: Int, nodes: Array<VNode>): VNode =
    row(uiFlags, *nodes)

fun row(vararg nodes: VNode): VNode =
    row(uiFlags = 0, nodes = *nodes)

fun row(uiFlags: Int, vararg nodes: VNode): VNode {
    val html = h("div", *nodes)

    return if (uiFlags and Flag.widthFill != 0 && uiFlags and Flag.widthBetween == 0) {
        html
    } else if (uiFlags and Flag.alignRight != 0) {
        h("u.$any.$single.$container.$contentCenterY.$alignContainerRight", arrayOf(html))
    } else if (uiFlags and Flag.centerX != 0) {
        h("s.$any.$single.$container.$contentCenterY.$alignContainerCenterX", arrayOf(html))
    } else html
}


fun column(uiFlags: Int, nodes: Array<VNode>): VNode =
    column(uiFlags, *nodes)

fun column(nodes: Array<VNode>): VNode =
    column(uiFlags = 0, nodes = *nodes)

fun column(vararg nodes: VNode): VNode =
    column(0, *nodes)

fun column(uiFlags: Int, vararg nodes: VNode): VNode {
    val html = h("div", arrayOf(*nodes))

    return when {
        uiFlags and Flag.heightFill != 0 && uiFlags and Flag.heightBetween == 0 ->
            html

        uiFlags and Flag.centerY != 0 ->
            h("s.$any.$single.$container.$alignContainerCenterY", html)

        uiFlags and Flag.alignBottom != 0 ->
            h("u.$any.$single.$container.$alignContainerBottom", html)

        else -> html
    }
}




object Flag {
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
}


val alignContainerRight = "acr"
val alignContainerBottom = "acb"
val alignContainerCenterX = "accx"
val alignContainerCenterY = "accy"
val alignCenterX = "acx"
val alignCenterY = "acy"
val alignTop = "atop"
val alignBottom = "abottom"
val alignRight = "ar"
val alignLeft = "al"
val contentCenterX = "ccx"
val contentCenterY = "ccy"
val contentTop = "ct"
val contentBottom = "cb"
val contentRight = "cr"
val contentLeft = "cl"
val container = "c"
val single = "s"
val any = "a"
val wrapped = "w"
val row = "row"
val column = "col"
val paragraph = "par"
val widthExact = "we"
val heightContent = "hc"
val heightFill = "hf"
val heightFillPortion = "hfp"
val widthFill = "wf"
val widthFillPortion = "wfp"
val widthContent = "wc"
val spaceEvenly = "se"
val imageContainer = "ic"
val root = "root"
val borderNone = "bn"
val textJustify = "tj"
val textJustifyAll = "tja"
val textCenter = "tc"
val textRight = "tr"
val textLeft = "tl"
val hidden = "hidden"
val text = "txt"

enum class Alignment {
    Top, Bottom, Right, Left, CenterX, CenterY
}

fun selfName(alignment: Alignment): String =
    when (alignment) {
        Alignment.Top -> alignTop
        Alignment.Bottom -> alignBottom
        Alignment.Right -> alignRight
        Alignment.Left -> alignLeft
        Alignment.CenterX -> alignCenterX
        Alignment.CenterY -> alignCenterY
    }

fun contentName(alignment: Alignment): String =
    when (alignment) {
        Alignment.Top -> contentTop
        Alignment.Bottom -> contentBottom
        Alignment.Right -> contentRight
        Alignment.Left -> contentLeft
        Alignment.CenterX -> contentCenterX
        Alignment.CenterY -> contentCenterY
    }

fun describeAlignments(parentDescriptor: String, values: ((Alignment) -> Pair<String, String>)): String {
    val sb = StringBuilder()
    for (alignment in Alignment.values()) {
        val (content, indiv) = values(alignment)

        sb.append("""
            $parentDescriptor .${contentName(alignment)}
                $content

            $parentDescriptor > .$any .${selfName(alignment)}
                $indiv
        """.trimIndent())
    }

    return sb.toString()
}

fun rule(content: String, indiv: String): Pair<String, String> =
    Pair("{ $content }", "{ $indiv }")


fun describeAlignments_column(parentDescriptor: String): String =
    describeAlignments(parentDescriptor) {
        when(it) {
            Alignment.Top -> rule(
                "justify-content: flex-start;",
                "margin-bottom: auto;"
            )
            Alignment.Bottom -> rule(
                "justify-content: flex-end;",
                "margin-top: auto;"
            )
            Alignment.Right -> rule(
                "align-items: flex-end;",
                "align-self: flex-end;"
            )
            Alignment.Left -> rule(
                "justify-content: flex-start;",
                ""
            )
            Alignment.CenterX -> rule(
                "align-items: center;",
                "align-self: center;"
            )
            Alignment.CenterY -> rule(
                "justify-content: center;",
                ""
            )
        }
    }

fun describeAlignments_row(parentDescriptor: String): String =
    describeAlignments(parentDescriptor) {
        when(it) {
            Alignment.Top -> rule(
                "align-items: flex-start;",
                "align-self: flex-start;"
            )
            Alignment.Bottom -> rule(
                "align-items: flex-end;",
                "align-self: flex-end;"
            )
            Alignment.Right -> rule(
                "justify-content: flex-end;",
                ""
            )
            Alignment.Left -> rule(
                "justify-content: flex-start;",
                ""
            )
            Alignment.CenterX -> rule(
                "justify-content: center;",
                ""
            )
            Alignment.CenterY -> rule(
                "align-items: center;",
                "align-self: center;"
            )
        }
    }

fun describeAlignments_el(parentDescriptor: String): String =
    describeAlignments(parentDescriptor) {
        when (it) {
            Alignment.Top -> rule(
                "justify-content: flex-start;",
                "margin-bottom: auto !important; margin-top: 0 !important;"
            )
            Alignment.Bottom -> rule(
                "justify-content: flex-end;",
                "margin-top: auto !important; margin-bottom: 0 !important;"
            )
            Alignment.Right -> rule(
                "align-items: flex-end;",
                "align-self: flex-end;"
            )
            Alignment.Left -> rule(
                "align-items: flex-start;",
                "align-self: flex-start;"
            )
            Alignment.CenterX -> rule(
                "align-items: center;",
                "align-self: center;"
            )
            Alignment.CenterY -> Pair(
                "> .$any { margin-top: auto; margin-bottom: auto; }",
                "{ margin-top: auto !important; margin-bottom: auto !important; }"
            )
        }
    }

fun describeAlignments_paragraph(parentDescriptor: String): String =
    describeAlignments(parentDescriptor) {
        when (it) {
            Alignment.Top -> rule("", "")
            Alignment.Bottom -> rule("", "")
            Alignment.Right -> rule("", "float: right;")
            Alignment.Left -> rule("", "float: left;")
            Alignment.CenterX -> rule("", "")
            Alignment.CenterY -> rule("", "")
        }
    }

fun elDescription(d: String) = """
    $d {
        display: flex;
        flex-direction: column;
        white-space: pre;
    }
    $d > .$heightContent {
        height: auto;
    }
    $d > .$heightFill {
        flex-grow: 100000;
    }
    $d > .$widthFill {
        width: 100%;
    }
    $d > .$widthContent {
        align-self: flex-start;
    }

    ${describeAlignments_el(d)}
""".trimIndent()

val globalStylesheet = """
    html,body {
        height: 100%;
        padding: 0;
        margin: 0;
    }
    .$any.$single.$imageContainer {
        display: block;
    }
    .$any:focus {
        outline: none;
    }
    .$root {
        width: 100%;
        height: auto;
        min-height: 100%;
        z-index: 0;
    }
    .$root .$any.$heightFill {
        height: 100%;
    }
    .$root .$any.$heightFill > .$heightFill {
        height: 100%
    }

    .$any {
        position: relative;
        border: none;
        flex-shrink: 0;
        display: flex;
        flex-direction: row;
        flex-basis: auto;
        resize: none;
        font-feature-settings: inherit;
        flex-basis: 0%;
        box-sizing: border-box;
        margin: 0;
        padding: 0;
        border-width: 0;
        border-style: solid;

        font-size: inherit;
        color: inherit;
        font-family: inherit;
        line-height: 1;
        font-weight: inherit;
        text-decoration: none;
        font-style: inherit;
    }

    .$any .$wrapped {
        flex-wrap: wrap;
    }

    .$any .$widthContent {
        width: auto;
    }

    .$any .$borderNone {
        border-width: 0;
    }

    ${elDescription(".$any .$single")}

    .$any .$row {
        display: flex;
        flex-direction: row;
    }
    .$any .$row > .$any {
        flex-basis: 0%;
    }
    .$any .$row > .$any $widthExact {
        flex-basis: auto;
    }
    .$any .$row > .$heightFill {
        align-self: stretch !important;
    }
    .$any .$row > .$heightFillPortion {
        align-self: stretch !important;
    }
    .$any .$row > .$widthFill {
        flex-grow: 100000;
    }
    .$any .$row > .$container {
        flex-grow: 0;
        flex-basis: auto;
        align-self: stretch;
    }
    .$any .$row > u:first-of-type.$alignContainerRight {
        flex-grow: 1;
    }
    .$any .$row > s:first-of-type.$alignContainerCenterX {
        flex-grow: 1;
    }
    .$any .$row > s:first-of-type.$alignContainerCenterX > .$alignCenterX {
        margin-left: auto !important;
    }
    .$any .$row > s:last-of-type.$alignContainerCenterX {
        flex-grow: 1;
    }
    .$any .$row > s:last-of-type.$alignContainerCenterX > $alignCenterX {
        margin-right: auto !important;
    }
    .$any .$row > s:only-of-type.$alignContainerCenterX {
        flex-grow: 1;
    }
    .$any .$row > s:only-of-type.$alignContainerCenterX > .$alignCenterY {
        margin-top: auto !important;
        margin-bottom: auto !important;
    }
    .$any .$row > s:last-of-type.$alignContainerCenterX ~ u {
        flex-grow: 0;
    }
    .$any .$row > u:first-of-type.$alignContainerRight ~ s.$alignContainerCenterX {
        flex-grow: 0;
    }
    ${describeAlignments_row(".$any .$row")}
    .$any .$row .$spaceEvenly {
        justify-content: space-between;
    }

    .$any .$column {
        display: flex;
        flex-direction: column;
    }
    .$any .$column > .$heightFill {
        flex-grow: 100000;
    }
    .$any .$column > .$widthFill {
        width: 100%;
    }
    .$any .$column > .$widthFillPortion {
        width: 100%;
    }
    .$any .$column > .$widthContent {
        align-self: flex-start;
    }
    .$any .$column > u:first-of-type.$alignContainerBottom {
        flex-grow: 1;
    }
    .$any .$column > u:first-of-type.$alignContainerCenterY {
        flex-grow: 1
    }
    .$any .$column > u:first-of-type.$alignContainerCenterY > .$alignCenterY {
        margin-top: auto !important;
        margin-bottom: 0 !important;
    }
    .$any .$column > u:last-of-type.$alignContainerCenterY {
        flex-grow: 1;
    }
    .$any .$column > u:last-of-type.$alignContainerCenterY > .$alignCenterY {
        margin-bottom: auto !important;
        margin-top: 0 !important;
    }
    .$any .$column > s:only-of-type.$alignContainerCenterY {
        flex-grow: 1;
    }
    .$any .$column > s:only-of-type.$alignContainerCenterY > .$alignCenterY {
        margin-top: auto !important;
        margin-bottom: auto !important;
    }
    .$any .$column > s:last-of-type.$alignContainerCenterY ~ u {
        flex-grow: 0;
    }
    .$any .$column > u:first-of-type.$alignContainerBottom ~ s.$alignContainerCenterY {
        flex-grow: 0;
    }

    ${describeAlignments_column(".$any .$column")}

    .$any .$column > .$container {
        flex-grow: 0;
        flex-basis: auto;
        width: 100%;
        align-self: stretch !important;
    }

    .$any .$column .$spaceEvenly {
        justify-content: space-between;
    }

    .$any .$paragraph {
        display: block;
        white-space: normal;
    }
    .$any .$paragraph > .$text {
        display: inline;
        white-space: normal;
    }
    .$any .$paragraph > .$single {
        display: inline;
        white-space: normal;
    }
    .$any .$paragraph > .$row {
        display: inline-flex;
    }
    .$any .$paragraph > .$column {
        display: inline-flex;
    }
    ${describeAlignments_paragraph(".$any .$paragraph")}
    .$any .$hidden {
        display: none;
    }
    .$any .$textJustify {
        text-align: justify;
    }
    .$any .$textJustifyAll {
        text-align: justify-all;
    }
    .$any .$textCenter {
        text-align: center;
    }
    .$any .$textRight {
        text-align: right;
    }
    .$any .$textLeft {
        text-align: left;
    }

     // lines 1659-1670

""".trimIndent()
