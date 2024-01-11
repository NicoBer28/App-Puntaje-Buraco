package com.example.puntajeburaco20.ViewModels

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.puntajeburaco20.R


lateinit var botonNuevaPartida: Button
lateinit var botonHistoriales: Button
lateinit var botonNuevoUsuario: Button

lateinit var jugadorUno: String
lateinit var jugadorDos: String
var jugadorTres: String = ""
var jugadorCuatro: String = ""


/**
 * A simple [Fragment] subclass.
 * Use the [PartidaFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PartidaFragment : Fragment() {

    private val mutableList: MutableList<String> = mutableListOf("Nombre", "Nico", "Sergio", "Vero", "Juli", "Mati", "Bobe", "Invitado 1", "Invitado 2", "Invitado 3", "Invitado 4")


    private val mutableListCopia: MutableList<String> = mutableListOf("Jugador 1", "nico", "sergio", "vero")

    private val mutableListJugadores: MutableList<String> = mutableListOf("2 Jugadores", "4 Jugadores")
    private var primerNombre3: Int = 0
    private var primerNombre4: Int = 0

    /*mutableList.add("Damasco")
mutableList.removeAt(1)*/

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spinner1: Spinner = view.findViewById(R.id.spinner1)
        val spinner2: Spinner = view.findViewById(R.id.spinner2)
        val spinner3: Spinner = view.findViewById(R.id.spinner3)
        val spinner4: Spinner = view.findViewById(R.id.spinner4)
        val spinnerJugadores: Spinner = view.findViewById(R.id.spinnerJugadores)
        var opcionAnterior1 = mutableList[0]
        var opcionAnterior2 = mutableList[0]
        var opcionAnterior3 = mutableList[0]
        var opcionAnterior4 = mutableList[0]

        botonNuevaPartida = view.findViewById<Button>(R.id.btnNuevaPartida)
        botonHistoriales = view.findViewById<Button>(R.id.btnHistoriales)
        botonNuevoUsuario = view.findViewById<Button>(R.id.btnUsuario)


        val adapterJugadores = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            mutableListJugadores
        )
        adapterJugadores.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerJugadores.adapter = adapterJugadores

        spinnerJugadores.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
                        if(jugadorTres == mutableList[0]){
                            jugadorTres = ""
                        }
                        if(jugadorCuatro == mutableList[0]){
                            jugadorCuatro = ""
                        }
                        spinner1.setBackgroundColor(Color.MAGENTA)
                    }

                    1 -> { // Si se selecciona la segunda opción
                        spinner3.visibility = View.VISIBLE
                        spinner4.visibility = View.VISIBLE
                        if(jugadorTres == ""){
                            jugadorTres = mutableList[0]
                        }
                        if(jugadorCuatro == ""){
                            jugadorCuatro = mutableList[0]
                        }
                        spinner1.setBackgroundColor(Color.CYAN)
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
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableList)
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner1.adapter = adapter1

        spinner1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                jugadorUno = parent.getItemAtPosition(position).toString()
                if (opcionAnterior1 != mutableList[0]) {
                    mutableList.add(opcionAnterior1)
                }
                opcionAnterior1 = parent.getItemAtPosition(position).toString()

                if (opcionAnterior1 != mutableList[0]) {
                    mutableList.removeAt(position)
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Método requerido, pero puedes dejarlo vacío si no necesitas manejar este caso
            }
        }
        //-------------------
        val adapter2 =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableList)
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner2.adapter = adapter2

        spinner2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                jugadorDos = parent.getItemAtPosition(position).toString()

                if (opcionAnterior2 != mutableList[0]) {
                    mutableList.add(opcionAnterior2)
                }
                opcionAnterior2 = parent.getItemAtPosition(position).toString()
                if (opcionAnterior2 != mutableList[0]) {
                    mutableList.removeAt(position)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Método requerido, pero puedes dejarlo vacío si no necesitas manejar este caso
            }
        }
        //-------------------
        val adapter3 =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableList)
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner3.adapter = adapter3

        spinner3.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if(primerNombre3 == 1) {
                    jugadorTres = parent.getItemAtPosition(position).toString()
                }
                if(primerNombre3 == 0) {
                    primerNombre3 = 1
                }

                if (opcionAnterior3 != mutableList[0]) {
                    mutableList.add(opcionAnterior3)
                }
                opcionAnterior3 = parent.getItemAtPosition(position).toString()
                if (opcionAnterior3 != mutableList[0]) {
                    mutableList.removeAt(position)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Método requerido, pero puedes dejarlo vacío si no necesitas manejar este caso
            }
        }
        //-------------------
        val adapter4 =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableList)
        adapter4.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner4.adapter = adapter4

        spinner4.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if(primerNombre4 == 1) {
                    jugadorCuatro = parent.getItemAtPosition(position).toString()
                }
                if(primerNombre4 == 0) {
                    primerNombre4 = 1
                }
                if (opcionAnterior4 != mutableList[0]) {
                    mutableList.add(opcionAnterior4)
                }
                opcionAnterior4 = parent.getItemAtPosition(position).toString()
                if (opcionAnterior4 != mutableList[0]) {
                    mutableList.removeAt(position)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Método requerido, pero puedes dejarlo vacío si no necesitas manejar este caso
            }
        }




        botonNuevaPartida.setOnClickListener {
            if(jugadorUno == mutableList[0] || jugadorDos == mutableList[0] || jugadorTres == mutableList[0] || jugadorCuatro == mutableList[0]){
                Toast.makeText(requireContext(), "Elija un jugador", Toast.LENGTH_SHORT).show()
            }

        }

        botonNuevoUsuario.setOnClickListener {

        }

        botonHistoriales.setOnClickListener {

        }

    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_partida, container, false)
    }

    companion object {

    }
}
