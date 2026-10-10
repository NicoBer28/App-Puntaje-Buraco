package com.example.puntajeburaco20.domain.repository

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Cuenta
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.PedidoDeReclamo
import com.example.puntajeburaco20.domain.model.PerfilACargo
import com.example.puntajeburaco20.domain.model.Reclamo
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
     * @throws ErrorUsuario.ReclamoPendiente si la cuenta tiene un perfil reservado sin responder.
     */
    suspend fun crear(cuenta: Cuenta, nombre: String): Usuario

    /**
     * Vincula a la [cuenta] el perfil llamado [nombre], que la persona usaba antes de que la app
     * tuviera cuentas con mail. Demuestra que era suyo con la contraseña que tenía entonces.
     * Necesita conexión.
     *
     * @throws ErrorUsuario.UsuarioInexistente si no hay un perfil con ese nombre.
     * @throws ErrorUsuario.PerfilYaVinculado si el perfil ya es de una cuenta.
     * @throws ErrorUsuario.PerfilCreadoPorOtro si lo creó otra persona: nunca tuvo contraseña, y
     *   hay que pedírselo con [pedirPerfil].
     * @throws ErrorUsuario.ContrasenaAnteriorIncorrecta si la contraseña no es la que tenía.
     * @throws ErrorUsuario.ReclamoPendiente si la cuenta tiene un perfil reservado sin responder.
     */
    suspend fun vincularAnterior(cuenta: Cuenta, nombre: String, passwordAnterior: String): Jugador

    /**
     * Crea un perfil sin cuenta de acceso para alguien que no usa la app, ya como amigo de
     * [amigoDe], que es el perfil de [creador]. Queda a cargo de [creador] y reservado para
     * [mail]: cuando esa persona se registre con ese mail, la app se lo va a ofrecer. Todo es una
     * sola operación: nunca queda un perfil suelto que nadie tiene en su lista.
     *
     * @throws ErrorUsuario.NombreEnUso si ya existe un perfil con ese nombre.
     * @throws ErrorUsuario.MailConPerfil si ese mail ya tiene una cuenta o un perfil reservado.
     */
    suspend fun crearAmigoSinLogin(nombre: String, mail: String, creador: Cuenta, amigoDe: Jugador): Jugador

    /** Los perfiles sin cuenta que creó [creador] y que nadie aceptó todavía. Emite de nuevo cuando cambian. */
    fun observarPerfilesACargo(creador: Cuenta): Flow<List<PerfilACargo>>

    /**
     * Reserva [perfil] para [mail], en lugar del mail que tuviera. [nombreCreador] es el nombre
     * de usuario de [creador], que es lo que va a leer la persona cuando se le ofrezca el perfil.
     *
     * @throws ErrorUsuario.MailConPerfil si ese mail ya tiene una cuenta o un perfil reservado.
     */
    suspend fun reservarPara(perfil: PerfilACargo, mail: String, creador: Cuenta, nombreCreador: String)

    /** El perfil que alguien dejó reservado para el mail de la [cuenta], o `null` si no hay ninguno. */
    suspend fun buscarReclamo(cuenta: Cuenta): Reclamo?

    /**
     * Vincula a la [cuenta] el perfil reservado para su mail.
     *
     * @throws ErrorUsuario.ReclamoNoDisponible si ya no hay un perfil reservado para ese mail.
     */
    suspend fun aceptarReclamo(cuenta: Cuenta): Jugador

    /** Descarta el perfil reservado para el mail de la [cuenta]: sigue a cargo de quien lo creó. */
    suspend fun rechazarReclamo(cuenta: Cuenta)

    /**
     * La [cuenta] pide el perfil llamado [nombre], que otra persona creó para alguien que no
     * usaba la app. Queda a la espera de que quien lo creó lo confirme.
     *
     * @throws ErrorUsuario.UsuarioInexistente si no hay un perfil así creado por otra persona.
     * @throws ErrorUsuario.PerfilYaVinculado si el perfil ya es de una cuenta.
     * @throws ErrorUsuario.ReclamoPendiente si la cuenta tiene un perfil reservado sin responder.
     */
    suspend fun pedirPerfil(cuenta: Cuenta, nombre: String): PedidoDeReclamo

    /** El pedido que la [cuenta] tiene sin resolver, o `null`. Consulta al servidor. */
    suspend fun buscarPedidoPropio(cuenta: Cuenta): PedidoDeReclamo?

    /** Sigue el pedido de la [cuenta]: emite `null` cuando deja de existir, lo acepten o lo rechacen. */
    fun observarPedidoPropio(cuenta: Cuenta): Flow<PedidoDeReclamo?>

    /** La [cuenta] desiste de su pedido. */
    suspend fun cancelarPedido(cuenta: Cuenta)

    /** Los pedidos sin resolver sobre perfiles que creó [creador]. Emite de nuevo cuando cambian. */
    fun observarPedidosRecibidos(creador: Cuenta): Flow<List<PedidoDeReclamo>>

    /**
     * [creador] confirma que quien hizo el [pedido] es el dueño del perfil: lo deja reservado
     * para el mail del pedido, en lugar del que tuviera, y esa persona ya puede aceptarlo.
     *
     * @throws ErrorUsuario.PerfilYaVinculado si entretanto el perfil pasó a ser de una cuenta.
     * @throws ErrorUsuario.MailConPerfil si ese mail ya tiene una cuenta o un perfil reservado.
     */
    suspend fun aceptarPedido(pedido: PedidoDeReclamo, creador: Cuenta, nombreCreador: String)

    /** Quien creó el perfil dice que quien hizo el [pedido] no es su dueño. */
    suspend fun rechazarPedido(pedido: PedidoDeReclamo)

    /**
     * Cambia el nombre de usuario de [perfil], que tiene que ser el propio o uno sin cuenta que
     * haya creado quien hace el pedido. Reserva el nombre nuevo y libera el anterior en una sola
     * operación, y después lo corrige en la lista de cada amigo. Las partidas ya jugadas conservan
     * el nombre que tenía.
     *
     * @return el perfil con su nombre nuevo.
     * @throws ErrorUsuario.NombreEnUso si ya existe otro perfil con ese nombre.
     */
    suspend fun renombrar(perfil: Jugador, nombreNuevo: String): Jugador

    /**
     * Borra un perfil sin cuenta que [creador] creó para otra persona, con sus amistades, su
     * nombre reservado, la reserva de su mail y los pedidos que le hayan hecho. Las partidas que
     * jugó no se borran: siguen en el historial de los demás jugadores.
     */
    suspend fun borrarPerfilACargo(perfil: Jugador, creador: Cuenta)

    /**
     * Borra el perfil de la [cuenta] con sus amistades, y deja libres su nombre y su mail. La
     * cuenta de acceso se elimina aparte. Las partidas que jugó no se borran.
     */
    suspend fun borrarPerfilPropio(perfil: Jugador, cuenta: Cuenta)

    /** Registra la amistad en ambos sentidos. */
    suspend fun agregarAmistad(usuario: Jugador, amigo: Jugador)

    /** Elimina la amistad en ambos sentidos. */
    suspend fun eliminarAmistad(usuario: Jugador, amigo: Jugador)
}
