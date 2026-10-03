package com.example.puntajeburaco20.data.firestore

/**
 * Nombres de colecciones y campos en Firestore. Son los mismos que usaba la versión anterior de
 * la app, por lo que los datos existentes siguen siendo compatibles.
 *
 * ```
 * users/{idUsuario}                                 Nombre, Password, Amigos, AmigosNombre,
 *                                                   Partidas Jugadas, Partidas Ganadas
 * users/{idUsuario}/statistics/{idRival}            Partidas Jugadas, Partidas Ganadas
 * doubles/{idPareja}                                Partidas Jugadas, Partidas Ganadas
 * doubles/{idPareja}/statisticsDoubles/{idRival}    Partidas Jugadas, Partidas Ganadas
 * ```
 */
internal object EsquemaFirestore {
    const val USUARIOS = "users"
    const val ESTADISTICAS_INDIVIDUALES = "statistics"
    const val PAREJAS = "doubles"
    const val ESTADISTICAS_PAREJAS = "statisticsDoubles"

    const val NOMBRE = "Nombre"
    const val PASSWORD = "Password"
    const val AMIGOS_IDS = "Amigos"
    const val AMIGOS_NOMBRES = "AmigosNombre"
    const val PARTIDAS_JUGADAS = "Partidas Jugadas"
    const val PARTIDAS_GANADAS = "Partidas Ganadas"
}
