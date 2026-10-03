package codelab.lector.data.db

import androidx.room3.ColumnTypeConverter

class Converters {
    @ColumnTypeConverter
    fun floatsToText(values: List<Float>?): String? = values?.joinToString(",")

    @ColumnTypeConverter
    fun textToFloats(text: String?): List<Float>? =
        text?.let { if (it.isEmpty()) emptyList() else it.split(",").map(String::toFloat) }

    @ColumnTypeConverter
    fun stringsToText(values: List<String>): String = values.joinToString("\n")

    @ColumnTypeConverter
    fun textToStrings(text: String): List<String> =
        if (text.isEmpty()) emptyList() else text.split("\n")
}
