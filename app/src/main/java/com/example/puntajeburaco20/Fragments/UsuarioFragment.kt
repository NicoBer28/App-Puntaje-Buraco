package com.example.puntajeburaco20.Fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.puntajeburaco20.R
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale

lateinit var usuario: EditText
lateinit var password: EditText
lateinit var btnCrear: Button
lateinit var btnLogin: Button

lateinit var usuarioIngresado : String
lateinit var passwordIngresado : String
var usuarioIngresadoMin: String = ""

class UsuarioFragment : Fragment() {

    companion object {
        fun newInstance() = UsuarioFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_usuario, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = Firebase.firestore

        usuario = view.findViewById<EditText>(R.id.usuario)
        password = view.findViewById<EditText>(R.id.password)
        btnCrear = view.findViewById<Button>(R.id.btnCrear)
        btnLogin = view.findViewById<Button>(R.id.btnLogin)

        btnCrear.setOnClickListener {
            usuarioIngresado = usuario.text.toString()
            usuarioIngresadoMin = usuarioIngresado.lowercase(Locale.ROOT)
            passwordIngresado = password.text.toString()
            if (usuarioIngresado == "" || passwordIngresado == "") {
                Toast.makeText(requireContext(), "Complete todos los campos", Toast.LENGTH_SHORT).show()
            } else {
                if (usuarioIngresado.length < 3 || passwordIngresado.length < 3) {
                    Toast.makeText(
                        requireContext(),
                        "El usuario y la contraseña deben contener al menos 3 caracteres",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    if (usuarioIngresado.length > 8 || passwordIngresado.length > 8) {
                        Toast.makeText(
                            requireContext(),
                            "El usuario y la contraseña deben contener máximo 8 caracteres",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        if (Regex("[^a-zA-Z0-9]").containsMatchIn(usuarioIngresado) ||
                            Regex("[^a-zA-Z0-9]").containsMatchIn(passwordIngresado)) {
                            Toast.makeText(
                                requireContext(),
                                "No se permiten caracteres especiales, solo letras y números",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                         else {
                            db.collection("users")
                                .document(usuarioIngresadoMin)
                                .get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        Toast.makeText(
                                            requireContext(),
                                            "Este nombre ya está en uso",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                    } else {
                                        val nuevoUsuario = hashMapOf(
                                            "Nombre" to usuarioIngresado,
                                            "Password" to passwordIngresado,
                                            "Amigos" to mutableListOf<String>(),
                                            "AmigosNombre" to mutableListOf<String>(),
                                        )
                                        db.collection("users").document(usuarioIngresadoMin)
                                            .set(nuevoUsuario)
                                            .addOnSuccessListener {
                                                Toast.makeText(
                                                    requireContext(),
                                                    "Usuario creado con éxito",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                                val sharedPreferences =
                                                    requireActivity().getSharedPreferences(
                                                        "PuntajeBuracoPreferences",
                                                        Context.MODE_PRIVATE
                                                    )
                                                val editor = sharedPreferences.edit()

                                                // Guardar la posición del fragmento actual
                                                editor.putString(
                                                    "posicionFragmentoActual",
                                                    "PartidaFragment"
                                                )
                                                editor.putString(
                                                    "usuarioActual",
                                                    usuarioIngresadoMin
                                                )

                                                editor.apply()

                                                findNavController().navigate(R.id.partidaFragment)
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(
                                                    requireContext(),
                                                    "Error al crear usuario: $e",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        requireContext(),
                                        "Error al verificar usuario: $e",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                }
            }
        }

        btnLogin.setOnClickListener {
            usuarioIngresado = usuario.text.toString()
            usuarioIngresadoMin = usuarioIngresado.lowercase(Locale.ROOT)
            passwordIngresado = password.text.toString()
            if (usuarioIngresado == "" || passwordIngresado == "") {
                Toast.makeText(requireContext(), "Complete todos los campos", Toast.LENGTH_SHORT).show()
            }
            else {
                            db.collection("users")
                                .document(usuarioIngresadoMin)
                                .get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        if (document["Password"] == passwordIngresado) {
                                            val sharedPreferences =
                                                requireActivity().getSharedPreferences(
                                                    "PuntajeBuracoPreferences",
                                                    Context.MODE_PRIVATE
                                                )
                                            val editor = sharedPreferences.edit()

                                            // Guardar la posición del fragmento actual
                                            editor.putString(
                                                "posicionFragmentoActual",
                                                "PartidaFragment"
                                            )
                                            editor.putString(
                                                "usuarioActual",
                                                usuarioIngresadoMin
                                            )

                                            editor.apply()

                                            findNavController().navigate(R.id.partidaFragment)

                                        } else {
                                            Toast.makeText(
                                                requireContext(),
                                                "Contraseña incorrecta",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            "Este usuario no existe",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        requireContext(),
                                        "Error al verificar usuario: $e",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                }


}