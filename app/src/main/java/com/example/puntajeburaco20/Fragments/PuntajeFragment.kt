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
import com.example.puntajeburaco20.OverlayView
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
private lateinit var btnSumarEq1: Button
private lateinit var btnSumarEq2: Button
private lateinit var btnCerrarCamara: Button


var partidaTerminada: Boolean = false


class PuntajeFragment : Fragment() {

    companion object {
        fun newInstance() = PuntajeFragment()
        private const val CAMERA_REQUEST_CODE = 100
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101
        private lateinit var yoloDetector: YOLODetector
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    // 1. Agregá esta variable global en la clase
    private lateinit var viewFinder: androidx.camera.view.PreviewView
    // Executor para correr el análisis en segundo plano (para que no se trabe la UI)
    private lateinit var cameraExecutor: java.util.concurrent.ExecutorService
    private lateinit var overlay: OverlayView
    // Variable volátil para guardar lo que está viendo la cámara en este instante
    private var ultimasDetecciones: List<YOLODetector.BoundingBox> = emptyList()
    private lateinit var cameraContainer: androidx.constraintlayout.widget.ConstraintLayout

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
        btnSumarEq1 = view.findViewById(R.id.btnSumarEq1)
        btnSumarEq2 = view.findViewById(R.id.btnSumarEq2)
        btnCerrarCamara = view.findViewById(R.id.btnCerrarCamara)

        cameraContainer = view.findViewById(R.id.cameraContainer)

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
        btnSumarEq1.text = "Sumar a ${equipoUno.text}"
        btnSumarEq2.text = "Sumar a ${equipoDos.text}"

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
        viewFinder = view.findViewById(R.id.viewFinder)
        cameraExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
        overlay = view.findViewById(R.id.overlay)
        btnCamara1.setOnClickListener {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
            }
        }

        btnSumarEq1.setOnClickListener {
            sumarDeteccionesYSalir(puntosUno)
        }

        btnSumarEq2.setOnClickListener {
            sumarDeteccionesYSalir(puntosDos)
        }

        btnCerrarCamara.setOnClickListener {
            detenerCamara()
            Toast.makeText(requireContext(), "Cancelado", Toast.LENGTH_SHORT).show()
        }

    }

    // 3. La función que inicia la cámara
    private fun startCamera() {
        val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Vinculamos el ciclo de vida
            val cameraProvider = cameraProviderFuture.get()

            // Preview: Lo que ves en pantalla
            val preview = androidx.camera.core.Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            // ImageAnalysis: Acá es donde YOLO trabaja
            val imageAnalyzer = androidx.camera.core.ImageAnalysis.Builder()
                // ESTRATEGIA: Si el modelo es lento, descarta frames viejos. Solo analiza lo último.
                .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888) // Pide formato Bitmap friendly
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        procesarImagen(imageProxy)
                    }
                }

            // Seleccionar cámara trasera
            val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll() // Desconectar todo antes de reconectar
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

                // Hacemos visible el visor
                activity?.runOnUiThread {
                    cameraContainer.visibility = View.VISIBLE
                }

            } catch (exc: Exception) {
                Log.e("CameraX", "Error al iniciar cámara", exc)
            }

        }, androidx.core.content.ContextCompat.getMainExecutor(requireContext()))
    }

    // 4. El puente entre CameraX y tu YOLO
    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun procesarImagen(imageProxy: androidx.camera.core.ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            // 1. Obtener la rotación necesaria (suele ser 90° o 270° en portrait)
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees.toFloat()

            // 2. Convertir a Bitmap
            val rawBitmap = imageProxy.toBitmap()

            // 3. Corregir la rotación usando una Matrix
            val matrix = android.graphics.Matrix()
            matrix.postRotate(rotationDegrees)

            // 4. Crear el Bitmap rotado (y aquí es donde entra YOLO cómodo)
            val rotatedBitmap = Bitmap.createBitmap(
                rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
            )

            // 5. Pasar el bitmap DERECHO al detector
            val detections = yoloDetector.detectObjects(rotatedBitmap)

            if (!detections.isNullOrEmpty()) {
                ultimasDetecciones = detections
                val maxConf = detections.maxOf { it.cnf }
                val detectedClasses = detections.joinToString { "${it.clsName} (${String.format("%.2f", it.cnf)})" }

                Log.d("YOLO_LIVE", "--> Confianza: $maxConf | Objetos: $detectedClasses")

                // Opcional: Para saber si estamos viendo bien, imprimir el tamaño del bitmap
                // Log.d("YOLO_DEBUG", "Bitmap size: ${rotatedBitmap.width}x${rotatedBitmap.height}")
                // IMPORTANTE: Como vamos a tocar la UI (dibujar), hay que salir del hilo secundario
                activity?.runOnUiThread {
                    overlay.visibility = View.VISIBLE // Asegurarnos que se vea
                    overlay.setResults(detections)
                }
            }else {
                // Si no detecta nada, limpiamos el dibujo
                activity?.runOnUiThread {
                    overlay.setResults(emptyList())
                }
            }

            // IMPORTANTE: Cerrar siempre
            imageProxy.close()
        }
    }

    // 5. Helpers de permisos (copiá esto tal cual)
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        androidx.core.content.ContextCompat.checkSelfPermission(
            requireContext(), it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun sumarDeteccionesYSalir(campoDestino: EditText) {
        // 1. Apagar cámara y ocultar cosas
        detenerCamara()

        // 2. Calcular Puntos
        var sumaTotal = 0
        val detalle = StringBuilder()

        if (ultimasDetecciones.isNotEmpty()) {
            for (box in ultimasDetecciones) {
                // LÓGICA DE PUNTAJE BURAKO (Ajustala a tus reglas)
                val puntos = when (box.clsName) {
                    "3", "4", "5", "6", "7" -> 5
                    "8", "9", "10", "11", "12", "13" -> 10 // Acepta numeros o letras por las dudas
                    "1" -> 15 // As de burako suele valer 15
                    "2" -> 20 // El 2 (Pinelle) vale 20
                    "C" -> 50
                    else -> 0
                }
                sumaTotal += puntos
                detalle.append("${box.clsName}($puntos) + ")
            }

            // Log para debug
            Log.d("SUMA_FINAL", "Cálculo: $detalle = $sumaTotal")

            // 3. Poner el resultado en el EditText correspondiente
            val puntosActuales = campoDestino.text.toString().toIntOrNull() ?: 0

            // Escribimos en el objeto referenciado
            campoDestino.setText((puntosActuales + sumaTotal).toString())

            Toast.makeText(requireContext(), "Se sumaron $sumaTotal puntos", Toast.LENGTH_SHORT).show()

        } else {
            Toast.makeText(requireContext(), "No se detectó nada para sumar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun detenerCamara() {
        try {
            val cameraProvider = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(requireContext()).get()
            cameraProvider.unbindAll() // Libera la cámara
        } catch (e: Exception) {
            Log.e("Camera", "Error al detener cámara", e)
        }

        // Ocultamos el contenedor completo
        activity?.runOnUiThread {
            cameraContainer.visibility = View.GONE
            // Opcional: Limpiar el overlay para que no quede dibujado lo viejo la proxima vez
            overlay.setResults(emptyList())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }


}