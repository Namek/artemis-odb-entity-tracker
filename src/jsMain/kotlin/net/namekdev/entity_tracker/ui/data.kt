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

fun extractInputValue(value: InputValue?, dataType: DataType): Any? =
    when (value) {
        null -> null
        is InputValueText -> value.text
        is InputValueFloatingPoint -> {
            when (dataType) {
                DataType.Float -> value.number.toFloat()
                DataType.Double -> value.number.toDouble()
                else ->
                    throw RuntimeException("textEdit() floating point should be only Float/Double and it is: ${dataType}")
            }
        }
        is InputValueInteger -> {
            when (dataType) {
                DataType.Byte -> value.number.toByte()
                DataType.Short -> value.number.toShort()
                DataType.Int -> value.number.toInt()
                DataType.Long -> value.number.toLong()
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