package com.example.puntajeburaco20.domain.repository

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Cuenta
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

/** Acceso a los perfiles de los jugadores, independiente de dónde estén guardados. */
interface UsuarioRepository {

    /**
     * Emite el usuario, con sus amigos, cada vez que cambia; o `null` si no existe. La lista de
     * amigos es privada: solo se puede pedir el perfil propio.
     */
    fun observar(id: String): Flow<Usuario?>

    /** El usuario con sus amigos. Como [observar], solo sirve para el perfil propio. */
    suspend fun obtener(id: String): Usuario?

    /** Busca a cualquier jugador por su nombre de usuario, sin distinguir mayúsculas. */
    suspend fun buscarPorNombre(nombre: String): Jugador?

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
     * Crea un perfil sin cuenta de acceso para alguien que no usa la app, ya como amigo de
     * [amigoDe], que es el perfil de [creador]. Queda a cargo de [creador] hasta que esa persona
     * lo reclame. Todo es una sola operación: nunca queda un perfil suelto que nadie tiene en
     * su lista.
     *
     * @throws ErrorUsuario.NombreEnUso si ya existe un perfil con ese nombre.
     */
    suspend fun crearAmigoSinLogin(nombre: String, creador: Cuenta, amigoDe: Jugador): Jugador

    /** Registra la amistad en ambos sentidos. */
    suspend fun agregarAmistad(usuario: Jugador, amigo: Jugador)

    /** Elimina la amistad en ambos sentidos. */
    suspend fun eliminarAmistad(usuario: Jugador, amigo: Jugador)
}
