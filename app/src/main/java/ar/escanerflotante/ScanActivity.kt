package ar.escanerflotante

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.widget.Button
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Ventanita chica con la cámara. Lee un código, se lo pasa al servicio
 * y se cierra sola (la cámara se apaga apenas se cierra).
 */
class ScanActivity : AppCompatActivity() {

    private lateinit var vistaCamara: PreviewView
    private lateinit var analizador: ExecutorService
    private var camara: Camera? = null
    private var linternaPrendida = false

    private val lector = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_ITF, Barcode.FORMAT_QR_CODE
            )
            .build()
    )

    private var ultimaLectura: String? = null
    private val terminado = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        setFinishOnTouchOutside(true)

        analizador = Executors.newSingleThreadExecutor()
        vistaCamara = findViewById(R.id.vistaCamara)
        findViewById<Button>(R.id.botonCancelar).setOnClickListener { finish() }
        findViewById<Button>(R.id.botonLinterna).setOnClickListener {
            linternaPrendida = !linternaPrendida
            camara?.cameraControl?.enableTorch(linternaPrendida)
        }

        // Si en 20 segundos no leyó nada, se cierra para no gastar batería
        handler.postDelayed({ finish() }, 20_000)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            iniciarCamara()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara()
        } else {
            Toast.makeText(this, "Sin permiso de cámara no puedo escanear", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun iniciarCamara() {
        val futuro = ProcessCameraProvider.getInstance(this)
        futuro.addListener({
            if (isFinishing) return@addListener
            val proveedor = futuro.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(vistaCamara.surfaceProvider)
            }
            val analisis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .build()
                )
                .build()
            analisis.setAnalyzer(analizador) { analizar(it) }
            try {
                proveedor.unbindAll()
                camara = proveedor.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analisis
                )
            } catch (e: Exception) {
                Toast.makeText(this, "No se pudo abrir la cámara", Toast.LENGTH_LONG).show()
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analizar(proxy: ImageProxy) {
        val imagen = proxy.image
        if (imagen == null || terminado.get()) {
            proxy.close()
            return
        }
        val entrada = InputImage.fromMediaImage(imagen, proxy.imageInfo.rotationDegrees)
        lector.process(entrada)
            .addOnSuccessListener { codigos ->
                val valor = codigos.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                if (valor != null) {
                    // Pedimos la misma lectura dos veces seguidas para evitar errores
                    if (valor == ultimaLectura) entregar(valor) else ultimaLectura = valor
                }
            }
            .addOnCompleteListener { proxy.close() }
    }

    private fun entregar(codigo: String) {
        if (!terminado.compareAndSet(false, true)) return
        val servicio = ScanService.instancia
        if (servicio != null) {
            servicio.recibirCodigo(codigo)
        } else {
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("código", codigo))
            Toast.makeText(this, "Código $codigo copiado", Toast.LENGTH_LONG).show()
        }
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        lector.close()
        analizador.shutdown()
        super.onDestroy()
    }
}
