package com.example.puntaje_buraco_3.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.puntaje_buraco_3.model.Usuario;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UsuarioRepository {

    private final FirebaseFirestore db;

    private static final String COLECCION_USUARIOS = "users_v2";
    private static final String COLECCION_ANTERIOR = "users";


    public UsuarioRepository() {
        db = FirebaseFirestore.getInstance();
    }


    public Task<Void> guardarPerfilMigrado(String uid, String apodo) {

        DocumentReference docViejoRef = db.collection(COLECCION_ANTERIOR).document(apodo.toLowerCase());
        DocumentReference docNuevoRef = db.collection(COLECCION_USUARIOS).document(uid);

        return docViejoRef.get().continueWithTask(task -> {
            if (!task.isSuccessful() || !task.getResult().exists()) throw new Exception("Perfil no encontrado");

            DocumentSnapshot doc = task.getResult();
            int pj = doc.getLong("Partidas Jugadas") != null ? doc.getLong("Partidas Jugadas").intValue() : 0;
            int pg = doc.getLong("Partidas Ganadas") != null ? doc.getLong("Partidas Ganadas").intValue() : 0;
            List<String> amigos = (List<String>) doc.get("Amigos");

            Usuario migrado = new Usuario(apodo, pj, pg, amigos != null ? amigos : new ArrayList<>());

            return docViejoRef.collection("statistics").get().continueWithTask(taskStats -> {
                WriteBatch batch = db.batch();
                batch.set(docNuevoRef, migrado);

                for (DocumentSnapshot s : taskStats.getResult()) {
                    batch.set(docNuevoRef.collection("statistics").document(s.getId()), s.getData());
                }
                return batch.commit();
            });
        });
    }

    public Task<Void> guardarPerfil(String uid, String apodo) {  //no me gusta el uso de hayQueMigrar, se deberia hacer de alguna forma mejor
        Usuario nuevo = new Usuario(apodo, 0, 0, new ArrayList<>());
        return db.collection(COLECCION_USUARIOS).document(uid).set(nuevo);
    }

    public Task<DocumentSnapshot> obtenerPerfilUsuario(String uid) {
        return db.collection(COLECCION_USUARIOS)
                .document(uid)
                .get();
    }

    //no se si esta bien separado back y front. revisar
    public enum EstadoApodo {
        DISPONIBLE,
        OCUPADO_V2,
        EXISTE_V1
    }

    public interface ApodoCallback {
        void onResultado(EstadoApodo estado);
        void onError(Exception e);
    }

    public void validarApodo(String apodo, ApodoCallback callback) {
        // 1. Buscamos en la nueva
        db.collection(COLECCION_USUARIOS)
                .whereEqualTo("nombreUsuario", apodo)
                .get()
                .addOnSuccessListener(queryV2 -> {
                    if (!queryV2.isEmpty()) {
                        callback.onResultado(EstadoApodo.OCUPADO_V2);
                    } else {
                        // 2. Si no está, buscamos en la vieja
                        db.collection(COLECCION_ANTERIOR)
                                .document(apodo.toLowerCase())
                                .get()
                                .addOnSuccessListener(documentV1 -> {
                                    if (documentV1.exists()) {
                                        callback.onResultado(EstadoApodo.EXISTE_V1);
                                    } else {
                                        callback.onResultado(EstadoApodo.DISPONIBLE);
                                    }
                                })
                                .addOnFailureListener(callback::onError);
                    }
                })
                .addOnFailureListener(callback::onError);
    }
}