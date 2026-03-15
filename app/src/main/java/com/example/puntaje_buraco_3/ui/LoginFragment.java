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

import com.example.puntaje_buraco_3.R;
import com.example.puntaje_buraco_3.repository.AuthRepository;

public class LoginFragment extends Fragment {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnIngresar;
    private Button btnRegistrarse;
    private Button btnGoogle;

    private AuthRepository authRepository;

    public LoginFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepository = new AuthRepository();

        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnIngresar = view.findViewById(R.id.btnIngresar);
        btnRegistrarse = view.findViewById(R.id.btnRegistrarse);
        btnGoogle = view.findViewById(R.id.btnGoogle);

        btnIngresar.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            etPassword.setError(null);

            authRepository.loginConEmail(email, password)
                    .addOnSuccessListener(authResult -> {
                        Toast.makeText(getContext(), "¡Login exitoso!", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(view).navigate(R.id.action_login_to_usuarioFragment);
                    })
                    .addOnFailureListener(e -> {
                        manejarError(e);
                    });
        });

        btnRegistrarse.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            etPassword.setError(null);

            authRepository.registrarConEmail(email, password)
                    .addOnSuccessListener(authResult -> {
                        Toast.makeText(getContext(), "¡Cuenta creada!", Toast.LENGTH_SHORT).show();

                        Navigation.findNavController(view).navigate(R.id.action_login_to_usuarioFragment);
                    })
                    .addOnFailureListener(e -> {
                        manejarError(e);
                    });
        });

        btnGoogle.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Próximamente: Ingreso con Google", Toast.LENGTH_SHORT).show();
        });
    }

    private void manejarError(Exception e) {
        if (e instanceof IllegalArgumentException) {
            etPassword.setError(e.getMessage());
            etPassword.requestFocus();
        } else {
            // Si es un error de Firebase (ej. "Contraseña incorrecta", "Usuario no existe")
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}