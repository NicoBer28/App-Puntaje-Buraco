package com.example.puntajeburaco20.domain.repository

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Cuenta
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

/** Acceso a los perfiles de los jugadores, independiente de dónde estén guardados. */
interface UsuarioRepository {

    /** Emite el usuario cada vez que cambia, o `null` si no existe. */
    fun observar(id: String): Flow<Usuario?>

    suspend fun obtener(id: String): Usuario?

    /** Busca por nombre de usuario, sin distinguir mayúsculas. */
    suspend fun buscarPorNombre(nombre: String): Usuario?

    /**
     * Id del perfil vinculado a la cuenta [uid], o `null` si todavía no tiene uno. Emite de nuevo
     * cuando cambia. No emite hasta saberlo con certeza (por ejemplo, sin conexión ni copia local).
     */
    fun observarIdDeCuenta(uid: String): Flow<String?>

    /**
     * Crea el perfil de la [cuenta] con ese nombre de usuario. Verificar que el nombre esté libre,
     * reservarlo y crear el perfil es una sola operación atómica: dos registros simultáneos con el
     * mismo nombre no se pisan.
     *
     * @throws ErrorUsuario.NombreEnUso si ya existe un perfil con ese nombre.
     */
    suspend fun crear(cuenta: Cuenta, nombre: String): Usuario

    /**
     * Crea un perfil sin cuenta de acceso para alguien que no usa la app. Queda a cargo de
     * [creador] hasta que esa persona lo reclame.
     *
     * @throws ErrorUsuario.NombreEnUso si ya existe un perfil con ese nombre.
     */
    suspend fun crearSinLogin(nombre: String, creador: Cuenta): Usuario

    /** Registra la amistad en ambos sentidos. */
    suspend fun agregarAmistad(usuario: Jugador, amigo: Jugador)

    /** Elimina la amistad en ambos sentidos. */
    suspend fun eliminarAmistad(usuario: Jugador, amigo: Jugador)
}
