package net.namekdev.entity_tracker.ui

import snabbdom.modules.On


sealed class Style
class AStyle(val selector: String, val props: Array<Pair<String, String>>) : Style()
class Single(val klass: String, val prop: String, val value: String) : Style()
class SpacingStyle(val cls: String, val x: Int, val y: Int) : Style()
class PaddingStyle(val cls: String, val top: Int, val right: Int, val bottom: Int, val left: Int) : Style()
class Colored(val cls: String, val prop: String, val color: Color) : Style()
class Transparency(val name: String, val alpha: Float) : Style()
class BorderWidth(val cls: String, val top: Int, val right: Int, val bottom: Int, val left: Int) : Style()
class PseudoSelector(val cls: PseudoClass, val styles: Array<Style>) : Style()
class FontSize(val size: Int) : Style()
class Transform(val transformation: Transformation) : Style()
// TODO fontfamily, Shadows


// Width -> Px px, Content, Fill portion, Min size (Width), Max size (Width)
// Height -> Px px, Content, Fill portion
// AlignX _/Right/CenterX
// AlignY _/Bottom/CenterY


sealed class Attribute {
    object NoAttribute : Attribute()
    class Events(val on: On) : Attribute()

    class Class(val flag: Int, val exactClassName: String) : Attribute()
    class StyleClass(val flag: Int, val style: Style) : Attribute()

    sealed class Width : Attribute() {
        data class Px(val px: Int) : Width()
        object Content : Width()
        data class Fill(val portion: Int) : Width()
        data class Min(val size: Int, val width: Width) : Width()
        data class Max(val size: Int, val width: Width) : Width()
    }

    sealed class Height : Attribute() {
        data class Px(val px: Int) : Height()
        object Content : Height()
        data class Fill(val portion: Int) : Height()
        data class Min(val size: Int, val height: Height) : Height()
        data class Max(val size: Int, val height: Height) : Height()
    }

    class AlignX(val hAlign: HAlign) : Attribute()
    class AlignY(val vAlign: VAlign) : Attribute()
}

class SizingRender(
    val flags: Int,
    val classes: String,
    val styles: Array<Style>? = null)


enum class Alignment {
    Top, Bottom, Right, Left, CenterX, CenterY
}

enum class HAlign {
    Left, CenterX, Right
}

enum class VAlign {
    Top, CenterY, Bottom
}

sealed class Length {
    data class Px(val px: Int) : Length()
    object Content : Length()
    data class Fill(val portion: Int): Length()
    data class Min(val length: Length) : Length()
    data class Max(val length: Length) : Length()
}

sealed class Transformation
object Untransformed : Transformation()
class Moved(val xyz: XYZ) : Transformation()
class FullTransform(val translate: XYZ, val scale: XYZ, val rotate: XYZ, val angle: Angle) : Transformation()

data class XYZ(val x: Float, val y: Float, val z: Float)
typealias Angle = Float