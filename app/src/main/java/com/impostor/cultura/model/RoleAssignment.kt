package com.impostor.cultura.model

enum class ImpostorMode {
    FAKE_WORD,
    NO_WORD
}

/**
 * Rol asignado a un jugador en una ronda.
 * @param isImpostor verdadero si a este jugador le tocó ser el impostor.
 * @param word la palabra que debe ver (la real, o la falsa si es impostor en modo palabra falsa).
 *             Es null cuando el impostor no recibe palabra.
 * @param mode modo de la ronda.
 */
data class RoleAssignment(
    val isImpostor: Boolean,
    val word: String?,
    val mode: ImpostorMode
)
