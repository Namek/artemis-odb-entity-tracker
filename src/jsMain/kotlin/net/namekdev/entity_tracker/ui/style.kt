package net.namekdev.entity_tracker.ui


typealias Stylesheet = MutableMap<String, Style>
typealias Color = Rgba

data class Rgba(val r: Short, val g: Short, val b: Short, val a: Short) {
    fun formatWithDashes(): String = "$r-$g-$b-$a"
    fun format(): String = "rgba($r,$g,$b,${a / 255f})"
}
fun hexToColor(rgb: Int, alpha: Short = 255.toShort()): Rgba =
    Rgba(((rgb shr 16) and 0xFF).toShort(), ((rgb shr 8) and 0xFF).toShort(), (rgb and 0xFF).toShort(), alpha)


data class OptionRecord(val hover: HoverSetting, val focus: FocusStyle)

enum class HoverSetting { NoHover, AllowHover, ForceHover }
data class FocusStyle(
    val borderColor: Color? = null,
    val shadow : Shadow? = null,
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

internal fun renderStyleRule(sb: StringBuilder, options: OptionRecord, rule: Style, maybePseudo: PseudoClass?) = when(rule) {
    is AStyle -> {
        renderStyle(sb, maybePseudo, options, rule.selector, rule.props)
    }
    // TODO Shadows, Transparency, FontSize, FontFamily
    is Single -> {
        renderStyle(sb, maybePseudo, options, ".${rule.klass}", arrayOf(Pair(rule.prop, rule.value)))
    }
    is Colored -> {
        renderStyle(sb, maybePseudo, options, ".${rule.cls}", arrayOf(Pair(rule.prop, rule.color.format())))
    }
    is SpacingStyle -> TODO()
    is PaddingStyle -> {
        val padding = "${rule.top}px ${rule.right}px ${rule.bottom}px ${rule.left}px"
        renderStyle(sb, maybePseudo, options, ".${rule.cls}", arrayOf(Pair("padding", padding)))
    }
    // TODO BorderWidth, PseudoSelector, Transform(?)
}

internal fun renderStyle(sb: StringBuilder, maybePseudo: PseudoClass?, options: OptionRecord, selector: String, props: Array<Pair<String, String>>) {
    if (maybePseudo == null) {
        sb.append(selector, "{")
        renderProps(sb, false, props)
        sb.append("\n}")
    }
    else {
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

        sb.append("""
            $parentDescriptor .${contentName(alignment)}
                $content

            $parentDescriptor > .${Classes.any} .${selfName(alignment)}
                $indiv
        """.trimIndent())
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