package com.example.puntajeburaco20.Fragments

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
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
import androidx.navigation.fragment.findNavController
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.ViewModels.HistorialViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale

lateinit var btnBuscar: Button
lateinit var btnVolver2: Button
lateinit var partidasJugadas: TextView
lateinit var partidasGanadas: TextView
lateinit var partidasPerdidas: TextView
lateinit var ganUno: TextView
lateinit var ganDos: TextView
lateinit var jugUno: TextView
lateinit var jugDos: TextView
lateinit var perdUno: TextView
lateinit var perdDos: TextView

var jugadorUno3: String = ""
var jugadorDos3: String = ""
var jugadorTres3: String = ""
var jugadorCuatro3: String = ""
var cantJugadores3: Int = 2

class HistorialFragment : Fragment() {

    private var mutableList2: MutableList<String> = mutableListOf()

    private val mutableListJugadores2: MutableList<String> =
        mutableListOf("2 Jugadores", "4 Jugadores")
    private var primerNombre3: Int = 0
    private var primerNombre4: Int = 0

    companion object {
        fun newInstance() = HistorialFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_historial, container, false)
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = Firebase.firestore

        val sharedPreferences =
            requireActivity().getSharedPreferences("PuntajeBuracoPreferences", Context.MODE_PRIVATE)
        var usuarioActual = sharedPreferences.getString("usuarioActual", "aaa")

        btnBuscar = view.findViewById<Button>(R.id.btnBuscar)
        btnVolver2 = view.findViewById<Button>(R.id.btnVolver2)
        partidasJugadas = view.findViewById<EditText>(R.id.partJugadas)
        partidasGanadas = view.findViewById<EditText>(R.id.partGanadas)
        partidasPerdidas = view.findViewById<EditText>(R.id.partPerdidas)
        ganUno = view.findViewById<EditText>(R.id.ganUno)
        ganDos = view.findViewById<EditText>(R.id.ganDos)
        jugUno = view.findViewById<EditText>(R.id.jugUno)
        jugDos = view.findViewById<EditText>(R.id.jugDos)
        perdUno = view.findViewById<EditText>(R.id.perdUno)
        perdDos = view.findViewById<EditText>(R.id.perdDos)

        if (usuarioActual != null) {

            mutableList2.clear()

            db.collection("users").document(usuarioActual)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        var nombreUsuarioActual = documentSnapshot.get("Nombre") as String?
                        if (nombreUsuarioActual != null) {
                            mutableList2.add(0, nombreUsuarioActual)
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
                        mutableList2.addAll(it)
                    }



                    //      Log.d("MiApp", "$mutableList")


                    mutableList2.add(0, "General")

                    //   Log.d("MiApp", "$mutableList")
                    jugadorUno3 = mutableList2[0]
                    jugadorDos3 = mutableList2[0]
                    cantJugadores3 = 2
                    var primeraSeleccionUno = 1
                    var primeraSeleccionDos = 1
                    var primeraSeleccionTres = 1
                    var primeraSeleccionCuatro = 1


                    val spinner1: Spinner = view.findViewById(R.id.spnJugUno)
                    val spinner2: Spinner = view.findViewById(R.id.spnJugDos)
                    val spinner3: Spinner = view.findViewById(R.id.spnJugTres)
                    val spinner4: Spinner = view.findViewById(R.id.spnJugCuatro)
                    val spinnerJugadores: Spinner = view.findViewById(R.id.spnCantJug)
                    var opcionAnterior1 = mutableList2[0]
                    var opcionAnterior2 = mutableList2[0]
                    var opcionAnterior3 = mutableList2[0]
                    var opcionAnterior4 = mutableList2[0]

                    primerNombre3 = 0
                    primerNombre4 = 0


                    val adapterJugadores = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        mutableListJugadores2
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
                                        cantJugadores3 = 2
                                        jugUno.text = "0"
                                        ganUno.text = "0"
                                        perdUno.text = "0"
                                        jugDos.text = "0"
                                        perdDos.text = "0"
                                        ganDos.text = "0"
                                        if (jugadorTres3 == mutableList2[0]) {
                                            jugadorTres3 = ""
                                        }
                                        if (jugadorCuatro3 == mutableList2[0]) {
                                            jugadorCuatro3 = ""
                                        }


                                    }

                                    1 -> { // Si se selecciona la segunda opción
                                        spinner3.visibility = View.VISIBLE
                                        spinner4.visibility = View.VISIBLE
                                        cantJugadores3 = 4
                                        jugUno.text = "0"
                                        ganUno.text = "0"
                                        perdUno.text = "0"
                                        jugDos.text = "0"
                                        perdDos.text = "0"
                                        ganDos.text = "0"
                                        if (jugadorTres3 == "") {
                                            jugadorTres3 = mutableList2[0]
                                        }
                                        if (jugadorCuatro3 == "") {
                                            jugadorCuatro3 = mutableList2[0]
                                        }
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
                            android.R.layout.simple_spinner_item,
                            mutableList2
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
                                jugadorUno3 = parent.getItemAtPosition(position).toString()
                                jugUno.text = "0"
                                ganUno.text = "0"
                                perdUno.text = "0"
                                jugDos.text = "0"
                                perdDos.text = "0"
                                ganDos.text = "0"
                                if (opcionAnterior1 != mutableList2[0]) {
                                    mutableList2.add(opcionAnterior1)
                                }
                                opcionAnterior1 = parent.getItemAtPosition(position).toString()

                                if (opcionAnterior1 != mutableList2[0]) {
                                    mutableList2.removeAt(position)
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
                            android.R.layout.simple_spinner_item,
                            mutableList2
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
                                jugadorDos3 = parent.getItemAtPosition(position).toString()
                                jugUno.text = "0"
                                ganUno.text = "0"
                                perdUno.text = "0"
                                jugDos.text = "0"
                                perdDos.text = "0"
                                ganDos.text = "0"
                                if (opcionAnterior2 != mutableList2[0]) {
                                    mutableList2.add(opcionAnterior2)
                                }
                                opcionAnterior2 = parent.getItemAtPosition(position).toString()
                                if (opcionAnterior2 != mutableList2[0]) {
                                    mutableList2.removeAt(position)
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
                            android.R.layout.simple_spinner_item,
                            mutableList2
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
                                jugadorTres3 = parent.getItemAtPosition(position).toString()
                                jugUno.text = "0"
                                ganUno.text = "0"
                                perdUno.text = "0"
                                jugDos.text = "0"
                                perdDos.text = "0"
                                ganDos.text = "0"
                                if (opcionAnterior3 != mutableList2[0]) {
                                    mutableList2.add(opcionAnterior3)
                                }
                                opcionAnterior3 = parent.getItemAtPosition(position).toString()
                                if (opcionAnterior3 != mutableList2[0]) {
                                    mutableList2.removeAt(position)
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
                            android.R.layout.simple_spinner_item,
                            mutableList2
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
                                jugadorCuatro3 = parent.getItemAtPosition(position).toString()
                                jugUno.text = "0"
                                ganUno.text = "0"
                                perdUno.text = "0"
                                jugDos.text = "0"
                                perdDos.text = "0"
                                ganDos.text = "0"
                                if (opcionAnterior4 != mutableList2[0]) {
                                    mutableList2.add(opcionAnterior4)
                                }
                                opcionAnterior4 = parent.getItemAtPosition(position).toString()
                                if (opcionAnterior4 != mutableList2[0]) {
                                    mutableList2.removeAt(position)
                                }
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {
                            // Método requerido, pero puedes dejarlo vacío si no necesitas manejar este caso
                        }
                    }
                }




            btnBuscar.setOnClickListener {
                if(cantJugadores3 == 2){
                    if(jugadorUno3 == mutableList2[0]){
                        Toast.makeText(requireContext(), "Complete el primer campo", Toast.LENGTH_SHORT).show()
                    }else{
                        if(jugadorDos3 == mutableList2[0]){
                            db.collection("users").document(jugadorUno3.lowercase(Locale.ROOT)).get().addOnSuccessListener { documentSnapshot ->
                                if (documentSnapshot.exists()) {
                                    val partidasJugadas = documentSnapshot.getLong("Partidas Jugadas") ?: 0
                                    val partidasGanadas = documentSnapshot.getLong("Partidas Ganadas") ?: 0
                                    jugUno.text = partidasJugadas.toString()
                                    ganUno.text = partidasGanadas.toString()
                                    perdUno.text = (partidasJugadas - partidasGanadas).toString()

                                }
                            }

                        }else{
                            db.collection("users").document(jugadorUno3.lowercase(Locale.ROOT)).collection("statistics")
                                .document(jugadorDos3.lowercase(Locale.ROOT)).get().addOnSuccessListener { documentSnapshot ->
                                if (documentSnapshot.exists()) {
                                    val partidasJugadas = documentSnapshot.getLong("Partidas Jugadas") ?: 0
                                    val partidasGanadas = documentSnapshot.getLong("Partidas Ganadas") ?: 0
                                    jugUno.text = partidasJugadas.toString()
                                    ganUno.text = partidasGanadas.toString()
                                    perdUno.text = (partidasJugadas - partidasGanadas).toString()
                                    jugDos.text = partidasJugadas.toString()
                                    perdDos.text = partidasGanadas.toString()
                                    ganDos.text = (partidasJugadas - partidasGanadas).toString()

                                }
                                    else{
                                    jugUno.text = "0"
                                    ganUno.text = "0"
                                    perdUno.text = "0"
                                    jugDos.text = "0"
                                    perdDos.text = "0"
                                    ganDos.text = "0"
                                    Toast.makeText(requireContext(), "Nunca jugaron entre ellos", Toast.LENGTH_SHORT).show()

                                }
                            }

                        }
                    }

                }
                if(cantJugadores3 == 4) {
                    val stringAlfaSuperior =
                        if (jugadorUno3.lowercase(Locale.ROOT) < jugadorTres3.lowercase(Locale.ROOT)) jugadorUno3 else jugadorTres3
                    val stringAlfaInferior =
                        if (jugadorUno3.lowercase(Locale.ROOT) < jugadorTres3.lowercase(Locale.ROOT)) jugadorTres3 else jugadorUno3
                    var equipoOne = "$stringAlfaSuperior$stringAlfaInferior"

                    val stringAlfaSuperior2 =
                        if (jugadorDos3.lowercase(Locale.ROOT) < jugadorCuatro3.lowercase(Locale.ROOT)) jugadorDos3 else jugadorCuatro3
                    val stringAlfaInferior2 =
                        if (jugadorDos3.lowercase(Locale.ROOT) < jugadorCuatro3.lowercase(Locale.ROOT)) jugadorCuatro3 else jugadorDos3
                    var equipoTwo = "$stringAlfaSuperior2$stringAlfaInferior2"

                    if (jugadorUno3 == mutableList2[0] || jugadorTres3 == mutableList2[0]) {
                        Toast.makeText(
                            requireContext(),
                            "Complete el primer campo",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        if ((jugadorDos3 == mutableList2[0] && jugadorCuatro3 != mutableList2[0]) || (jugadorDos3 != mutableList2[0] && jugadorCuatro3 == mutableList2[0])) {
                            Toast.makeText(
                                requireContext(),
                                "Complete el segundo campo",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            if (jugadorDos3 == mutableList2[0] && jugadorCuatro3 == mutableList2[0]) {
                                db.collection("doubles").document(equipoOne.lowercase(Locale.ROOT))
                                    .get().addOnSuccessListener { documentSnapshot ->
                                    if (documentSnapshot.exists()) {
                                        val partidasJugadas =
                                            documentSnapshot.getLong("Partidas Jugadas") ?: 0
                                        val partidasGanadas =
                                            documentSnapshot.getLong("Partidas Ganadas") ?: 0
                                        jugUno.text = partidasJugadas.toString()
                                        ganUno.text = partidasGanadas.toString()
                                        perdUno.text =
                                            (partidasJugadas - partidasGanadas).toString()

                                    }else{
                                        Toast.makeText(
                                            requireContext(),
                                            "Nunca jugaron juntos",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }

                            } else {
                                db.collection("doubles").document(equipoOne.lowercase(Locale.ROOT))
                                    .collection("statisticsDoubles")
                                    .document(equipoTwo.lowercase(Locale.ROOT)).get()
                                    .addOnSuccessListener { documentSnapshot ->
                                        if (documentSnapshot.exists()) {
                                            val partidasJugadas =
                                                documentSnapshot.getLong("Partidas Jugadas") ?: 0
                                            val partidasGanadas =
                                                documentSnapshot.getLong("Partidas Ganadas") ?: 0
                                            jugUno.text = partidasJugadas.toString()
                                            ganUno.text = partidasGanadas.toString()
                                            perdUno.text =
                                                (partidasJugadas - partidasGanadas).toString()
                                            jugDos.text = partidasJugadas.toString()
                                            perdDos.text = partidasGanadas.toString()
                                            ganDos.text =
                                                (partidasJugadas - partidasGanadas).toString()

                                        } else {
                                            jugUno.text = "0"
                                            ganUno.text = "0"
                                            perdUno.text = "0"
                                            jugDos.text = "0"
                                            perdDos.text = "0"
                                            ganDos.text = "0"
                                            Toast.makeText(
                                                requireContext(),
                                                "Nunca jugaron entre ellos",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                        }
                                    }

                            }
                        }

                    }
                }

            }

            /*
                                    db.collection("users").document(jugadorUno.lowercase(Locale.ROOT)).collection("statistics")
                            .document(jugadorDos.lowercase(Locale.ROOT)).get().addOnSuccessListener { documentSnapshot ->
                                if (documentSnapshot.exists()) {

             */


            btnVolver2.setOnClickListener {
                findNavController().navigate(R.id.partidaFragment)

            }

        }


    }

}