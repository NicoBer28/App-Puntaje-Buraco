package com.example.puntajeburaco20.Fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.puntajeburaco20.R
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


lateinit var botonNuevaPartida: Button
lateinit var botonHistoriales: Button
lateinit var botonAgregarUsuario: Button
lateinit var nombreUsuario: TextView


var jugadorUno2: String = ""
var jugadorDos2: String = ""
var jugadorTres2: String = ""
var jugadorCuatro2: String = ""
var cantJugadores2: Int = 2


/**
 * A simple [Fragment] subclass.
 * Use the [PartidaFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PartidaFragment : Fragment() {

    private var mutableList: MutableList<String> = mutableListOf()

    private val mutableListJugadores: MutableList<String> =
        mutableListOf("2 Jugadores", "4 Jugadores")
    private var primerNombre3: Int = 0
    private var primerNombre4: Int = 0

    /*mutableList.add("Damasco")
mutableList.removeAt(1)*/


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_partida, container, false)
    }

    companion object {

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Confirmación")
                        .setMessage("¿Estás seguro de que quieres salir?")
                        .setPositiveButton("Sí") { _, _ ->

                            findNavController().popBackStack()

                        }
                        .setNegativeButton("No", null)
                        .show()
                }
            })

        val db = Firebase.firestore

        botonNuevaPartida = view.findViewById<Button>(R.id.btnNuevaPartida)
        botonHistoriales = view.findViewById<Button>(R.id.btnHistoriales)
        botonAgregarUsuario = view.findViewById<Button>(R.id.btnUsuario)
        nombreUsuario = view.findViewById<EditText>(R.id.nombreUsuario)

        val sharedPreferences =
            requireActivity().getSharedPreferences("PuntajeBuracoPreferences", Context.MODE_PRIVATE)
        val usuarioActual = sharedPreferences.getString("usuarioActual", "aaa")



        if (usuarioActual != null) {

            mutableList.clear()

            db.collection("users").document(usuarioActual)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        // El documento existe, obtén el valor del campo específico
                        nombreUsuario.text = documentSnapshot.get("Nombre") as String?
                        var nombreUsuarioActual = documentSnapshot.get("Nombre") as String?
                        if (nombreUsuarioActual != null) {
                            mutableList.add(0, nombreUsuarioActual)
                        }

                        // Ahora puedes usar el valor del campo según sea necesario
                    } else {
                    }
                }
                .addOnFailureListener { e ->
                }


            db.collection("users")
                .document(usuarioActual)
                .get()
                .addOnSuccessListener { document3 ->
                    val nuevosAmigos: List<String>? = document3.get("AmigosNombre") as? List<String>

                    nuevosAmigos?.let {
                        mutableList.addAll(it)
                    }



              //      Log.d("MiApp", "$mutableList")


                    mutableList.add(0, "Nombre")

                 //   Log.d("MiApp", "$mutableList")
                    jugadorUno2 = mutableList[0]
                    jugadorDos2 = mutableList[0]
                    cantJugadores2 = 2
                    var primeraSeleccionUno = 1
                    var primeraSeleccionDos = 1
                    var primeraSeleccionTres = 1
                    var primeraSeleccionCuatro = 1


                    val spinner1: Spinner = view.findViewById(R.id.spinner1)
                    val spinner2: Spinner = view.findViewById(R.id.spinner2)
                    val spinner3: Spinner = view.findViewById(R.id.spinner3)
                    val spinner4: Spinner = view.findViewById(R.id.spinner4)
                    val spinnerJugadores: Spinner = view.findViewById(R.id.spinnerJugadores)
                    var opcionAnterior1 = mutableList[0]
                    var opcionAnterior2 = mutableList[0]
                    var opcionAnterior3 = mutableList[0]
                    var opcionAnterior4 = mutableList[0]

                    primerNombre3 = 0
                    primerNombre4 = 0


                    val adapterJugadores = ArrayAdapter(
                        requireContext(),
                        R.layout.spinner_selected,
                        mutableListJugadores
                    )
                    adapterJugadores.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerJugadores.adapter = adapterJugadores

                    spinnerJugadores.onItemSelectedListener =
                        object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: AdapterView<*>,
                                view: View?,
                                position: Int,
                                id: Long
                            ) {
                                when (position) {
                                    0 -> { // Si se selecciona la primera opción
                                        spinner3.visibility = View.GONE
                                        spinner4.visibility = View.GONE
                                        cantJugadores2 = 2
                                        val sharedPreferences = requireActivity().getSharedPreferences("PuntajeBuracoPreferences", Context.MODE_PRIVATE)
                                        val editor = sharedPreferences.edit()

                                        editor.putInt("cantJugadores", cantJugadores2)

                                        editor.apply()
                                        if (jugadorTres2 == mutableList[0]) {
                                            jugadorTres2 = ""
                                        }
                                        if (jugadorCuatro2 == mutableList[0]) {
                                            jugadorCuatro2 = ""
                                        }
                                        spinner2.setBackgroundResource(R.drawable.spinner_equipo_dos)


                                    }

                                    1 -> { // Si se selecciona la segunda opción
                                        spinner3.visibility = View.VISIBLE
                                        spinner4.visibility = View.VISIBLE
                                        cantJugadores2 = 4
                                        val sharedPreferences = requireActivity().getSharedPreferences("PuntajeBuracoPreferences", Context.MODE_PRIVATE)
                                        val editor = sharedPreferences.edit()

                                        editor.putInt("cantJugadores", cantJugadores2)

                                        editor.apply()
                                        if (jugadorTres2 == "") {
                                            jugadorTres2 = mutableList[0]
                                        }
                                        if (jugadorCuatro2 == "") {
                                            jugadorCuatro2 = mutableList[0]
                                        }
                                        spinner2.setBackgroundResource(R.drawable.spinner_equipo_uno)
                                    }

                                }
                                //val selectedItemJugadores = parent.getItemAtPosition(position).toString()
                                //Toast.makeText(requireContext(), "Seleccionado: $selectedItemJugadores", Toast.LENGTH_SHORT).show()
                            }

                            override fun onNothingSelected(parent: AdapterView<*>) {
                                // Método requerido, pero puedes dejarlo vacío si no necesitas manejar este caso
                            }
                        }
                    //-------------------

                    val adapter1 =
                        ArrayAdapter(
                            requireContext(),
                            R.layout.spinner_selected,
                            mutableList
                        )
                    adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner1.adapter = adapter1
                    //spinner1.setSelection(0)
                    spinner1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            // Log.d("nombres", "$opcionAnterior1")
                            if (primeraSeleccionUno == 1) {
                                spinner1.setSelection(0)
                                primeraSeleccionUno = 0
                            } else {
                                jugadorUno2 = parent.getItemAtPosition(position).toString()
                                if(jugadorUno2 != "Nombre") {
                                    val sharedPreferences = requireActivity().getSharedPreferences(
                                        "PuntajeBuracoPreferences",
                                        Context.MODE_PRIVATE
                                    )
                                    val editor = sharedPreferences.edit()

                                    editor.putString("jugadorUno", jugadorUno2)
                                    Log.d("nombres", jugadorUno2)

                                    editor.apply()
                                }
                                if (opcionAnterior1 != mutableList[0]) {
                                    mutableList.add(opcionAnterior1)
                                }
                                opcionAnterior1 = parent.getItemAtPosition(position).toString()

                                if (opcionAnterior1 != mutableList[0]) {
                                    mutableList.removeAt(position)
                                }
                            }

                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {
                            // Método requerido, pero puedes dejarlo vacío si no necesitas manejar este caso
                        }
                    }
                    //-------------------
                    val adapter2 =
                        ArrayAdapter(
                            requireContext(),
                            R.layout.spinner_selected,
                            mutableList
                        )
                    adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner2.adapter = adapter2
                    spinner2.setSelection(0)
                    spinner2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            if (primeraSeleccionDos == 1) {
                                spinner2.setSelection(0)
                                primeraSeleccionDos = 0
                            } else {
                                jugadorDos2 = parent.getItemAtPosition(position).toString()
                                if(jugadorDos2 != "Nombre") {
                                    val sharedPreferences = requireActivity().getSharedPreferences(
                                        "PuntajeBuracoPreferences",
                                        Context.MODE_PRIVATE
                                    )
                                    val editor = sharedPreferences.edit()

                                    editor.putString("jugadorDos", jugadorDos2)

                                    editor.apply()
                                }
                                if (opcionAnterior2 != mutableList[0]) {
                                    mutableList.add(opcionAnterior2)
                                }
                                opcionAnterior2 = parent.getItemAtPosition(position).toString()
                                if (opcionAnterior2 != mutableList[0]) {
                                    mutableList.removeAt(position)
                                }
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {
                            // Método requerido, pero puedes dejarlo vacío si no necesitas manejar este caso
                        }
                    }
                    //-------------------
                    val adapter3 =
                        ArrayAdapter(
                            requireContext(),
                            R.layout.spinner_selected,
                            mutableList
                        )
                    adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner3.adapter = adapter3
                    spinner3.setSelection(0)
                    spinner3.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            if (primeraSeleccionTres == 1) {
                                spinner3.setSelection(0)
                                primeraSeleccionTres = 0
                            } else {
                                jugadorTres2 = parent.getItemAtPosition(position).toString()
                                if(jugadorTres2 != "Nombre") {
                                    val sharedPreferences = requireActivity().getSharedPreferences(
                                        "PuntajeBuracoPreferences",
                                        Context.MODE_PRIVATE
                                    )
                                    val editor = sharedPreferences.edit()

                                    editor.putString("jugadorTres", jugadorTres2)

                                    editor.apply()
                                }
                                if (opcionAnterior3 != mutableList[0]) {
                                    mutableList.add(opcionAnterior3)
                                }
                                opcionAnterior3 = parent.getItemAtPosition(position).toString()
                                if (opcionAnterior3 != mutableList[0]) {
                                    mutableList.removeAt(position)
                                }
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {
                            // Método requerido, pero puedes dejarlo vacío si no necesitas manejar este caso
                        }
                    }
                    //-------------------
                    val adapter4 =
                        ArrayAdapter(
                            requireContext(),
                            R.layout.spinner_selected,
                            mutableList
                        )
                    adapter4.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner4.adapter = adapter4
                    spinner4.setSelection(0)
                    spinner4.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            if (primeraSeleccionCuatro == 1) {
                                spinner4.setSelection(0)
                                primeraSeleccionCuatro = 0
                            } else {
                                jugadorCuatro2 = parent.getItemAtPosition(position).toString()
                                if(jugadorCuatro2 != "Nombre") {
                                    val sharedPreferences = requireActivity().getSharedPreferences(
                                        "PuntajeBuracoPreferences",
                                        Context.MODE_PRIVATE
                                    )
                                    val editor = sharedPreferences.edit()

                                    editor.putString("jugadorCuatro", jugadorCuatro2)

                                    editor.apply()
                                }
                                if (opcionAnterior4 != mutableList[0]) {
                                    mutableList.add(opcionAnterior4)
                                }
                                opcionAnterior4 = parent.getItemAtPosition(position).toString()
                                if (opcionAnterior4 != mutableList[0]) {
                                    mutableList.removeAt(position)
                                }
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {
                            // Método requerido, pero puedes dejarlo vacío si no necesitas manejar este caso
                        }
                    }
                }




            botonNuevaPartida.setOnClickListener {
                if (jugadorUno2 == mutableList[0] || jugadorDos2 == mutableList[0] || jugadorTres2 == mutableList[0] || jugadorCuatro2 == mutableList[0]) {
                    Toast.makeText(requireContext(), "Elija un jugador", Toast.LENGTH_SHORT).show()
                } else {
                    val sharedPreferences = requireActivity().getSharedPreferences(
                        "PuntajeBuracoPreferences",
                        Context.MODE_PRIVATE
                    )
                    val editor = sharedPreferences.edit()

                    // Guardar la posición del fragmento actual
                    editor.putString("posicionFragmentoActual", "PuntajeFragment")
                    editor.apply()

                    findNavController().navigate(R.id.puntajeFragment)
                }

            }

            botonAgregarUsuario.setOnClickListener {
                findNavController().navigate(R.id.agregarFragment)

            }

            botonHistoriales.setOnClickListener {
                findNavController().navigate(R.id.historialFragment)

            }

        }


    }
}
