package com.impostor.cultura.data

import android.content.Context
import com.impostor.cultura.model.Category

/**
 * Repositorio de palabras. Si existe `assets/palabras.csv` (formato categoria,palabra)
 * se usa ese archivo; si no, se usan las palabras de ejemplo incorporadas.
 */
object WordData {

    private val defaultCategories: List<Category> = listOf(
        Category(
            "Bailes típicos",
            listOf("Joropo", "Gaita", "Tambor", "Calipso", "Danza")
        ),
        Category(
            "Instrumentos musicales",
            listOf("Cuatro", "Bandola", "Maracas", "Furruco", "Arpa")
        ),
        Category(
            "Fiestas religiosas",
            listOf(
                "Fiesta de San Juan",
                "Fiesta de San Pedro",
                "Divina Pastora",
                "Virgen de Coromoto",
                "Feria de la Chinita"
            )
        ),
        Category(
            "Gastronomía",
            listOf("Arepa", "Cachapa", "Hallaca", "Pabellón criollo", "Asado negro")
        ),
        Category(
            "Dichos",
            listOf(
                "Camarón que se duerme se lo lleva la corriente",
                "Más vale tarde que nunca",
                "El que con lobos anda a aullar se enseña",
                "A quien madruga Dios le ayuda",
                "Dime con quién andas y te diré quién eres"
            )
        ),
        Category(
            "Mitos y leyendas",
            listOf("La Sayona", "La Llorona", "El Silbón", "El Carretón", "María Lionza")
        )
    )

    @Volatile
    private var categories: List<Category> = defaultCategories

    fun load(context: Context) {
        val fromCsv = CsvWordLoader.loadFromAssets(context)
        if (fromCsv.isNotEmpty()) {
            categories = fromCsv
        }
    }

    fun all(): List<Category> = categories

    fun names(): List<String> = categories.map { it.name }

    fun get(name: String): Category? = categories.firstOrNull { it.name == name }
}
