package com.example.puntaje_buraco_3.repository;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class AuthRepository {

    private final FirebaseAuth mAuth;

    public AuthRepository() {
        mAuth = FirebaseAuth.getInstance();
    }

    public Task<AuthResult> registrarConEmail(String email, String password) {
        if (!esEmailValido(email)) {
            return Tasks.forException(new IllegalArgumentException("El formato del correo es inválido"));
        } else if (!esPasswordValida(password)) {
            return Tasks.forException(new IllegalArgumentException("La contraseña es inválida. Debe tener al menos 6 caracteres"));
        }
        return mAuth.createUserWithEmailAndPassword(email, password);    }

    public Task<AuthResult> loginConEmail(String email, String password) {
        if (!esEmailValido(email)) {
            return Tasks.forException(new IllegalArgumentException("El formato del correo es inválido"));
        } else if (!esPasswordValida(password)) {
            return Tasks.forException(new IllegalArgumentException("La contraseña es inválida. Debe tener al menos 6 caracteres"));
        }
        return mAuth.signInWithEmailAndPassword(email, password);
    }

    public Task<AuthResult> loginConGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        return mAuth.signInWithCredential(credential);
    }

    public FirebaseUser getUsuarioActual() {
        return mAuth.getCurrentUser();
    }

    public void cerrarSesion() {
        mAuth.signOut();
    }

    private boolean esEmailValido(String email) {
        return email != null && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean esPasswordValida(String password) {
        return password != null && password.length() >= 6;
    }
}