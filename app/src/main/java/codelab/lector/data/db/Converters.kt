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

    /** Una línea por capítulo: inicio, fin y título separados por tabulador. */
    @ColumnTypeConverter
    fun chaptersToText(chapters: ChapterList): String = chapters.items.joinToString("\n") {
        "${it.startMs}\t${it.endMs}\t${it.title.replace('\t', ' ').replace('\n', ' ')}"
    }

    @ColumnTypeConverter
    fun textToChapters(text: String): ChapterList = ChapterList(
        if (text.isEmpty()) emptyList()
        else text.split("\n").map { line ->
            val (start, end, title) = line.split("\t", limit = 3)
            ChapterInfo(start.toLong(), end.toLong(), title)
        },
    )
}
