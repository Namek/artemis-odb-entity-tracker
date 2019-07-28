package net.namekdev.entity_tracker.ui

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


typealias Stylesheet = MutableMap<String, Style>
typealias Color = Rgba

data class Rgba(val r: Short, val g: Short, val b: Short, val a: Short) {
    fun formatWithDashes(): String = "$r-$g-$b-$a"
    fun format(): String = "rgba($r,$g,$b,${a / 255f})"
}

fun hexToColor(rgb: Int, alpha: Short = 255.toShort()): Rgba =
    Rgba(((rgb shr 16) and 0xFF).toShort(), ((rgb shr 8) and 0xFF).toShort(), (rgb and 0xFF).toShort(), alpha)

fun rgb(r: Short, g: Short, b: Short) = Rgba(r, g, b, 255)
fun rgba(r: Short, g: Short, b: Short, alpha: Short) = Rgba(r, g, b, alpha)

data class OptionRecord(val hover: HoverSetting, val focus: FocusStyle)

enum class HoverSetting { NoHover, AllowHover, ForceHover }
data class FocusStyle(
    val borderColor: Color? = null,
    val shadow: Shadow? = null,
    val backgroundColor: Color? = null
)

data class Shadow(
    val color: Color,
    val offset: Pair<Int, Int>,
    val blur: Int,
    val size: Int
)

enum class PseudoClass { Focus, Hover, Active }

internal fun mergeStylesheet(to: Stylesheet, from: Stylesheet) {
    for ((k, v) in from) {
        if (!to.containsKey(k))
            to.put(k, v)
    }
}

internal fun toStyleSheetString(options: OptionRecord, stylesheet: Collection<Style>): String {
    val sb = StringBuilder()
    for (style in stylesheet) {
        renderStyleRule(sb, options, style, null)
    }
    return sb.toString()
}

internal fun renderStyleRule(sb: StringBuilder, options: OptionRecord, rule: Style, maybePseudo: PseudoClass?) {
    val renderStyle = {selector: String, props: Array<Pair<String, String>> ->
        renderStyle(sb, maybePseudo, options, selector, props)
    }
    when (rule) {
        is AStyle -> {
            renderStyle(rule.selector, rule.props)
        }
        is Transparency -> {
            val opacity = max(0f, min(1f, 1f - rule.alpha))
            renderStyle(".${rule.name}", arrayOf(Pair("opacity", opacity.toString())))
        }
        // TODO Shadows, FontFamily
        is FontSize -> {
            renderStyle(".font-size-${rule.size}", arrayOf(Pair("font-size", "${rule.size}px")))
        }
        is Single -> {
            renderStyle(".${rule.klass}", arrayOf(Pair(rule.prop, rule.value)))
        }
        is Colored -> {
            renderStyle(".${rule.cls}", arrayOf(Pair(rule.prop, rule.color.format())))
        }
        is SpacingStyle -> {
            val cls = ".${rule.cls}"
            val halfX = "${rule.x.toFloat() / 2}px"
            val halfY = "${rule.y.toFloat() / 2}px"
            val xPx = "${rule.x}px"
            val yPx = "${rule.y}px"
            val row = ".${Classes.row}"
            val wrappedRow = ".${Classes.wrapped}$row"
            val column = ".${Classes.column}"
            val paragraph = ".${Classes.paragraph}"
            val left = ".${Classes.alignLeft}"
            val right = ".${Classes.alignRight}"
            val any = ".${Classes.any}"
            val single = ".${Classes.single}"

            renderStyle("$cls$row > $any + $any", arrayOf(Pair("margin-left", xPx)))
            renderStyle("$cls$wrappedRow > $any", arrayOf(Pair("margin", "$halfY $halfX")))
            renderStyle("$cls$column > $any + $any", arrayOf(Pair("margin-top", yPx)))
            // note: omitted `.cls.page` here
            renderStyle("$cls$paragraph", arrayOf(Pair("line-height", "calc(1em + ${rule.y}px)")))
            renderStyle("textarea$cls", arrayOf(Pair("line-height", "calc(1em + ${rule.y}px)")))
            renderStyle("$cls$paragraph > $left", arrayOf(Pair("margin-right", xPx)))
            renderStyle("$cls$paragraph > $right", arrayOf(Pair("margin-left", xPx)))
            renderStyle(
                "$cls$paragraph::after", arrayOf(
                    Pair("content", "''"),
                    Pair("display", "block"),
                    Pair("height", "0"),
                    Pair("width", "0"),
                    Pair("margin-top", "${-1 * rule.y / 2}px")
                )
            )
            renderStyle(
                "$cls$paragraph::before", arrayOf(
                    Pair("content", "''"),
                    Pair("display", "block"),
                    Pair("height", "0"),
                    Pair("width", "0"),
                    Pair("margin-bottom", "${-1 * rule.y / 2}px")
                )
            )
        }
        is PaddingStyle -> {
            val padding = "${rule.top}px ${rule.right}px ${rule.bottom}px ${rule.left}px"
            renderStyle(".${rule.cls}", arrayOf(Pair("padding", padding)))
        }
        is BorderWidth -> {
            val borderWidth = "${rule.top}px ${rule.right}px ${rule.bottom}px ${rule.left}px"
            renderStyle(".${rule.cls}", arrayOf(Pair("border-width", borderWidth)))
        }
        is PseudoSelector -> {
            val ps = rule.cls
            for (style in rule.styles) {
                renderStyleRule(sb, options, style, ps)
            }
        }
        is Transform -> {
            val value = transformValue(rule.transformation)
            val klass = transformClass(rule.transformation)

            if (value != null && klass != null)
                renderStyle(".${klass}", arrayOf(Pair("transform", value)))
        }
    }
}

internal fun renderStyle(
    sb: StringBuilder,
    maybePseudo: PseudoClass?,
    options: OptionRecord,
    selector: String,
    props: Array<Pair<String, String>>
) {
    if (maybePseudo == null) {
        sb.append(selector, "{")
        renderProps(sb, false, props)
        sb.append("\n}")
    } else {
        when (maybePseudo) {
            PseudoClass.Hover -> {
                when (options.hover) {
                    HoverSetting.NoHover -> {
                    }

                    HoverSetting.ForceHover -> {
                        sb.append(selector, "-hv {")
                        renderProps(sb, true, props)
                        sb.append("\n}")
                    }

                    HoverSetting.AllowHover -> {
                        sb.append(selector, "-hv:hover {")
                        renderProps(sb, false, props)
                        sb.append("\n}")
                    }
                }
            }

            PseudoClass.Focus -> {
                val tmpSb = StringBuilder()
                renderProps(tmpSb, false, props)

                val renderedProps = tmpSb.toString()

                sb.append(selector, "-fs:focus {", renderedProps, "\n}\n")
                sb.append("\n")
                sb.append(".", Classes.any, ":focus ~ ", selector, "-fs:not(.focus)  {", renderedProps, "\n}\n")
                sb.append("\n")
                sb.append(".", Classes.any, ":focus ", selector, "-fs  {", renderedProps, "\n}")
                sb.append("\n")
                sb.append(".focusable-parent:focus ~ .", Classes.any, " ", selector, "-fs {", renderedProps, "\n}")
            }

            PseudoClass.Active -> {
                sb.append(selector, "-act:active {")
                renderProps(sb, false, props)
                sb.append("\n}")
            }
        }
    }
}

internal inline fun renderProps(sb: StringBuilder, force: Boolean, props: Array<Pair<String, String>>) {
    sb.append("\n  ")
    for (prop in props)
        sb.append(prop.first, ": ", prop.second)
    sb.append(if (force) " !important;" else ";")
}

internal fun transformClass(transformation: Transformation): String? {
    val t = transformation
    val fc = ::floatClass

    return when (t) {
        is Untransformed -> null

        is Moved ->
            "mv-${fc(t.xyz.x)}-${fc(t.xyz.y)}-${fc(t.xyz.z)}"

        is FullTransform ->
            "tfrm-${fc(t.translate.x)}-${fc(t.translate.x)}-${fc(t.translate.x)}" +
                    "-${fc(t.scale.x)}-${fc(t.scale.y)}-${fc(t.scale.z)}" +
                    "-${fc(t.rotate.x)}-${fc(t.rotate.y)}-${fc(t.rotate.z)}" +
                    "-${fc(t.angle)}"
    }
}

private fun transformValue(transformation: Transformation): String? {
    val t = transformation

    return when (t) {
        is Untransformed -> null

        is Moved ->
            "translate3d(${t.xyz.x}px, ${t.xyz.y}px, ${t.xyz.z}px)"

        is FullTransform ->
            "translate3d(${t.translate.x}px, ${t.translate.y}px, ${t.translate.z}px) " +
            "scale3d(${t.scale.x}, ${t.scale.y}, ${t.scale.z}) " +
            "rotate3d(${t.rotate.x}, ${t.rotate.y}, ${t.rotate.z}, ${t.angle}rad)"
    }
}


val globalStylesheet =
// note: those below are NOT copied from elm-ui!

// Table exists as a grid in elm-ui so we need to patch it
// with those simple rules so we can use them without grids:
    """
    .${Classes.any} > table {
        display: table !important;
    }
    .${Classes.any} > table tr {
        display: table-row !important;
    }
    .${Classes.any} > table th,
    .${Classes.any} > table td {
        display: table-cell !important;
        text-align: left;
    }
""".trimIndent() +

// We also need some style for buttons, don't we?
    """
    button {
        background-color: aliceblue;
        border: 0.5px solid rgb(170,170,170);
        border-radius: 3px;
        padding: 1px 4px !important;
    }
    """.trimIndent() +

// note: those below ARE copied from elm-ui!
    """
    html,body {
        height: 100%;
        padding: 0;
        margin: 0;
    }
    .${Classes.any}.${Classes.single}.${Classes.imageContainer} {
        display: block;
    }
    .${Classes.any}:focus {
        outline: none;
    }
    .${Classes.root} {
        width: 100%;
        height: auto;
        min-height: 100%;
        z-index: 0;
    }
    .${Classes.root}.${Classes.any}.${Classes.heightFill} {
        height: 100%;
    }
    .${Classes.root}.${Classes.any}.${Classes.heightFill} > .${Classes.heightFill} {
        height: 100%
    }

    .${Classes.any} {
        position: relative;
        border: none;
        flex-shrink: 0;
        display: flex;
        flex-direction: row;
        flex-basis: auto;
        resize: none;
        font-feature-settings: inherit;

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

    .${Classes.any}.${Classes.wrapped} {
        flex-wrap: wrap;
    }

    .${Classes.any}.${Classes.noTextSelection} {
        -moz-user-select: none;
        -webkit-user-select: none;
        -ms-user-select: none;
        user-select: none;
    }

    .${Classes.any}.${Classes.cursorPointer} {
        cursor: pointer;
    }

    .${Classes.any}.${Classes.cursorText} {
        cursor: text;
    }

    .${Classes.any}.${Classes.widthContent} {
        width: auto;
    }

    .${Classes.any}.${Classes.borderNone} {
        border-width: 0;
    }

    ${elDescription(".${Classes.any}.${Classes.single}")}

    .${Classes.any}.${Classes.row} {
        display: flex;
        flex-direction: row;
    }
    .${Classes.any}.${Classes.row} > .${Classes.any} {
        flex-basis: 0%;
    }
    .${Classes.any}.${Classes.row} > .${Classes.any}.${Classes.widthExact} {
        flex-basis: auto;
    }
    .${Classes.any}.${Classes.row} > .${Classes.heightFill} {
        align-self: stretch !important;
    }
    .${Classes.any}.${Classes.row} > .${Classes.heightFillPortion} {
        align-self: stretch !important;
    }
    .${Classes.any}.${Classes.row} > .${Classes.widthFill} {
        flex-grow: 100000;
    }
    .${Classes.any}.${Classes.row} > .${Classes.container} {
        flex-grow: 0;
        flex-basis: auto;
        align-self: stretch;
    }
    .${Classes.any}.${Classes.row} > u:first-of-type.${Classes.alignContainerRight} {
        flex-grow: 1;
    }
    .${Classes.any}.${Classes.row} > s:first-of-type.${Classes.alignContainerCenterX} {
        flex-grow: 1;
    }
    .${Classes.any}.${Classes.row} > s:first-of-type.${Classes.alignContainerCenterX} > .${Classes.alignCenterX} {
        margin-left: auto !important;
    }
    .${Classes.any}.${Classes.row} > s:last-of-type.${Classes.alignContainerCenterX} {
        flex-grow: 1;
    }
    .${Classes.any}.${Classes.row} > s:last-of-type.${Classes.alignContainerCenterX} > .${Classes.alignCenterX} {
        margin-right: auto !important;
    }
    .${Classes.any}.${Classes.row} > s:only-of-type.${Classes.alignContainerCenterX} {
        flex-grow: 1;
    }
    .${Classes.any}.${Classes.row} > s:only-of-type.${Classes.alignContainerCenterX} > .${Classes.alignCenterY} {
        margin-top: auto !important;
        margin-bottom: auto !important;
    }
    .${Classes.any}.${Classes.row} > s:last-of-type.${Classes.alignContainerCenterX} ~ u {
        flex-grow: 0;
    }
    .${Classes.any}.${Classes.row} > u:first-of-type.${Classes.alignContainerRight} ~ s.${Classes.alignContainerCenterX} {
        flex-grow: 0;
    }
    ${describeAlignments_row(".${Classes.any}.${Classes.row}")}
    .${Classes.any}.${Classes.row}.${Classes.spaceEvenly} {
        justify-content: space-between;
    }

    .${Classes.any}.${Classes.column} {
        display: flex;
        flex-direction: column;
    }
    .${Classes.any}.${Classes.column} > .${Classes.heightFill} {
        flex-grow: 100000;
    }
    .${Classes.any}.${Classes.column} > .${Classes.widthFill} {
        width: 100%;
    }
    .${Classes.any}.${Classes.column} > .${Classes.widthFillPortion} {
        width: 100%;
    }
    .${Classes.any}.${Classes.column} > .${Classes.widthContent} {
        align-self: flex-start;
    }
    .${Classes.any}.${Classes.column} > u:first-of-type.${Classes.alignContainerBottom} {
        flex-grow: 1;
    }
    .${Classes.any}.${Classes.column} > u:first-of-type.${Classes.alignContainerCenterY} {
        flex-grow: 1
    }
    .${Classes.any}.${Classes.column} > u:first-of-type.${Classes.alignContainerCenterY} > .${Classes.alignCenterY} {
        margin-top: auto !important;
        margin-bottom: 0 !important;
    }
    .${Classes.any}.${Classes.column} > u:last-of-type.${Classes.alignContainerCenterY} {
        flex-grow: 1;
    }
    .${Classes.any}.${Classes.column} > u:last-of-type.${Classes.alignContainerCenterY} > .${Classes.alignCenterY} {
        margin-bottom: auto !important;
        margin-top: 0 !important;
    }
    .${Classes.any}.${Classes.column} > s:only-of-type.${Classes.alignContainerCenterY} {
        flex-grow: 1;
    }
    .${Classes.any}.${Classes.column} > s:only-of-type.${Classes.alignContainerCenterY} > .${Classes.alignCenterY} {
        margin-top: auto !important;
        margin-bottom: auto !important;
    }
    .${Classes.any}.${Classes.column} > s:last-of-type.${Classes.alignContainerCenterY} ~ u {
        flex-grow: 0;
    }
    .${Classes.any}.${Classes.column} > u:first-of-type.${Classes.alignContainerBottom} ~ s.${Classes.alignContainerCenterY} {
        flex-grow: 0;
    }

    ${describeAlignments_column(".${Classes.any}.${Classes.column}")}

    .${Classes.any}.${Classes.column} > .${Classes.container} {
        flex-grow: 0;
        flex-basis: auto;
        width: 100%;
        align-self: stretch !important;
    }

    .${Classes.any}.${Classes.column}.${Classes.spaceEvenly} {
        justify-content: space-between;
    }

    .${Classes.any}.${Classes.paragraph} {
        display: block;
        white-space: normal;
    }
    .${Classes.any}.${Classes.paragraph} > .${Classes.text} {
        display: inline;
        white-space: normal;
    }
    .${Classes.any}.${Classes.paragraph} > .${Classes.single} {
        display: inline;
        white-space: normal;
    }
    .${Classes.any}.${Classes.paragraph} > .${Classes.row} {
        display: inline-flex;
    }
    .${Classes.any}.${Classes.paragraph} > .${Classes.column} {
        display: inline-flex;
    }
    ${describeAlignments_paragraph(".${Classes.any}.${Classes.paragraph}")}
    .${Classes.any}.${Classes.hidden} {
        display: none;
    }
    .${Classes.any}.${Classes.textJustify} {
        text-align: justify;
    }
    .${Classes.any}.${Classes.textJustifyAll} {
        text-align: justify-all;
    }
    .${Classes.any}.${Classes.textCenter} {
        text-align: center;
    }
    .${Classes.any}.${Classes.textRight} {
        text-align: right;
    }
    .${Classes.any}.${Classes.textLeft} {
        text-align: left;
    }
""".trimIndent() /* lines 1659-1670*/


fun elDescription(d: String) = """
    $d {
        display: flex;
        flex-direction: column;
        white-space: pre;
    }
    $d > .${Classes.heightContent} {
        height: auto;
    }
    $d > .${Classes.heightFill} {
        flex-grow: 100000;
    }
    $d > .${Classes.widthFill} {
        width: 100%;
    }
    $d > .${Classes.widthContent} {
        align-self: flex-start;
    }

    ${describeAlignments_el(d)}
""".trimIndent()


fun rule(content: String, indiv: String): Pair<String, String> =
    Pair(" { $content } ", " { $indiv } ")

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
                "align-items: flex-start;",
                "align-self: flex-start;"
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
                "> .${Classes.any} { margin-top: auto; margin-bottom: auto; }",
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


fun describeAlignments(parentDescriptor: String, values: ((Alignment) -> Pair<String, String>)): String {
    val sb = StringBuilder()
    for (alignment in Alignment.values()) {
        val (content, indiv) = values(alignment)

        sb.append(
        """${'\n'}
            $parentDescriptor.${contentName(alignment)}
            $content
            $parentDescriptor > .${Classes.any}.${selfName(alignment)}
            $indiv
            ${'\n'}
        """.trimIndent()
        )
    }

    return sb.toString()
}

fun selfName(alignment: Alignment): String =
    when (alignment) {
        Alignment.Top -> Classes.alignTop
        Alignment.Bottom -> Classes.alignBottom
        Alignment.Right -> Classes.alignRight
        Alignment.Left -> Classes.alignLeft
        Alignment.CenterX -> Classes.alignCenterX
        Alignment.CenterY -> Classes.alignCenterY
    }

fun contentName(alignment: Alignment): String =
    when (alignment) {
        Alignment.Top -> Classes.contentTop
        Alignment.Bottom -> Classes.contentBottom
        Alignment.Right -> Classes.contentRight
        Alignment.Left -> Classes.contentLeft
        Alignment.CenterX -> Classes.contentCenterX
        Alignment.CenterY -> Classes.contentCenterY
    }

fun spacingName(x: Int, y: Int) = "spacing-$x-$y"

fun floatClass(x: Float) = (x * 255).roundToInt().toString()
