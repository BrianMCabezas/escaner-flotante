package ar.escanerflotante

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat

/** Pantalla de configuración y prueba. */
class MainActivity : AppCompatActivity() {

    private lateinit var estadoCamara: TextView
    private lateinit var estadoServicio: TextView
    private lateinit var botonCamara: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        estadoCamara = findViewById(R.id.estadoCamara)
        estadoServicio = findViewById(R.id.estadoServicio)
        botonCamara = findViewById(R.id.botonCamara)

        botonCamara.setOnClickListener {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)
        }
        findViewById<Button>(R.id.botonAccesibilidad).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.botonInfoApp).setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", packageName, null))
            )
        }
        // El bloqueo de "ajustes restringidos" existe desde Android 13
        if (Build.VERSION.SDK_INT < 33) {
            findViewById<View>(R.id.avisoRestringido).visibility = View.GONE
            findViewById<View>(R.id.botonInfoApp).visibility = View.GONE
        }

        val switchBurbuja = findViewById<SwitchCompat>(R.id.switchBurbuja)
        switchBurbuja.isChecked = Prefs.mostrarBurbuja(this)
        switchBurbuja.setOnCheckedChangeListener { _, v ->
            Prefs.setMostrarBurbuja(this, v)
            ScanService.instancia?.actualizarBurbuja()
        }

        val grupo = findViewById<RadioGroup>(R.id.grupoAccion)
        grupo.check(
            when (Prefs.accion(this)) {
                Prefs.ACCION_ENTER -> R.id.accionEnter
                Prefs.ACCION_SIGUIENTE -> R.id.accionSiguiente
                else -> R.id.accionNada
            }
        )
        grupo.setOnCheckedChangeListener { _, id ->
            Prefs.setAccion(
                this, when (id) {
                    R.id.accionEnter -> Prefs.ACCION_ENTER
                    R.id.accionSiguiente -> Prefs.ACCION_SIGUIENTE
                    else -> Prefs.ACCION_NADA
                }
            )
        }

        val switchVibrar = findViewById<SwitchCompat>(R.id.switchVibrar)
        switchVibrar.isChecked = Prefs.vibrar(this)
        switchVibrar.setOnCheckedChangeListener { _, v -> Prefs.setVibrar(this, v) }

        val switchSonido = findViewById<SwitchCompat>(R.id.switchSonido)
        switchSonido.isChecked = Prefs.sonido(this)
        switchSonido.setOnCheckedChangeListener { _, v -> Prefs.setSonido(this, v) }
    }

    override fun onResume() {
        super.onResume()
        actualizarEstados()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        actualizarEstados()
    }

    private fun actualizarEstados() {
        val camaraOk = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        estadoCamara.text = if (camaraOk) "✅ Listo" else "❌ Falta el permiso"
        estadoCamara.setTextColor(ContextCompat.getColor(this, if (camaraOk) R.color.verde else R.color.rojo))
        botonCamara.visibility = if (camaraOk) View.GONE else View.VISIBLE

        val servicioOk = servicioActivo()
        estadoServicio.text = if (servicioOk) "✅ Activado" else "❌ Desactivado"
        estadoServicio.setTextColor(ContextCompat.getColor(this, if (servicioOk) R.color.verde else R.color.rojo))
    }

    private fun servicioActivo(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == packageName }
    }
}
