package net.namekdev.entity_tracker.ui


typealias Stylesheet = MutableMap<String, Style>
typealias Color = Rgba

data class Rgba(val r: Float, val g: Float, val b: Float, val a: Float)
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
    // TODO Colored
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
