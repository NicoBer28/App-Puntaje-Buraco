package com.example.puntajeburaco20.ui.common

import kotlin.coroutines.cancellation.CancellationException

/** Como [runCatching], pero deja pasar la cancelación de la corrutina en lugar de capturarla. */
inline fun <T> intentar(bloque: () -> T): Result<T> =
    try {
        Result.success(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
