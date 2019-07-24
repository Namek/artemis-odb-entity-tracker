package net.namekdev.entity_tracker.ui

import net.namekdev.entity_tracker.utils.serialization.DataType


fun dataTypeToIcon(dt: DataType, isPrimitive: Boolean): RNode {
    val text = when (dt) {
        DataType.Boolean ->
            if (isPrimitive) "⅟" else "፧"
        DataType.Byte ->
            if (isPrimitive) "b" else "B"
        DataType.Short ->
            if (isPrimitive) "s" else "S"
        DataType.Int ->
            if (isPrimitive) "i" else "I"
        DataType.Long ->
            if (isPrimitive) "l" else "L"
        DataType.Float ->
            if (isPrimitive) "f" else "F"
        DataType.Double ->
            if (isPrimitive) "d" else "D"
        DataType.String ->
            "\uD83D\uDDB9"
        DataType.Enum ->
            "e"
        DataType.Object ->
            "O"
        else ->
            "※"
    }

    return column(attrs(width(px(28))), text("($text) "))
}

fun InputValue.extractInputValue(dataType: DataType): Any? =
    when (this) {
        is InputValueText -> text
        is InputValueFloatingPoint -> {
            when (dataType) {
                DataType.Float -> number.toFloat()
                DataType.Double -> number.toDouble()
                else ->
                    throw RuntimeException("textEdit() floating point should be only Float/Double and it is: ${dataType}")
            }
        }
        is InputValueInteger -> {
            when (dataType) {
                DataType.Byte -> number.toByte()
                DataType.Short -> number.toShort()
                DataType.Int -> number.toInt()
                DataType.Long -> number.toLong()
                else ->
                    throw RuntimeException("integer was supposed to be Byte/Short/Int/Long and it is: ${dataType}")
            }
        }
    }

fun convertDataTypeToInputType(dataType: DataType): InputType =
    when (dataType) {
        DataType.Byte,
        DataType.Short,
        DataType.Int,
        DataType.Long ->
            InputType.Integer

        DataType.Float,
        DataType.Double ->
            InputType.FloatingPointNumber

        else -> InputType.Text
    }