package com.example.puntajeburaco20.Fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.ViewModels.PuntajeViewModel
import com.example.puntajeburaco20.YOLODetector
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import org.opencv.core.CvType
import org.opencv.android.Utils
import org.opencv.core.MatOfPoint
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.model.Model
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream


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
lateinit var empiezaJug: TextView


lateinit var btnSumar: Button
lateinit var btnAtras: Button
lateinit var btnFin: Button
lateinit var btnCamara1: Button

var partidaTerminada: Boolean = false


class PuntajeFragment : Fragment() {

    companion object {
        fun newInstance() = PuntajeFragment()
        private const val CAMERA_REQUEST_CODE = 100
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101
        private lateinit var yoloDetector: YOLODetector
    }

    private lateinit var viewModel: PuntajeViewModel
    var baseIngreUno: Int = 0
    var baseIngreDos: Int = 0
    var puntosIngreUno: Int = 0
    var puntosIngreDos: Int = 0

    var totalSumaUno: Int = 0
    var totalSumaDos: Int = 0

    var empiezaRonda: String = ""

    var jugadorUno: String = ""
    var jugadorDos: String = ""
    var jugadorTres: String = ""
    var jugadorCuatro: String = ""
    var cantJugadores: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_puntaje, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Error al cargar OpenCV")
        } else {
            Log.d("OpenCV", "OpenCV cargado correctamente")
        }
        yoloDetector = YOLODetector(requireContext())


        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(requireContext())
                    .setTitle("Confirmación")
                    .setMessage("¿Estás seguro de que quieres salir? Se perderán todos los puntos")
                    .setPositiveButton("Sí") { _, _ ->
                        val sharedPreferences = requireActivity().getSharedPreferences("PuntajeBuracoPreferences", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()

                        // Guardar la posición del fragmento actual
                        editor.putString("posicionFragmentoActual", "PartidaFragment")// 1 podría ser una constante que represente la posición de tu fragmento específico

                        editor.putString("empiezaRonda", "")
                        editor.putString("jugadorUno", "")
                        editor.putString("jugadorDos", "")
                        editor.putString("jugadorTres", "")
                        editor.putString("jugadorCuatro", "")
                        editor.putInt("cantJugadores", 0)
                        editor.putInt("baseIngreUno", 0)
                        editor.putInt("baseIngreDos", 0)
                        editor.putInt("puntosIngreUno", 0)
                        editor.putInt("puntosIngreDos", 0)
                        editor.putInt("totalSumaUno", 0)
                        editor.putInt("totalSumaDos", 0)
                        editor.putBoolean("esconderInput", true)

                        editor.apply()
                        findNavController().popBackStack()

                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        })

        val db = Firebase.firestore

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
        empiezaJug = view.findViewById<EditText>(R.id.empiezaJug)


        btnSumar = view.findViewById<Button>(R.id.btnSumar)
        btnAtras = view.findViewById<Button>(R.id.btnAtras)
        btnFin = view.findViewById<Button>(R.id.btnFin)
        btnCamara1 = view.findViewById<Button>(R.id.btnCamara1)

        baseAntUno.text = "0"
        baseAntDos.text = "0"
        totalDos.text = "0"
        puntosAntUno.text = "0"
        puntosAntDos.text = "0"
        totalUno.text = "0"

        var sharedPreferences = requireActivity().getSharedPreferences("PuntajeBuracoPreferences", Context.MODE_PRIVATE)

        btnSumar.isEnabled = (sharedPreferences.getBoolean("esconderInput", true))
        btnFin.isEnabled = (sharedPreferences.getBoolean("esconderInput", true))
        baseUno.isEnabled = (sharedPreferences.getBoolean("esconderInput", true))
        puntosUno.isEnabled = (sharedPreferences.getBoolean("esconderInput", true))
        baseDos.isEnabled = (sharedPreferences.getBoolean("esconderInput", true))
        puntosDos.isEnabled = (sharedPreferences.getBoolean("esconderInput", true))

        empiezaRonda = sharedPreferences.getString("empiezaRonda", "") ?: ""
        empiezaJug.text = empiezaRonda
        val jugadorUnoAnt = sharedPreferences.getString("jugadorUno", "") ?: ""
        if (jugadorUnoAnt != ""){
            jugadorUno = jugadorUnoAnt
        }
        Log.d("MiApp", jugadorUno)
        Log.d("MiApp", jugadorUnoAnt)
        val jugadorDosAnt = sharedPreferences.getString("jugadorDos", "") ?: ""
        if (jugadorDosAnt != ""){
            jugadorDos = jugadorDosAnt
        }
        val jugadorTresAnt = sharedPreferences.getString("jugadorTres", "") ?: ""
        if (jugadorTresAnt != ""){
            jugadorTres = jugadorTresAnt
        }
        val jugadorCuatroAnt = sharedPreferences.getString("jugadorCuatro", "") ?: ""
        if (jugadorCuatroAnt != ""){
            jugadorCuatro = jugadorCuatroAnt
        }
        val cantJugadoresAnt = sharedPreferences.getInt("cantJugadores", 0)
        if (cantJugadoresAnt != 0){
            cantJugadores = cantJugadoresAnt
        }


        baseIngreUno = sharedPreferences.getInt("baseIngreUno", 0)
        baseIngreDos = sharedPreferences.getInt("baseIngreDos", 0)
        puntosIngreUno = sharedPreferences.getInt("puntosIngreUno", 0)
        puntosIngreDos = sharedPreferences.getInt("puntosIngreDos", 0)
        totalSumaUno = sharedPreferences.getInt("totalSumaUno", 0)
        totalSumaDos = sharedPreferences.getInt("totalSumaDos", 0)
        baseAntUno.text = baseIngreUno.toString()
        baseAntDos.text = baseIngreDos.toString()
        puntosAntUno.text = puntosIngreUno.toString()
        puntosAntDos.text = puntosIngreDos.toString()
        totalUno.text = totalSumaUno.toString()
        totalDos.text = totalSumaDos.toString()

        if(empiezaRonda == ""){
            empiezaRonda = jugadorUno
            empiezaJug.text = empiezaRonda
        }
        empiezaJug.text = empiezaRonda
        val y = " y "
        if(cantJugadores == 2){
            equipoUno.text = jugadorUno
            equipoDos.text = jugadorDos
        }
        if(cantJugadores == 4){
            equipoUno.text = "$jugadorUno $y $jugadorDos"
            equipoDos.text = "$jugadorTres $y $jugadorCuatro"
        }

      /*  val editor = sharedPreferences.edit()

        editor.putString("jugadorUno", jugadorUno)
        editor.putString("jugadorDos", jugadorDos)
        editor.putString("jugadorTres", jugadorTres)
        editor.putString("jugadorCuatro", jugadorCuatro)
        editor.putInt("cantJugadores", cantJugadores)


        editor.apply()*/
        if(sharedPreferences.getBoolean("esconderInput", true)) {
            Toast.makeText(requireContext(), "Comienza $empiezaRonda", Toast.LENGTH_SHORT).show()
        }

        //copyTessDataIfNeeded(requireContext())

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
                Log.d("MiApp", jugadorUno)
                Log.d("MiApp", jugadorUnoAnt)

                if(cantJugadores == 2){
                    if(empiezaRonda == jugadorUno){
                        empiezaRonda = jugadorDos
                        empiezaJug.text = empiezaRonda
                    }else {
                        if (empiezaRonda == jugadorDos) {
                            empiezaRonda = jugadorUno
                            empiezaJug.text = empiezaRonda
                        }
                    }
                }
                if(cantJugadores == 4){
                    if(empiezaRonda == jugadorUno){
                        empiezaRonda = jugadorCuatro
                        empiezaJug.text = empiezaRonda
                    }else {
                        if (empiezaRonda == jugadorCuatro) {
                            empiezaRonda = jugadorDos
                            empiezaJug.text = empiezaRonda
                        } else {
                            if (empiezaRonda == jugadorDos) {
                                empiezaRonda = jugadorTres
                                empiezaJug.text = empiezaRonda
                            } else {
                                if (empiezaRonda == jugadorTres) {
                                    empiezaRonda = jugadorUno
                                    empiezaJug.text = empiezaRonda
                                }
                            }
                        }
                    }
                }



                Toast.makeText(requireContext(), "Comienza $empiezaRonda", Toast.LENGTH_SHORT).show()


                val editor = sharedPreferences.edit()

                editor.putString("empiezaRonda", empiezaRonda)
             /*   editor.putString("jugadorUno", jugadorUno)
                editor.putString("jugadorDos", jugadorDos)
                editor.putString("jugadorTres", jugadorTres)
                editor.putString("jugadorCuatro", jugadorCuatro)
                editor.putInt("cantJugadores", cantJugadores)
*/
                editor.putInt("baseIngreUno", baseIngreUno)
                editor.putInt("baseIngreDos", baseIngreDos)
                editor.putInt("puntosIngreUno", puntosIngreUno)
                editor.putInt("puntosIngreDos", puntosIngreDos)
                editor.putInt("totalSumaUno", totalSumaUno)
                editor.putInt("totalSumaDos", totalSumaDos)

                editor.apply()


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

        btnFin.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Fin de la partida")
                .setMessage("Eliga al ganador")
                .setPositiveButton(equipoDos.text) { _, _ ->
                    partidaTerminada = true

                    btnSumar.isEnabled = false
                    btnFin.isEnabled = false
                    baseUno.isEnabled = false
                    puntosUno.isEnabled = false
                    baseDos.isEnabled = false
                    puntosDos.isEnabled = false
                    val sharedPreferences = requireActivity().getSharedPreferences("PuntajeBuracoPreferences", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()

                    editor.putBoolean("esconderInput", false)

                    editor.apply()

                    if (cantJugadores == 2) {

                        db.collection("users").document(jugadorDos.lowercase(Locale.ROOT)).collection("statistics")
                            .document(jugadorUno.lowercase(Locale.ROOT)).get().addOnSuccessListener { documentSnapshot ->
                                if (documentSnapshot.exists()) {

                                    val partidasJugadasActual = documentSnapshot.getLong("Partidas Jugadas") ?: 0

                                    val nuevasPartidasJugadas = partidasJugadasActual + 1

                                    val partidasGanadasActual = documentSnapshot.getLong("Partidas Ganadas") ?: 0

                                    val nuevasPartidasGanadas = partidasGanadasActual + 1

                                    db.collection("users").document(jugadorDos.lowercase(Locale.ROOT)).collection("statistics")
                                        .document(jugadorUno.lowercase(Locale.ROOT)).update("Partidas Jugadas", nuevasPartidasJugadas, "Partidas Ganadas", nuevasPartidasGanadas)
                                        .addOnSuccessListener {

                                        }
                                        .addOnFailureListener { e ->
                                        }
                                    db.collection("users").document(jugadorUno.lowercase(Locale.ROOT)).collection("statistics")
                                        .document(jugadorDos.lowercase(Locale.ROOT)).update("Partidas Jugadas", nuevasPartidasJugadas)
                                        .addOnSuccessListener {

                                        }
                                        .addOnFailureListener { e ->
                                        }

                                } else {
                                    val nuevoDocumento = hashMapOf(
                                        "Partidas Jugadas" to 1,
                                        "Partidas Ganadas" to 1
                                    )

                                    db.collection("users").document(jugadorDos.lowercase(Locale.ROOT)).collection("statistics")
                                        .document(jugadorUno.lowercase(Locale.ROOT)).set(nuevoDocumento)
                                        .addOnSuccessListener {
                                        }
                                        .addOnFailureListener { e ->
                                        }

                                    val nuevoDocumento2 = hashMapOf(
                                        "Partidas Jugadas" to 1
                                    )

                                    db.collection("users").document(jugadorUno.lowercase(Locale.ROOT)).collection("statistics")
                                        .document(jugadorDos.lowercase(Locale.ROOT)).set(nuevoDocumento2)
                                        .addOnSuccessListener {
                                        }
                                        .addOnFailureListener { e ->
                                        }
                                }
                            }

                        db.collection("users").document(jugadorDos.lowercase(Locale.ROOT)).get().addOnSuccessListener { documentSnapshot ->
                            if (documentSnapshot.exists()) {

                                val partidasJugadasActualEquipo =
                                    documentSnapshot.getLong("Partidas Jugadas") ?: 0

                                val nuevasPartidasJugadasEquipo = partidasJugadasActualEquipo + 1

                                val partidasGanadasActualEquipo =
                                    documentSnapshot.getLong("Partidas Ganadas") ?: 0

                                val nuevasPartidasGanadasEquipo = partidasGanadasActualEquipo + 1

                                db.collection("users")
                                    .document(jugadorDos.lowercase(Locale.ROOT))
                                    .update(
                                        "Partidas Jugadas",
                                        nuevasPartidasJugadasEquipo,
                                        "Partidas Ganadas",
                                        nuevasPartidasGanadasEquipo
                                    )
                                    .addOnSuccessListener {

                                    }
                                    .addOnFailureListener { e ->
                                    }
                            }
                        }

                        db.collection("users").document(jugadorUno.lowercase(Locale.ROOT)).get().addOnSuccessListener { documentSnapshot ->
                            if (documentSnapshot.exists()) {

                                val partidasJugadasActualEquipo2 =
                                    documentSnapshot.getLong("Partidas Jugadas") ?: 0

                                val nuevasPartidasJugadasEquipo2 = partidasJugadasActualEquipo2 + 1


                                db.collection("users")
                                    .document(jugadorUno.lowercase(Locale.ROOT))
                                    .update(
                                        "Partidas Jugadas",
                                        nuevasPartidasJugadasEquipo2
                                    )
                                    .addOnSuccessListener {

                                    }
                                    .addOnFailureListener { e ->
                                    }
                            }
                        }


                }
                    if(cantJugadores == 4){
                        val stringAlfaSuperior = if (jugadorUno.lowercase(Locale.ROOT) < jugadorDos.lowercase(Locale.ROOT)) jugadorUno else jugadorDos
                        val stringAlfaInferior = if (jugadorUno.lowercase(Locale.ROOT) < jugadorDos.lowercase(Locale.ROOT)) jugadorDos else jugadorUno
                        var equipoOne = "$stringAlfaSuperior$stringAlfaInferior"

                        val stringAlfaSuperior2 = if (jugadorTres.lowercase(Locale.ROOT) < jugadorCuatro.lowercase(Locale.ROOT)) jugadorTres else jugadorCuatro
                        val stringAlfaInferior2 = if (jugadorTres.lowercase(Locale.ROOT) < jugadorCuatro.lowercase(Locale.ROOT)) jugadorCuatro else jugadorTres
                        var equipoTwo = "$stringAlfaSuperior2$stringAlfaInferior2"


                        db.collection("doubles").document(equipoTwo.lowercase(Locale.ROOT)).collection("statisticsDoubles")
                            .document(equipoOne.lowercase(Locale.ROOT)).get().addOnSuccessListener { documentSnapshot ->
                                if (documentSnapshot.exists()) {

                                    val partidasJugadasActual = documentSnapshot.getLong("Partidas Jugadas") ?: 0

                                    val nuevasPartidasJugadas = partidasJugadasActual + 1

                                    val partidasGanadasActual = documentSnapshot.getLong("Partidas Ganadas") ?: 0

                                    val nuevasPartidasGanadas = partidasGanadasActual + 1

                                    db.collection("doubles").document(equipoTwo.lowercase(Locale.ROOT)).collection("statisticsDoubles")
                                        .document(equipoOne.lowercase(Locale.ROOT)).update("Partidas Jugadas", nuevasPartidasJugadas, "Partidas Ganadas", nuevasPartidasGanadas)
                                        .addOnSuccessListener {

                                        }
                                        .addOnFailureListener { e ->
                                        }
                                    db.collection("doubles").document(equipoOne.lowercase(Locale.ROOT)).collection("statisticsDoubles")
                                        .document(equipoTwo.lowercase(Locale.ROOT)).update("Partidas Jugadas", nuevasPartidasJugadas)
                                        .addOnSuccessListener {

                                        }
                                        .addOnFailureListener { e ->
                                        }

                                } else {
                                    val nuevoDocumento = hashMapOf(
                                        "Partidas Jugadas" to 1,
                                        "Partidas Ganadas" to 1
                                    )

                                    db.collection("doubles").document(equipoTwo.lowercase(Locale.ROOT)).collection("statisticsDoubles")
                                        .document(equipoOne.lowercase(Locale.ROOT)).set(nuevoDocumento)
                                        .addOnSuccessListener {
                                        }
                                        .addOnFailureListener { e ->
                                        }

                                    val nuevoDocumento2 = hashMapOf(
                                        "Partidas Jugadas" to 1
                                    )

                                    db.collection("doubles").document(equipoOne.lowercase(Locale.ROOT)).collection("statisticsDoubles")
                                        .document(equipoTwo.lowercase(Locale.ROOT)).set(nuevoDocumento2)
                                        .addOnSuccessListener {
                                        }
                                        .addOnFailureListener { e ->
                                        }
                                }
                            }

                        val dataToUpdate = hashMapOf(
                            "Partidas Jugadas" to FieldValue.increment(1),
                            "Partidas Ganadas" to FieldValue.increment(1)
                        )

                        db.collection("doubles")
                            .document(equipoTwo.lowercase(Locale.ROOT))
                            .set(dataToUpdate, SetOptions.merge())
                            .addOnSuccessListener {
                                // Éxito al actualizar o crear el documento
                            }
                            .addOnFailureListener { e ->
                                // Manejar el fallo
                            }


                        val dataToUpdate2 = hashMapOf(
                            "Partidas Jugadas" to FieldValue.increment(1),
                        )

                        db.collection("doubles")
                            .document(equipoOne.lowercase(Locale.ROOT))
                            .set(dataToUpdate2, SetOptions.merge())
                            .addOnSuccessListener {
                                // Éxito al actualizar o crear el documento
                            }
                            .addOnFailureListener { e ->
                                // Manejar el fallo
                            }



                    }
                }
                .setNegativeButton(equipoUno.text) { _, _ ->
                    partidaTerminada = true

                    btnSumar.isEnabled = false
                    btnFin.isEnabled = false
                    baseUno.isEnabled = false
                    puntosUno.isEnabled = false
                    baseDos.isEnabled = false
                    puntosDos.isEnabled = false

                    val sharedPreferences = requireActivity().getSharedPreferences("PuntajeBuracoPreferences", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()

                    editor.putBoolean("esconderInput", false)

                    editor.apply()

                    if (cantJugadores == 2) {

                        db.collection("users").document(jugadorUno.lowercase(Locale.ROOT)).collection("statistics")
                            .document(jugadorDos.lowercase(Locale.ROOT)).get().addOnSuccessListener { documentSnapshot ->
                                if (documentSnapshot.exists()) {

                                    val partidasJugadasActual = documentSnapshot.getLong("Partidas Jugadas") ?: 0

                                    val nuevasPartidasJugadas = partidasJugadasActual + 1

                                    val partidasGanadasActual = documentSnapshot.getLong("Partidas Ganadas") ?: 0

                                    val nuevasPartidasGanadas = partidasGanadasActual + 1

                                    db.collection("users").document(jugadorUno.lowercase(Locale.ROOT)).collection("statistics")
                                        .document(jugadorDos.lowercase(Locale.ROOT)).update("Partidas Jugadas", nuevasPartidasJugadas, "Partidas Ganadas", nuevasPartidasGanadas)
                                        .addOnSuccessListener {

                                        }
                                        .addOnFailureListener { e ->
                                        }
                                    db.collection("users").document(jugadorDos.lowercase(Locale.ROOT)).collection("statistics")
                                        .document(jugadorUno.lowercase(Locale.ROOT)).update("Partidas Jugadas", nuevasPartidasJugadas)
                                        .addOnSuccessListener {

                                        }
                                        .addOnFailureListener { e ->
                                        }

                                } else {
                                    val nuevoDocumento = hashMapOf(
                                        "Partidas Jugadas" to 1,
                                        "Partidas Ganadas" to 1
                                        )

                                    db.collection("users").document(jugadorUno.lowercase(Locale.ROOT)).collection("statistics")
                                        .document(jugadorDos.lowercase(Locale.ROOT)).set(nuevoDocumento)
                                        .addOnSuccessListener {
                                        }
                                        .addOnFailureListener { e ->
                                        }

                                    val nuevoDocumento2 = hashMapOf(
                                        "Partidas Jugadas" to 1
                                    )

                                    db.collection("users").document(jugadorDos.lowercase(Locale.ROOT)).collection("statistics")
                                        .document(jugadorUno.lowercase(Locale.ROOT)).set(nuevoDocumento2)
                                        .addOnSuccessListener {
                                        }
                                        .addOnFailureListener { e ->
                                        }
                                }
                            }


                        db.collection("users").document(jugadorUno.lowercase(Locale.ROOT)).get().addOnSuccessListener { documentSnapshot ->
                            if (documentSnapshot.exists()) {

                                val partidasJugadasActualEquipo =
                                    documentSnapshot.getLong("Partidas Jugadas") ?: 0

                                val nuevasPartidasJugadasEquipo = partidasJugadasActualEquipo + 1

                                val partidasGanadasActualEquipo =
                                    documentSnapshot.getLong("Partidas Ganadas") ?: 0

                                val nuevasPartidasGanadasEquipo = partidasGanadasActualEquipo + 1

                                db.collection("users")
                                    .document(jugadorUno.lowercase(Locale.ROOT))
                                    .update(
                                        "Partidas Jugadas",
                                        nuevasPartidasJugadasEquipo,
                                        "Partidas Ganadas",
                                        nuevasPartidasGanadasEquipo
                                    )
                                    .addOnSuccessListener {

                                    }
                                    .addOnFailureListener { e ->
                                    }
                            }
                        }

                        db.collection("users").document(jugadorDos.lowercase(Locale.ROOT)).get().addOnSuccessListener { documentSnapshot ->
                            if (documentSnapshot.exists()) {

                                val partidasJugadasActualEquipo2 =
                                    documentSnapshot.getLong("Partidas Jugadas") ?: 0

                                val nuevasPartidasJugadasEquipo2 = partidasJugadasActualEquipo2 + 1


                                db.collection("users")
                                    .document(jugadorDos.lowercase(Locale.ROOT))
                                    .update(
                                        "Partidas Jugadas",
                                        nuevasPartidasJugadasEquipo2
                                    )
                                    .addOnSuccessListener {

                                    }
                                    .addOnFailureListener { e ->
                                    }
                            }
                        }


                    }

                    if(cantJugadores == 4){
                        val stringAlfaSuperior = if (jugadorUno.lowercase(Locale.ROOT) < jugadorDos.lowercase(Locale.ROOT)) jugadorUno else jugadorDos
                        val stringAlfaInferior = if (jugadorUno.lowercase(Locale.ROOT) < jugadorDos.lowercase(Locale.ROOT)) jugadorDos else jugadorUno
                        var equipoOne = "$stringAlfaSuperior$stringAlfaInferior"

                        val stringAlfaSuperior2 = if (jugadorTres.lowercase(Locale.ROOT) < jugadorCuatro.lowercase(Locale.ROOT)) jugadorTres else jugadorCuatro
                        val stringAlfaInferior2 = if (jugadorTres.lowercase(Locale.ROOT) < jugadorCuatro.lowercase(Locale.ROOT)) jugadorCuatro else jugadorTres
                        var equipoTwo = "$stringAlfaSuperior2$stringAlfaInferior2"


                        db.collection("doubles").document(equipoOne.lowercase(Locale.ROOT)).collection("statisticsDoubles")
                            .document(equipoTwo.lowercase(Locale.ROOT)).get().addOnSuccessListener { documentSnapshot ->
                                if (documentSnapshot.exists()) {

                                    val partidasJugadasActual = documentSnapshot.getLong("Partidas Jugadas") ?: 0

                                    val nuevasPartidasJugadas = partidasJugadasActual + 1

                                    val partidasGanadasActual = documentSnapshot.getLong("Partidas Ganadas") ?: 0

                                    val nuevasPartidasGanadas = partidasGanadasActual + 1

                                    db.collection("doubles").document(equipoOne.lowercase(Locale.ROOT)).collection("statisticsDoubles")
                                        .document(equipoTwo.lowercase(Locale.ROOT)).update("Partidas Jugadas", nuevasPartidasJugadas, "Partidas Ganadas", nuevasPartidasGanadas)
                                        .addOnSuccessListener {

                                        }
                                        .addOnFailureListener { e ->
                                        }
                                    db.collection("doubles").document(equipoTwo.lowercase(Locale.ROOT)).collection("statisticsDoubles")
                                        .document(equipoOne.lowercase(Locale.ROOT)).update("Partidas Jugadas", nuevasPartidasJugadas)
                                        .addOnSuccessListener {

                                        }
                                        .addOnFailureListener { e ->
                                        }

                                } else {
                                    val nuevoDocumento = hashMapOf(
                                        "Partidas Jugadas" to 1,
                                        "Partidas Ganadas" to 1
                                    )

                                    db.collection("doubles").document(equipoOne.lowercase(Locale.ROOT)).collection("statisticsDoubles")
                                        .document(equipoTwo.lowercase(Locale.ROOT)).set(nuevoDocumento)
                                        .addOnSuccessListener {
                                        }
                                        .addOnFailureListener { e ->
                                        }

                                    val nuevoDocumento2 = hashMapOf(
                                        "Partidas Jugadas" to 1
                                    )

                                    db.collection("doubles").document(equipoTwo.lowercase(Locale.ROOT)).collection("statisticsDoubles")
                                        .document(equipoOne.lowercase(Locale.ROOT)).set(nuevoDocumento2)
                                        .addOnSuccessListener {
                                        }
                                        .addOnFailureListener { e ->
                                        }
                                }
                            }


                        val dataToUpdate = hashMapOf(
                            "Partidas Jugadas" to FieldValue.increment(1),
                            "Partidas Ganadas" to FieldValue.increment(1)
                        )

                        db.collection("doubles")
                            .document(equipoOne.lowercase(Locale.ROOT))
                            .set(dataToUpdate, SetOptions.merge())
                            .addOnSuccessListener {
                                // Éxito al actualizar o crear el documento
                            }
                            .addOnFailureListener { e ->
                                // Manejar el fallo
                            }


                        val dataToUpdate2 = hashMapOf(
                            "Partidas Jugadas" to FieldValue.increment(1),
                        )

                        db.collection("doubles")
                            .document(equipoTwo.lowercase(Locale.ROOT))
                            .set(dataToUpdate2, SetOptions.merge())
                            .addOnSuccessListener {
                                // Éxito al actualizar o crear el documento
                            }
                            .addOnFailureListener { e ->
                                // Manejar el fallo
                            }


                    }
                }
                .setNeutralButton("Cancelar",null)
                .show()
        }

        btnAtras.setOnClickListener {
            val mensaje = if (partidaTerminada) {
                "Los puntos ya fueron guardados. ¿Estás seguro de que quieres salir?"
            } else {
                "¿Estás seguro de que quieres salir? Se perderán todos los puntos"
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Confirmación")
                .setMessage(mensaje)
                .setPositiveButton("Sí") { _, _ ->
                    val sharedPreferences = requireActivity().getSharedPreferences("PuntajeBuracoPreferences", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()

                    // Guardar la posición del fragmento actual
                    editor.putString("posicionFragmentoActual", "PartidaFragment")// 1 podría ser una constante que represente la posición de tu fragmento específico

                    editor.putString("empiezaRonda", "")
                    editor.putString("jugadorUno", "")
                    editor.putString("jugadorDos", "")
                    editor.putString("jugadorTres", "")
                    editor.putString("jugadorCuatro", "")
                    editor.putInt("cantJugadores", 0)
                    editor.putInt("baseIngreUno", 0)
                    editor.putInt("baseIngreDos", 0)
                    editor.putInt("puntosIngreUno", 0)
                    editor.putInt("puntosIngreDos", 0)
                    editor.putInt("totalSumaUno", 0)
                    editor.putInt("totalSumaDos", 0)
                    editor.putBoolean("esconderInput", true)

                    editor.apply()
                    findNavController().popBackStack()

                }
                .setNegativeButton("No", null)
                .show()
        }

        btnCamara1.setOnClickListener {
            openCamera()

        }

    }
    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                // Ejecutar detección en segundo plano para no congelar la UI
                Thread {
                    val detections = yoloDetector.detectObjects(imageBitmap)
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Detectados: $detections", Toast.LENGTH_LONG).show()
                    }
                }.start()
            } else {
                Toast.makeText(requireContext(), "Error al capturar imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }
    /*
    fun preprocessImage(imageBitmap: Bitmap): Bitmap {
        // Convertir Bitmap a Mat
        val mat = Mat()
        Utils.bitmapToMat(imageBitmap, mat)

        // Convertir a escala de grises
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGBA2GRAY)

        // Aplicar desenfoque gaussiano para reducir ruido
        Imgproc.GaussianBlur(grayMat, grayMat, Size(5.0, 5.0), 0.0)

        // Aplicar binarización (umbral adaptativo)
        val thresholdMat = Mat()
        Imgproc.adaptiveThreshold(
            grayMat, thresholdMat, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY, 11, 2.0
        )

        // Convertir Mat de nuevo a Bitmap
        val processedBitmap = Bitmap.createBitmap(thresholdMat.cols(), thresholdMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(thresholdMat, processedBitmap)

        return processedBitmap
    }
    */

/*
    private fun recognizeTextWithMLKit(imageBitmap: Bitmap) {
        val image = InputImage.fromBitmap(imageBitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val recognizedNumbers = mutableListOf<String>()

                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val text = line.text
                        val onlyNumbers = text.filter { it.isDigit() } // Filtrar solo números
                        if (onlyNumbers.isNotEmpty()) {
                            recognizedNumbers.add(onlyNumbers)
                        }
                    }
                }

                if (recognizedNumbers.isEmpty()) {
                    Toast.makeText(requireContext(), "No se detectaron números", Toast.LENGTH_SHORT).show()
                } else {
                    val numberCounts = recognizedNumbers.groupingBy { it }.eachCount()
                    println("Cantidad de veces que aparece cada número: $numberCounts")
                    Toast.makeText(requireContext(), "Números detectados: $numberCounts", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error en reconocimiento: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    */
    /*
    private fun openCamera() {
        // Verificar si se tienen los permisos necesarios antes de abrir la cámara
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
        } else {
            // Si no se tienen los permisos, solicitarlos
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }
    // Método para recibir la imagen después de tomar la foto
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                processImage(imageBitmap) // Pasa la imagen a la función para procesarla
            } else {
                Toast.makeText(requireContext(), "Error al capturar imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // Este método se llama cuando el usuario responde a la solicitud de permiso
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Función para convertir la imagen a escala de grises
    fun convertToGray(imageBitmap: Bitmap): Mat {
        val mat = Mat()
        Utils.bitmapToMat(imageBitmap, mat)
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY)
        return grayMat
    }

    // Función para aplicar un filtro GaussianBlur
    fun applyGaussianBlur(grayMat: Mat): Mat {
        val blurredMat = Mat()
        Imgproc.GaussianBlur(grayMat, blurredMat, Size(5.0, 5.0), 0.0)
        return blurredMat
    }

    // Función para detectar contornos
    fun detectContours(blurredMat: Mat): List<MatOfPoint> {
        val edges = Mat()
        Imgproc.Canny(blurredMat, edges, 100.0, 200.0)
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)
        return contours
    }

    // Función para reconocer texto usando Tesseract
    fun recognizeText(imageMat: Mat): String {
        val tessBaseApi = TessBaseAPI()
        val tessDataPath = requireContext().getExternalFilesDir(null)?.absolutePath ?: ""
        tessBaseApi.init(tessDataPath, "eng") // Asegura que la ruta es correcta
        tessBaseApi.setVariable("tessedit_char_whitelist", "0123456789")
        val bitmap = Bitmap.createBitmap(imageMat.cols(), imageMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(imageMat, bitmap)
        tessBaseApi.setImage(bitmap)
        val recognizedText = tessBaseApi.utF8Text
        tessBaseApi.end()
        return recognizedText
    }

    // Función para mostrar resultados y contar números
    fun processImage(imageBitmap: Bitmap) {
        val grayMat = convertToGray(imageBitmap)
        val blurredMat = applyGaussianBlur(grayMat)
        val contours = detectContours(blurredMat)
        val recognizedNumbers = mutableListOf<String>()

        for (contour in contours) {
            val boundingRect = Imgproc.boundingRect(contour)
            val numberMat = grayMat.submat(boundingRect)
            val text = recognizeText(numberMat)
            recognizedNumbers.add(text)
        }
        println("Cantidad de veces: $recognizedNumbers")
        val numberCounts = recognizedNumbers.groupingBy { it }.eachCount()
        println("Cantidad de veces que aparece cada número: $numberCounts")
    }



    fun copyTessDataIfNeeded(context: Context) {
        val tessDataDir = File(context.getExternalFilesDir(null), "tessdata")
        if (!tessDataDir.exists()) {
            tessDataDir.mkdirs()
        }

        val tessDataFile = File(tessDataDir, "eng.traineddata")
        if (!tessDataFile.exists()) {
            try {
                context.assets.open("tessdata/eng.traineddata").use { inputStream ->
                    FileOutputStream(tessDataFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
*/

}