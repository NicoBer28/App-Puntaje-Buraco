package com.example.puntajeburaco20.Fragments

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModelProvider
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
import com.example.puntajeburaco20.ViewModels.AgregarViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale


lateinit var usuarioAmigo: EditText
lateinit var btnAgregar: Button
lateinit var btnVolver: Button
lateinit var usuarioAmigoIngresado : String
lateinit var usuarioAmigoIngresadoMin: String

class AgregarFragment : Fragment() {

    companion object {
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_agregar, container, false)
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = Firebase.firestore

        val sharedPreferences = requireActivity().getSharedPreferences("PuntajeBuracoPreferences", Context.MODE_PRIVATE)
        var usuarioActual = sharedPreferences.getString("usuarioActual", "aaa")


        usuarioAmigo = view.findViewById<EditText>(R.id.usuarioAmigo)
        btnAgregar = view.findViewById<Button>(R.id.btnAgregar)
        btnVolver = view.findViewById<Button>(R.id.btnVolver)


        btnAgregar.setOnClickListener {
            usuarioAmigoIngresado = usuarioAmigo.text.toString()
            usuarioAmigoIngresadoMin = usuarioAmigoIngresado.lowercase(Locale.ROOT)
            if (usuarioAmigoIngresado == "") {
                Toast.makeText(requireContext(), "Complete el campo", Toast.LENGTH_SHORT).show()
            } else {
                if(usuarioAmigoIngresadoMin == usuarioActual) {
                    Toast.makeText(requireContext(), "El usuario es el tuyo :)", Toast.LENGTH_SHORT).show()
                }else {

                    db.collection("users")
                        .document(usuarioAmigoIngresadoMin)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                var nombreAmigo = document.get("Nombre")
                                if (usuarioActual != null) {
                                    db.collection("users")
                                        .document(usuarioActual)
                                        .get()
                                        .addOnSuccessListener { document2 ->
                                            var nombreUsuario = document2.get("Nombre")
                                            var listAmigos: MutableList<String> = (document2.get("Amigos") as? List<String>)?.toMutableList()
                                            ?: mutableListOf()
                                            var listAmigosNombre: MutableList<String> = (document2.get("AmigosNombre") as? List<String>)?.toMutableList()
                                                ?: mutableListOf()
                                            if (listAmigos != null) {
                                                if (listAmigos.contains(usuarioAmigoIngresadoMin)) {
                                                    Toast.makeText(requireContext(), "Este usuario ya es tu amigo", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    listAmigos.add(usuarioAmigoIngresadoMin)
                                                    listAmigosNombre.add(nombreAmigo as String)

                                                    db.collection("users")
                                                        .document(usuarioActual)
                                                        .update("Amigos", listAmigos, "AmigosNombre", listAmigosNombre)
                                                        .addOnSuccessListener {
                                                            db.collection("users")
                                                                .document(usuarioAmigoIngresadoMin)
                                                                .get()
                                                                .addOnSuccessListener { document3 ->
                                                                    var listAmigos2: MutableList<String> = (document3.get("Amigos") as? List<String>)?.toMutableList()
                                                                        ?: mutableListOf()
                                                                    var listAmigosNombre2: MutableList<String> = (document3.get("AmigosNombre") as? List<String>)?.toMutableList()
                                                                        ?: mutableListOf()
                                                                    listAmigos2.add(usuarioActual)
                                                                    listAmigosNombre2.add(
                                                                        nombreUsuario as String
                                                                    )

                                                                    db.collection("users")
                                                                                .document(usuarioAmigoIngresadoMin)
                                                                                .update("Amigos", listAmigos2, "AmigosNombre", listAmigosNombre2)
                                                                                .addOnSuccessListener {
                                                                                    Toast.makeText(requireContext(), "Usuario Agregado", Toast.LENGTH_SHORT).show()
                                                                                    usuarioAmigo.setText("")
                                                                                }
                                                                }

                                                        }
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


                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "El usuario no existe",
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
        btnVolver.setOnClickListener {
            findNavController().navigate(R.id.partidaFragment)
        }
        }

        }


