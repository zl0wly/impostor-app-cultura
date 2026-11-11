package com.impostor.cultura.data

import android.content.Context
import com.impostor.cultura.model.Category

/**
 * Lector simple de CSV con formato: categoria,palabra
 * (una fila por palabra). La primera fila se ignora si empieza con "categoria".
 */
object CsvWordLoader {

    fun loadFromAssets(context: Context): List<Category> {
        return try {
            context.assets.open("palabras.csv").bufferedReader(Charsets.UTF_8).use { reader ->
                parse(reader.readLines())
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parse(lines: List<String>): List<Category> {
        val map = LinkedHashMap<String, MutableList<String>>()
        var first = true
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (first) {
                first = false
                if (line.lowercase().startsWith("categoria")) continue
            }
            val parts = line.split(",").map { it.trim() }
            if (parts.size >= 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                map.getOrPut(parts[0]) { mutableListOf() }.add(parts[1])
            }
        }
        return map.map { (category, words) -> Category(category, words) }
    }
}
