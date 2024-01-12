package com.example.puntajeburaco20.Fragments

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.ViewModels.PuntajeViewModel

lateinit var equipoUno: TextView
lateinit var equipoDos: TextView
lateinit var baseAntUno: TextView
lateinit var baseAntDos: TextView
lateinit var totalDos: TextView
 lateinit var baseUno: EditText
 lateinit var puntosUno: EditText
 lateinit var baseDos: EditText
 lateinit var puntosDos: EditText
lateinit var puntosAntUno: TextView
lateinit var puntosAntDos: TextView
lateinit var totalUno: TextView

lateinit var btnSumar: Button
lateinit var totaaaaaaaaaaaalUno: Button



class PuntajeFragment : Fragment() {

    companion object {
        fun newInstance() = PuntajeFragment()
    }

    private lateinit var viewModel: PuntajeViewModel
    var baseIngreUno: Int = 0
    var baseIngreDos: Int = 0
    var puntosIngreUno: Int = 0
    var puntosIngreDos: Int = 0

    var totalSumaUno: Int = 0
    var totalSumaDos: Int = 0

    var empiezaRonda: String = jugadorUno

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_puntaje, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        equipoUno = view.findViewById<EditText>(R.id.equipoUno)
        equipoDos = view.findViewById<EditText>(R.id.equipoDos)
        baseAntUno = view.findViewById<EditText>(R.id.baseAntUno)
        baseAntDos = view.findViewById<EditText>(R.id.baseAntDos)
        totalDos = view.findViewById<EditText>(R.id.totalDos)
        baseUno = view.findViewById<EditText>(R.id.baseUno)
        puntosUno = view.findViewById<EditText>(R.id.puntosUno)
        baseDos = view.findViewById<EditText>(R.id.baseDos)
        puntosDos = view.findViewById<EditText>(R.id.puntosDos)
        puntosAntUno = view.findViewById<EditText>(R.id.puntosAntUno)
        puntosAntDos = view.findViewById<EditText>(R.id.puntosAntDos)
        totalUno = view.findViewById<EditText>(R.id.totalUno)

        btnSumar = view.findViewById<Button>(R.id.btnSumar)

        val y = " y "
        if(cantJugadores == 2){
            equipoUno.text = jugadorUno
            equipoDos.text = jugadorDos
        }
        if(cantJugadores == 4){
            equipoUno.text = "$jugadorUno $y $jugadorDos"
            equipoDos.text = "$jugadorTres $y $jugadorCuatro"
        }
        baseAntUno.text = "0"
        baseAntDos.text = "0"
        totalDos.text = "0"
        puntosAntUno.text = "0"
        puntosAntDos.text = "0"
        totalUno.text = "0"
        Toast.makeText(requireContext(), "Comienza $empiezaRonda", Toast.LENGTH_SHORT).show()

        // btnSumar = view.findViewById<Button>(R.id.btnSumar)

        btnSumar.setOnClickListener {
            if (baseUno.text.toString() != "" &&
                baseDos.text.toString() != "" &&
                puntosUno.text.toString() != "" &&
                puntosDos.text.toString() != "") {

                baseIngreUno = (baseUno.text.toString()).toInt()
                baseIngreDos = (baseDos.text.toString()).toInt()
                puntosIngreUno = (puntosUno.text.toString()).toInt()
                puntosIngreDos = (puntosDos.text.toString()).toInt()

                baseAntUno.text = baseIngreUno.toString()
                baseAntDos.text = baseIngreDos.toString()
                puntosAntUno.text = puntosIngreUno.toString()
                puntosAntDos.text = puntosIngreDos.toString()

                totalSumaUno += baseIngreUno + puntosIngreUno
                totalSumaDos += baseIngreDos + puntosIngreDos

                totalUno.text = totalSumaUno.toString()
                totalDos.text = totalSumaDos.toString()

                baseUno.text.clear()
                baseDos.text.clear()
                puntosUno.text.clear()
                puntosDos.text.clear()

                if(cantJugadores == 2){
                    if(empiezaRonda == jugadorUno){
                        empiezaRonda = jugadorDos
                    }else {
                        if (empiezaRonda == jugadorDos) {
                            empiezaRonda = jugadorUno
                        }
                    }
                }
                if(cantJugadores == 4){
                    if(empiezaRonda == jugadorUno){
                        empiezaRonda = jugadorCuatro
                    }else {
                        if (empiezaRonda == jugadorCuatro) {
                            empiezaRonda = jugadorDos
                        } else {
                            if (empiezaRonda == jugadorDos) {
                                empiezaRonda = jugadorTres
                            } else {
                                if (empiezaRonda == jugadorTres) {
                                    empiezaRonda = jugadorUno
                                }
                            }
                        }
                    }
                }
                Toast.makeText(requireContext(), "Comienza $empiezaRonda", Toast.LENGTH_SHORT).show()

            }else{
                if (baseUno.text.toString() == "" &&
                    baseDos.text.toString() == "" &&
                    puntosUno.text.toString() == "" &&
                    puntosDos.text.toString() == ""){
                    Toast.makeText(requireContext(), "Comienza $empiezaRonda", Toast.LENGTH_SHORT).show()

                }else {
                    Toast.makeText(
                        requireContext(),
                        "Complete todos los campos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        }



    }

}