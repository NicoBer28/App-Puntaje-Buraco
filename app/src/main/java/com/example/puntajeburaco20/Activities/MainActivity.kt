package com.example.puntajeburaco20.Activities

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import com.example.puntajeburaco20.Fragments.PuntajeFragment
import com.example.puntajeburaco20.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        // Configurar el ActionBar con el NavController
        //NavigationUI.setupActionBarWithNavController(this, navController)

        // Obtener las SharedPreferences
        val prefs: SharedPreferences = getSharedPreferences("PuntajeBuracoPreferences", Context.MODE_PRIVATE)

        // Obtener el nombre del fragmento guardado en las SharedPreferences
        val currentFragment = prefs.getString("posicionFragmentoActual", null)

        // Navegar al fragmento correspondiente basado en el valor recuperado
        when (currentFragment) {
            "PartidaFragment" -> {
                navController.navigate(R.id.partidaFragment)
            }

            "PuntajeFragment" -> {
                navController.navigate(R.id.puntajeFragment)
            }
            // Añade más casos según tus fragmentos si es necesario
            else -> {
                navController.navigate(R.id.usuarioFragment)
            }
        }


    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
/*
    override fun onBackPressed() {
        val navController = findNavController(R.id.nav_host_fragment)
        AlertDialog.Builder(this)
            .setTitle("Confirmación")
            .setMessage("¿Estás seguro de que quieres salir? Se perderán todos los puntos")
            .setPositiveButton("Sí") { _, _ ->
                val sharedPreferences = this.getSharedPreferences("PuntajeBuracoPreferences", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()

                // Guardar la posición del fragmento actual
                editor.putString("posicionFragmentoActual", "PartidaFragment")// 1 podría ser una constante que represente la posición de tu fragmento específico
                editor.apply()
                // Acción cuando el usuario confirma
                if (!navController.popBackStack()) {

                    // Si no hay más fragmentos en la pila, comportamiento predeterminado
                    super.onBackPressed()
                }            }
            .setNegativeButton("No", null)
            .show()
    }
*/
}
