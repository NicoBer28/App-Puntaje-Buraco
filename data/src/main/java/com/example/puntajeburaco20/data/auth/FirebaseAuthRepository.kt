package com.example.puntajeburaco20.data.auth

import android.util.Log
import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Cuenta
import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Cuentas de acceso con Firebase Authentication (mail y contraseña). */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
) : AuthRepository {

    // Firebase avisa cuando alguien entra o sale, pero no cuando se verifica el mail: por eso la
    // cuenta se guarda acá y recargar() la actualiza a mano.
    private val actual = MutableStateFlow(auth.currentUser?.aCuenta())

    override val cuenta: Flow<Cuenta?> = actual.asStateFlow()

    init {
        // Idioma de los mails de verificación y de recuperación de contraseña.
        auth.setLanguageCode(IDIOMA_MAILS)
        auth.addAuthStateListener { actual.value = it.currentUser?.aCuenta() }
    }

    override suspend fun registrar(mail: String, password: String): Cuenta = traducirErrores {
        auth.createUserWithEmailAndPassword(mail, password).await()
        publicarCuentaActual() ?: throw ErrorUsuario.SinSesion
    }

    override suspend fun iniciarSesion(mail: String, password: String): Cuenta = traducirErrores {
        auth.signInWithEmailAndPassword(mail, password).await()
        publicarCuentaActual() ?: throw ErrorUsuario.SinSesion
    }

    override suspend fun cerrarSesion() {
        auth.signOut()
        publicarCuentaActual()
    }

    override suspend fun enviarVerificacion() = traducirErrores {
        val usuario = auth.currentUser ?: throw ErrorUsuario.SinSesion
        usuario.sendEmailVerification().await()
        Unit
    }

    override suspend fun recargar(): Cuenta? = traducirErrores {
        val usuario = auth.currentUser ?: return@traducirErrores publicarCuentaActual()
        usuario.reload().await()
        if (usuario.isEmailVerified) renovarTokenSiNoDiceVerificado(usuario)
        publicarCuentaActual()
    }

    override suspend fun enviarRecuperacion(mail: String) = traducirErrores {
        try {
            auth.sendPasswordResetEmail(mail).await()
        } catch (_: FirebaseAuthInvalidUserException) {
            // No se informa si el mail tiene cuenta o no.
        }
        Unit
    }

    override suspend fun confirmarIdentidad(password: String) = traducirErrores {
        val usuario = auth.currentUser ?: throw ErrorUsuario.SinSesion
        usuario.reauthenticate(EmailAuthProvider.getCredential(usuario.email.orEmpty(), password)).await()
        Unit
    }

    override suspend fun borrarCuenta() = traducirErrores {
        val usuario = auth.currentUser ?: throw ErrorUsuario.SinSesion
        usuario.delete().await()
        publicarCuentaActual()
        Unit
    }

    /**
     * Las reglas de Firestore leen del token si el mail está verificado, y el token guardado es
     * anterior a la verificación. Sin renovarlo seguirían rechazando todo hasta que venza solo.
     */
    private suspend fun renovarTokenSiNoDiceVerificado(usuario: FirebaseUser) {
        val token = usuario.getIdToken(false).await()
        if (token.claims[CLAIM_MAIL_VERIFICADO] != true) usuario.getIdToken(true).await()
    }

    private fun publicarCuentaActual(): Cuenta? = auth.currentUser?.aCuenta().also { actual.value = it }

    private fun FirebaseUser.aCuenta() = Cuenta(uid = uid, mail = email.orEmpty(), verificada = isEmailVerified)

    /** Convierte los errores de Firebase en los del dominio; cualquier otro sigue de largo. */
    private inline fun <T> traducirErrores(operacion: () -> T): T =
        try {
            operacion()
        } catch (e: FirebaseException) {
            val error = when (e) {
                // Va primero: es un caso particular de credenciales inválidas.
                is FirebaseAuthWeakPasswordException ->
                    ErrorUsuario.ContrasenaCorta(ValidadorCredenciales.LONGITUD_MINIMA_PASSWORD)
                is FirebaseAuthInvalidCredentialsException ->
                    if (e.errorCode == CODIGO_MAIL_INVALIDO) ErrorUsuario.MailInvalido else ErrorUsuario.CredencialesIncorrectas
                is FirebaseAuthInvalidUserException -> ErrorUsuario.CredencialesIncorrectas
                is FirebaseAuthUserCollisionException -> ErrorUsuario.MailEnUso
                is FirebaseTooManyRequestsException -> ErrorUsuario.DemasiadosIntentos
                is FirebaseNetworkException -> ErrorUsuario.SinConexion
                else -> throw e
            }
            Log.w(TAG, "Firebase Auth rechazó la operación", e)
            throw error
        }

    private companion object {
        const val TAG = "Cuentas"
        const val IDIOMA_MAILS = "es"
        const val CODIGO_MAIL_INVALIDO = "ERROR_INVALID_EMAIL"
        const val CLAIM_MAIL_VERIFICADO = "email_verified"
    }
}
