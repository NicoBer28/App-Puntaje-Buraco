package com.example.puntaje_buraco_3.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseUser;
import com.example.puntaje_buraco_3.R;
import com.example.puntaje_buraco_3.repository.AuthRepository;
import com.example.puntaje_buraco_3.repository.UsuarioRepository;

public class UsuarioFragment extends Fragment {

    private EditText etApodo;
    private Button btnGuardarApodo;

    private AuthRepository authRepository;
    private UsuarioRepository usuarioRepository;

    public UsuarioFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_usuario, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etApodo = view.findViewById(R.id.etApodo);
        btnGuardarApodo = view.findViewById(R.id.btnGuardarApodo);

        authRepository = new AuthRepository();
        usuarioRepository = new UsuarioRepository();

        btnGuardarApodo.setOnClickListener(v -> {
            String apodoIngresado = etApodo.getText().toString().trim();

            if (apodoIngresado.isEmpty()) {
                etApodo.setError("Por favor, ingresa un apodo");
                return;
            }

            btnGuardarApodo.setEnabled(false);

            //no se si esta bien separado back y front. revisar
            usuarioRepository.validarApodo(apodoIngresado, new UsuarioRepository.ApodoCallback() {
                @Override
                public void onResultado(UsuarioRepository.EstadoApodo estado) {
                    switch (estado) {
                        case OCUPADO_V2:
                            etApodo.setError("Este apodo ya está en uso. ¡Elige otro!");
                            btnGuardarApodo.setEnabled(true);
                            break;

                        case EXISTE_V1:
                            mostrarDialogoMigracion(apodoIngresado, view);
                            break;

                        case DISPONIBLE:
                            guardarNuevoPerfil(view, apodoIngresado);
                            break;
                    }
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnGuardarApodo.setEnabled(true);
                }
            });
        });
    }

    private void mostrarDialogoMigracion(String apodo, View view) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Apodo encontrado")
                .setMessage("Existe un historial viejo con el nombre '" + apodo + "'. ¿Eres tú y quieres recuperar tus puntos?")
                .setCancelable(false)
                .setPositiveButton("Sí, soy yo (Aceptar)", (dialog, which) -> {
                    guardarNuevoPerfilMigrado(view, apodo);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    dialog.dismiss();
                    etApodo.setError("Por favor, elige un apodo distinto");
                    btnGuardarApodo.setEnabled(true);
                })
                .show();
    }


//deberia unificar. codigo repetido
    private void guardarNuevoPerfil(View view, String apodo) {
        String uid = authRepository.getUsuarioActual().getUid();

        usuarioRepository.guardarPerfil(uid, apodo)
                .addOnSuccessListener(aVoid -> Navigation.findNavController(view).navigate(R.id.action_usuarioFragment_to_partidaFragment))
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    private void guardarNuevoPerfilMigrado(View view, String apodo) {
        String uid = authRepository.getUsuarioActual().getUid();

        usuarioRepository.guardarPerfilMigrado(uid, apodo)
                .addOnSuccessListener(aVoid -> Navigation.findNavController(view).navigate(R.id.action_usuarioFragment_to_partidaFragment))
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
