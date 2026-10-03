package com.example.puntajeburaco20.domain.repository

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

/** Acceso a las cuentas de usuario, independiente de dónde estén guardadas. */
interface UsuarioRepository {

    /** Emite el usuario cada vez que cambia, o `null` si no existe. */
    fun observar(id: String): Flow<Usuario?>

    suspend fun obtener(id: String): Usuario?

    /**
     * Devuelve el usuario si las credenciales son correctas.
     *
     * @throws ErrorUsuario.UsuarioInexistente si no hay una cuenta con ese nombre.
     * @throws ErrorUsuario.ContrasenaIncorrecta si la contraseña no coincide.
     */
    suspend fun autenticar(nombre: String, password: String): Usuario

    /**
     * Crea una cuenta nueva sin amigos. Verificar que el nombre esté libre y crearla es una sola
     * operación atómica: dos registros simultáneos con el mismo nombre no se pisan.
     *
     * @throws ErrorUsuario.NombreEnUso si ya existe una cuenta con ese nombre.
     */
    suspend fun crear(nombre: String, password: String): Usuario

    /** Registra la amistad en ambos sentidos. */
    suspend fun agregarAmistad(usuario: Jugador, amigo: Jugador)

    /** Elimina la amistad en ambos sentidos. */
    suspend fun eliminarAmistad(usuario: Jugador, amigo: Jugador)
}
