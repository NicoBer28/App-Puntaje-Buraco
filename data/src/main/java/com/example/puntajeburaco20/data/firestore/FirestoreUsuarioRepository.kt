package com.example.puntajeburaco20.data.firestore

import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.AMIGOS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.CREADO_POR
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.CUENTAS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.CREADOR
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.DESDE
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.FECHA
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.MAIL
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.MAILS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.NOMBRE
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.NOMBRES
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.NOMBRE_CREADOR
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.PEDIDOS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.PERFIL
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.PERFILES
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.PRUEBA_CLAVE_VIEJA
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.UID
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.pruebaDeClaveVieja
import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Cuenta
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.PedidoDeReclamo
import com.example.puntajeburaco20.domain.model.PerfilACargo
import com.example.puntajeburaco20.domain.model.Reclamo
import com.example.puntajeburaco20.domain.model.Usuario
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Perfiles y amistades en el esquema nuevo (ver [EsquemaFirestore]). */
@Singleton
class FirestoreUsuarioRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val sincronizacion: FirestoreSincronizacionRepository,
) : UsuarioRepository {

    private fun perfil(id: String) = db.collection(PERFILES).document(id)

    private fun amigos(idPerfil: String) = perfil(idPerfil).collection(AMIGOS)

    private fun reservaDe(nombre: String) = db.collection(NOMBRES).document(Usuario.claveDeNombre(nombre))

    /** El documento de un mail: el registro de la cuenta que lo tiene, o un reclamo. */
    private fun registroDe(mail: String) = db.collection(MAILS).document(mail)

    /** El pedido de una cuenta: tiene a lo sumo uno, con su uid como id. */
    private fun pedidoDe(uid: String) = db.collection(PEDIDOS).document(uid)

    override fun observar(id: String): Flow<Usuario?> =
        combine(perfil(id).snapshots(), amigos(id).snapshots()) { documento, amistades ->
            documento.aUsuario(amistades)
        }

    override suspend fun obtener(id: String): Usuario? {
        val documento = perfil(id).get().await()
        if (!documento.exists()) return null
        return documento.aUsuario(amigos(id).get().await())
    }

    override suspend fun buscarPorNombre(nombre: String): Jugador? {
        val idPerfil = reservaDe(nombre).get().await().getString(PERFIL) ?: return null
        return perfil(idPerfil).get().await().aJugador()
    }

    override fun observarIdDeCuenta(uid: String): Flow<String?> =
        db.collection(CUENTAS).document(uid).confirmados().map { it.getString(PERFIL) }

    /**
     * El perfil, el nombre, la cuenta y el mail se escriben juntos porque las reglas de seguridad
     * exigen que sean coherentes entre sí.
     */
    override suspend fun crear(cuenta: Cuenta, nombre: String): Usuario = conConexion {
        exigirSinReclamo(cuenta)
        val referencia = db.collection(PERFILES).document()
        reservandoNombre(nombre) { transaccion ->
            val apuntaAlPerfil = mapOf(PERFIL to referencia.id)
            transaccion.set(referencia, mapOf(NOMBRE to nombre, UID to cuenta.uid))
            transaccion.set(reservaDe(nombre), apuntaAlPerfil)
            transaccion.set(db.collection(CUENTAS).document(cuenta.uid), apuntaAlPerfil)
            transaccion.set(registroDe(cuenta.mail), apuntaAlPerfil + (UID to cuenta.uid))
        }
        Usuario(Jugador(referencia.id, nombre), amigos = emptyList())
    }

    /**
     * El perfil pasa a tener dueño, y la cuenta y el mail quedan apuntando a él, todo junto como
     * en [crear]. La cuenta lleva además la prueba de la contraseña anterior: las reglas de
     * seguridad la comparan con la credencial que dejó la migración y, si no coincide, rechazan
     * la operación completa.
     */
    override suspend fun vincularAnterior(cuenta: Cuenta, nombre: String, passwordAnterior: String): Jugador =
        try {
            exigirSinReclamo(cuenta)
            db.runTransaction<Result<Jugador>> { transaccion ->
                val idPerfil = transaccion.get(reservaDe(nombre)).getString(PERFIL)
                    ?: return@runTransaction Result.failure(ErrorUsuario.UsuarioInexistente)
                val documento = transaccion.get(perfil(idPerfil))
                val jugador = documento.aJugador()
                    ?: return@runTransaction Result.failure(ErrorUsuario.UsuarioInexistente)
                if (documento.contains(UID)) return@runTransaction Result.failure(ErrorUsuario.PerfilYaVinculado)
                // Un perfil que alguien creó para otro nunca tuvo contraseña.
                if (documento.contains(CREADO_POR)) return@runTransaction Result.failure(ErrorUsuario.PerfilCreadoPorOtro)

                val prueba = pruebaDeClaveVieja(idPerfil, passwordAnterior)
                transaccion.vincular(idPerfil, cuenta, datosDeCuenta = mapOf(PRUEBA_CLAVE_VIEJA to prueba))
                Result.success(jugador)
            }.await().getOrThrow()
        } catch (e: FirebaseFirestoreException) {
            throw when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> ErrorUsuario.ContrasenaAnteriorIncorrecta
                FirebaseFirestoreException.Code.UNAVAILABLE -> ErrorUsuario.SinConexion
                else -> e
            }
        }

    /**
     * El perfil nace ya con la amistad, porque las reglas no aceptan uno que nadie tenga en su
     * lista, y con el reclamo que lo deja reservado para el [mail].
     */
    override suspend fun crearAmigoSinLogin(nombre: String, mail: String, creador: Cuenta, amigoDe: Jugador): Jugador =
        conConexion {
            exigirMailLibre(mail)
            val referencia = db.collection(PERFILES).document()
            val nuevo = Jugador(referencia.id, nombre)
            reservandoNombre(nombre) { transaccion ->
                transaccion.set(referencia, mapOf(NOMBRE to nombre, CREADO_POR to creador.uid))
                transaccion.set(reservaDe(nombre), mapOf(PERFIL to referencia.id))
                transaccion.set(amigos(nuevo.id).document(amigoDe.id), amistadCon(amigoDe))
                transaccion.set(amigos(amigoDe.id).document(nuevo.id), amistadCon(nuevo))
                transaccion.set(registroDe(mail), reclamoDe(nuevo.id, creador, amigoDe.nombre))
            }
            nuevo
        }

    override fun observarPerfilesACargo(creador: Cuenta): Flow<List<PerfilACargo>> = combine(
        db.collection(PERFILES).whereEqualTo(CREADO_POR, creador.uid).snapshots(),
        db.collection(MAILS).whereEqualTo(CREADO_POR, creador.uid).snapshots(),
    ) { perfiles, reclamos ->
        val mailDe = reclamos.documents.associate { it.getString(PERFIL) to it.id }
        perfiles.documents
            // Los que ya tienen dueño dejaron de estar a cargo de quien los creó.
            .filterNot { it.contains(UID) }
            .mapNotNull { documento -> documento.aJugador()?.let { PerfilACargo(it, mailDe[documento.id]) } }
            .sortedBy { it.jugador.nombre.lowercase(Locale.ROOT) }
    }

    /** El id de un reclamo es el mail: corregirlo es borrar el reclamo y crear otro, a la vez. */
    override suspend fun reservarPara(perfil: PerfilACargo, mail: String, creador: Cuenta, nombreCreador: String) {
        conConexion {
            exigirMailLibre(mail)
            db.batch().apply {
                perfil.mail?.let { delete(registroDe(it)) }
                set(registroDe(mail), reclamoDe(perfil.jugador.id, creador, nombreCreador))
            }.commit().await()
        }
    }

    override suspend fun buscarReclamo(cuenta: Cuenta): Reclamo? = conConexion {
        val reclamo = registroDe(cuenta.mail).get(Source.SERVER).await()
        val idPerfil = reclamo.getString(PERFIL)?.takeUnless { reclamo.contains(UID) } ?: return null
        val documento = perfil(idPerfil).get(Source.SERVER).await()
        val jugador = documento.aJugador()?.takeUnless { documento.contains(UID) }
        if (jugador == null) {
            // Quedó apuntando a un perfil que ya no se puede aceptar: mientras exista, la cuenta
            // tampoco podría crear un perfil nuevo.
            reclamo.reference.delete().await()
            return null
        }
        Reclamo(jugador, nombreCreador = reclamo.getString(NOMBRE_CREADOR).orEmpty())
    }

    /** El reclamo pasa a ser el registro de la cuenta, en la misma operación que vincula el perfil. */
    override suspend fun aceptarReclamo(cuenta: Cuenta): Jugador = conConexion {
        db.runTransaction<Result<Jugador>> { transaccion ->
            val noDisponible = Result.failure<Jugador>(ErrorUsuario.ReclamoNoDisponible)
            val reclamo = transaccion.get(registroDe(cuenta.mail))
            val idPerfil = reclamo.getString(PERFIL)?.takeUnless { reclamo.contains(UID) }
                ?: return@runTransaction noDisponible
            val documento = transaccion.get(perfil(idPerfil))
            val jugador = documento.aJugador()?.takeUnless { documento.contains(UID) }
                ?: return@runTransaction noDisponible
            transaccion.vincular(idPerfil, cuenta)
            Result.success(jugador)
        }.await().getOrThrow()
    }

    override suspend fun rechazarReclamo(cuenta: Cuenta) {
        conConexion { registroDe(cuenta.mail).delete().await() }
    }

    override suspend fun pedirPerfil(cuenta: Cuenta, nombre: String): PedidoDeReclamo = conConexion {
        exigirSinReclamo(cuenta)
        val idPerfil = reservaDe(nombre).get(Source.SERVER).await().getString(PERFIL)
            ?: throw ErrorUsuario.UsuarioInexistente
        val documento = perfil(idPerfil).get(Source.SERVER).await()
        val jugador = documento.aJugador() ?: throw ErrorUsuario.UsuarioInexistente
        if (documento.contains(UID)) throw ErrorUsuario.PerfilYaVinculado
        val creador = documento.getString(CREADO_POR) ?: throw ErrorUsuario.UsuarioInexistente

        // El nombre y el creador del perfil se copian para mostrar el pedido sin más lecturas.
        val datos = mapOf(
            PERFIL to idPerfil,
            NOMBRE to jugador.nombre,
            MAIL to cuenta.mail,
            CREADOR to creador,
            FECHA to FieldValue.serverTimestamp(),
        )
        pedidoDe(cuenta.uid).set(datos).await()
        PedidoDeReclamo(cuenta.uid, cuenta.mail, jugador)
    }

    override suspend fun buscarPedidoPropio(cuenta: Cuenta): PedidoDeReclamo? =
        conConexion { pedidoDe(cuenta.uid).get(Source.SERVER).await().aPedido() }

    override fun observarPedidoPropio(cuenta: Cuenta): Flow<PedidoDeReclamo?> =
        pedidoDe(cuenta.uid).confirmados().map { it.aPedido() }

    override suspend fun cancelarPedido(cuenta: Cuenta) {
        conConexion { pedidoDe(cuenta.uid).delete().await() }
    }

    override fun observarPedidosRecibidos(creador: Cuenta): Flow<List<PedidoDeReclamo>> =
        db.collection(PEDIDOS).whereEqualTo(CREADOR, creador.uid).snapshots()
            .map { pedidos -> pedidos.documents.mapNotNull { it.aPedido() }.sortedBy { it.mail } }

    /**
     * Aceptar es reservar el perfil para el mail del pedido, como en [reservarPara], y borrar el
     * pedido en la misma operación.
     */
    override suspend fun aceptarPedido(pedido: PedidoDeReclamo, creador: Cuenta, nombreCreador: String) {
        conConexion {
            if (perfil(pedido.perfil.id).get(Source.SERVER).await().contains(UID)) {
                // Otra cuenta ya se quedó con el perfil: el pedido no tiene sentido.
                pedidoDe(pedido.uid).delete().await()
                throw ErrorUsuario.PerfilYaVinculado
            }
            val reservas = db.collection(MAILS)
                .whereEqualTo(CREADO_POR, creador.uid)
                .whereEqualTo(PERFIL, pedido.perfil.id)
                .get(Source.SERVER)
                .await()
                .documents
            val yaReservado = reservas.any { it.id == pedido.mail }
            if (!yaReservado) exigirMailLibre(pedido.mail)
            db.batch().apply {
                reservas.filter { it.id != pedido.mail }.forEach { delete(it.reference) }
                if (!yaReservado) set(registroDe(pedido.mail), reclamoDe(pedido.perfil.id, creador, nombreCreador))
                delete(pedidoDe(pedido.uid))
            }.commit().await()
        }
    }

    override suspend fun rechazarPedido(pedido: PedidoDeReclamo) {
        conConexion { pedidoDe(pedido.uid).delete().await() }
    }

    /**
     * El perfil, la reserva nueva y la anterior cambian juntos: las reglas no aceptan que un
     * perfil se quede con dos nombres ni con ninguno. La copia del nombre que guarda cada amigo
     * se corrige después, de a pocas.
     */
    override suspend fun renombrar(perfil: Jugador, nombreNuevo: String): Jugador = conConexion {
        val nombreActual = nombreActualDe(perfil)
        if (Usuario.claveDeNombre(nombreNuevo) == Usuario.claveDeNombre(nombreActual)) {
            // Solo cambian mayúsculas: la reserva es la misma.
            perfil(perfil.id).update(NOMBRE, nombreNuevo).await()
        } else {
            reservandoNombre(nombreNuevo) { transaccion ->
                transaccion.update(perfil(perfil.id), NOMBRE, nombreNuevo)
                transaccion.delete(reservaDe(nombreActual))
                transaccion.set(reservaDe(nombreNuevo), mapOf(PERFIL to perfil.id))
            }
        }
        for (grupo in amigos(perfil.id).get(Source.SERVER).await().documents.chunked(AMISTADES_POR_LOTE)) {
            db.batch().apply {
                grupo.forEach { amigo -> update(amigos(amigo.id).document(perfil.id), NOMBRE, nombreNuevo) }
            }.commit().await()
        }
        perfil.copy(nombre = nombreNuevo)
    }

    override suspend fun borrarPerfilACargo(perfil: Jugador, creador: Cuenta) {
        conConexion {
            val nombreActual = nombreActualDe(perfil)
            quitarAmistadesDe(perfil.id)
            val suyos = { coleccion: String, campoCreador: String ->
                db.collection(coleccion).whereEqualTo(campoCreador, creador.uid).whereEqualTo(PERFIL, perfil.id)
            }
            val reservas = suyos(MAILS, CREADO_POR).get(Source.SERVER).await().documents
            val pedidos = suyos(PEDIDOS, CREADOR).get(Source.SERVER).await().documents
            db.batch().apply {
                (reservas + pedidos).forEach { delete(it.reference) }
                delete(perfil(perfil.id))
                delete(reservaDe(nombreActual))
            }.commit().await()
        }
    }

    /** El perfil, su nombre, la cuenta y el mail se borran juntos: las reglas no aceptan menos. */
    override suspend fun borrarPerfilPropio(perfil: Jugador, cuenta: Cuenta) {
        conConexion {
            val nombreActual = nombreActualDe(perfil)
            quitarAmistadesDe(perfil.id)
            db.batch().apply {
                delete(perfil(perfil.id))
                delete(reservaDe(nombreActual))
                delete(db.collection(CUENTAS).document(cuenta.uid))
                delete(registroDe(cuenta.mail))
            }.commit().await()
        }
    }

    /** Los dos lados van en el mismo lote: las reglas rechazan una amistad a medias. */
    override suspend fun agregarAmistad(usuario: Jugador, amigo: Jugador) {
        sincronizacion.enviar(
            db.batch().apply {
                set(amigos(usuario.id).document(amigo.id), amistadCon(amigo))
                set(amigos(amigo.id).document(usuario.id), amistadCon(usuario))
            },
        )
    }

    override suspend fun eliminarAmistad(usuario: Jugador, amigo: Jugador) {
        sincronizacion.enviar(
            db.batch().apply {
                delete(amigos(usuario.id).document(amigo.id))
                delete(amigos(amigo.id).document(usuario.id))
            },
        )
    }

    /**
     * Ejecuta [escrituras] solo si el nombre de usuario está libre. Usa una transacción: si otro
     * dispositivo reserva el mismo nombre entre la lectura y la escritura, Firestore reintenta y
     * la segunda vez lo encuentra ocupado. Las transacciones necesitan conexión.
     *
     * @throws ErrorUsuario.NombreEnUso si el nombre ya está reservado.
     */
    private suspend fun reservandoNombre(nombre: String, escrituras: (Transaction) -> Unit) {
        val libre = db.runTransaction { transaccion ->
            val estaLibre = !transaccion.get(reservaDe(nombre)).exists()
            if (estaLibre) escrituras(transaccion)
            estaLibre
        }.await()
        if (!libre) throw ErrorUsuario.NombreEnUso
    }

    /**
     * Sigue un documento, pero sin dar por cierto que no existe hasta que lo dice el servidor:
     * sin conexión, o antes de sincronizar, Firestore responde eso mismo con lo que tiene en el
     * dispositivo. Pide también los cambios de metadatos porque, si la copia local ya decía que
     * no existía, la confirmación del servidor no cambia ningún dato y de otro modo no llegaría.
     */
    private fun DocumentReference.confirmados(): Flow<DocumentSnapshot> =
        snapshots(MetadataChanges.INCLUDE).filter { it.exists() || !it.metadata.isFromCache }

    /** El nombre con el que el perfil está reservado ahora: el que trae quien llama puede ser viejo. */
    private suspend fun nombreActualDe(perfil: Jugador): String =
        perfil(perfil.id).get(Source.SERVER).await().getString(NOMBRE) ?: throw ErrorUsuario.UsuarioInexistente

    /**
     * Quita todas las amistades de un perfil, antes de borrarlo. Va de a pocas porque las reglas
     * consultan los dos perfiles de cada amistad y tienen un tope de documentos por operación; si
     * se corta en el medio, el perfil sigue existiendo y se puede volver a intentar.
     */
    private suspend fun quitarAmistadesDe(idPerfil: String) {
        for (grupo in amigos(idPerfil).get(Source.SERVER).await().documents.chunked(AMISTADES_POR_LOTE)) {
            db.batch().apply {
                grupo.forEach { amigo ->
                    delete(amigo.reference)
                    delete(amigos(amigo.id).document(idPerfil))
                }
            }.commit().await()
        }
    }

    /** Las escrituras con las que un perfil sin dueño pasa a ser de la [cuenta]. */
    private fun Transaction.vincular(idPerfil: String, cuenta: Cuenta, datosDeCuenta: Map<String, Any> = emptyMap()) {
        update(perfil(idPerfil), UID, cuenta.uid)
        set(db.collection(CUENTAS).document(cuenta.uid), mapOf(PERFIL to idPerfil) + datosDeCuenta)
        set(registroDe(cuenta.mail), mapOf(PERFIL to idPerfil, UID to cuenta.uid))
    }

    /**
     * Una cuenta con un perfil reservado para su mail tiene que aceptarlo o rechazarlo antes de
     * quedarse con otro: mientras el reclamo exista, las reglas no la dejan registrar su mail.
     */
    private suspend fun exigirSinReclamo(cuenta: Cuenta) {
        if (registroDe(cuenta.mail).get(Source.SERVER).await().exists()) throw ErrorUsuario.ReclamoPendiente
    }

    /**
     * Las reglas dejan ver que un mail está libre, pero no leer el registro de otro: que
     * rechacen la lectura también quiere decir que está ocupado.
     *
     * @throws ErrorUsuario.MailConPerfil si el mail ya tiene una cuenta o un perfil reservado.
     */
    private suspend fun exigirMailLibre(mail: String) {
        val ocupado = try {
            registroDe(mail).get(Source.SERVER).await().exists()
        } catch (e: FirebaseFirestoreException) {
            if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
            true
        }
        if (ocupado) throw ErrorUsuario.MailConPerfil
    }

    private fun reclamoDe(idPerfil: String, creador: Cuenta, nombreCreador: String) =
        mapOf(PERFIL to idPerfil, CREADO_POR to creador.uid, NOMBRE_CREADOR to nombreCreador)

    /** Las operaciones que necesitan al servidor fallan con [ErrorUsuario.SinConexion] si no lo alcanzan. */
    private inline fun <T> conConexion(operacion: () -> T): T =
        try {
            operacion()
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) throw ErrorUsuario.SinConexion
            throw e
        }

    /** Lo que guarda cada perfil sobre un amigo: su nombre, para no leer un perfil por cada uno. */
    private fun amistadCon(amigo: Jugador) = mapOf(NOMBRE to amigo.nombre, DESDE to FieldValue.serverTimestamp())

    private fun DocumentSnapshot.aJugador(): Jugador? = getString(NOMBRE)?.let { Jugador(id, it) }

    private fun DocumentSnapshot.aPedido(): PedidoDeReclamo? {
        val idPerfil = getString(PERFIL) ?: return null
        return PedidoDeReclamo(
            uid = id,
            mail = getString(MAIL).orEmpty(),
            perfil = Jugador(idPerfil, getString(NOMBRE).orEmpty()),
        )
    }

    private fun DocumentSnapshot.aUsuario(amistades: QuerySnapshot): Usuario? {
        val jugador = aJugador() ?: return null
        val amigos = amistades.documents.mapNotNull { it.aJugador() }.sortedBy { it.nombre.lowercase(Locale.ROOT) }
        return Usuario(jugador, amigos)
    }

    private companion object {
        /** Amistades que se corrigen o se quitan en una misma operación. */
        const val AMISTADES_POR_LOTE = 3
    }
}
