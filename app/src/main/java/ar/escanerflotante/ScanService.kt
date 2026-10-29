package ar.escanerflotante

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import android.widget.Toast
import kotlin.math.abs

/**
 * Servicio de accesibilidad: dibuja el botón flotante y escribe el código
 * escaneado en el campo de texto que estaba seleccionado.
 */
class ScanService : AccessibilityService() {

    companion object {
        var instancia: ScanService? = null
            private set
    }

    private val handler = Handler(Looper.getMainLooper())
    private var wm: WindowManager? = null
    private var burbuja: View? = null
    private var params: WindowManager.LayoutParams? = null

    /** Último campo editable que tocó el usuario (en cualquier app). */
    private var ultimoEditable: AccessibilityNodeInfo? = null
    /** Campo donde vamos a escribir el código que se está escaneando. */
    private var campoObjetivo: AccessibilityNodeInfo? = null

    private var tono: ToneGenerator? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instancia = this
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        actualizarBurbuja()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val origen = event?.source ?: return
        if (origen.isEditable) ultimoEditable = origen
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        limpiar()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        limpiar()
        super.onDestroy()
    }

    private fun limpiar() {
        quitarBurbuja()
        tono?.release()
        tono = null
        instancia = null
    }

    // ---------------------------------------------------------------- burbuja

    fun actualizarBurbuja() {
        if (Prefs.mostrarBurbuja(this)) mostrarBurbuja() else quitarBurbuja()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    @SuppressLint("ClickableViewAccessibility")
    private fun mostrarBurbuja() {
        if (burbuja != null) return
        val tam = dp(58)
        val vista = ImageView(this).apply {
            setImageResource(R.drawable.ic_burbuja)
            setBackgroundResource(R.drawable.fondo_burbuja)
            val p = dp(14)
            setPadding(p, p, p, p)
            contentDescription = getString(R.string.escanear)
        }
        val ancho = resources.displayMetrics.widthPixels
        val alto = resources.displayMetrics.heightPixels
        val lp = WindowManager.LayoutParams(
            tam, tam,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = Prefs.burbujaX(this@ScanService).takeIf { it >= 0 } ?: (ancho - tam - dp(8))
            y = Prefs.burbujaY(this@ScanService).takeIf { it >= 0 } ?: (alto / 3)
        }

        val umbral = ViewConfiguration.get(this).scaledTouchSlop
        var inicioX = 0f
        var inicioY = 0f
        var origX = 0
        var origY = 0
        var movio = false
        var tiempoInicio = 0L

        vista.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    inicioX = e.rawX; inicioY = e.rawY
                    origX = lp.x; origY = lp.y
                    movio = false
                    tiempoInicio = SystemClock.uptimeMillis()
                    v.alpha = 0.7f
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - inicioX
                    val dy = e.rawY - inicioY
                    if (!movio && (abs(dx) > umbral || abs(dy) > umbral)) movio = true
                    if (movio) {
                        val w = resources.displayMetrics.widthPixels
                        val h = resources.displayMetrics.heightPixels
                        lp.x = (origX + dx.toInt()).coerceIn(0, w - tam)
                        lp.y = (origY + dy.toInt()).coerceIn(0, h - tam)
                        wm?.updateViewLayout(v, lp)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.alpha = 1f
                    if (movio) {
                        Prefs.setPosicion(this, lp.x, lp.y)
                    } else if (SystemClock.uptimeMillis() - tiempoInicio > 600) {
                        abrirAjustes()
                    } else {
                        tocarBurbuja()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1f
                    true
                }
                else -> false
            }
        }

        try {
            wm?.addView(vista, lp)
            burbuja = vista
            params = lp
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo mostrar el botón: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun quitarBurbuja() {
        burbuja?.let { runCatching { wm?.removeView(it) } }
        burbuja = null
        params = null
    }

    private fun abrirAjustes() {
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // ---------------------------------------------------------------- escaneo

    private fun tocarBurbuja() {
        val foco = runCatching {
            rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        }.getOrNull()
        campoObjetivo = when {
            foco != null && foco.isEditable -> foco
            ultimoEditable?.let { it.refresh() && it.isEditable } == true -> ultimoEditable
            else -> null
        }
        startActivity(
            Intent(this, ScanActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
        )
    }

    /** Lo llama la ventanita del escáner cuando leyó un código. */
    fun recibirCodigo(codigo: String) {
        avisar()
        // Esperamos a que se cierre la ventanita y vuelva la app anterior
        handler.postDelayed({ escribir(codigo) }, 400)
    }

    private fun escribir(codigo: String) {
        val campo = campoObjetivo?.takeIf { it.refresh() && it.isEditable }
            ?: runCatching { rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) }
                .getOrNull()?.takeIf { it.isEditable }
        campoObjetivo = null

        if (campo == null) {
            copiar(codigo)
            Toast.makeText(
                this,
                "Código $codigo copiado. Tocá un campo de texto antes de escanear para que se escriba solo.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val ok = insertarTexto(campo, codigo) || pegar(campo, codigo)
        if (!ok) {
            copiar(codigo)
            Toast.makeText(this, "No pude escribir acá. Código $codigo copiado, pegalo.", Toast.LENGTH_LONG).show()
            return
        }
        handler.postDelayed({ accionPosterior(campo) }, 150)
    }

    private fun insertarTexto(campo: AccessibilityNodeInfo, codigo: String): Boolean {
        val mostrandoPista = Build.VERSION.SDK_INT >= 26 && campo.isShowingHintText
        val actual = if (mostrandoPista) "" else (campo.text?.toString() ?: "")
        var ini = campo.textSelectionStart
        var fin = campo.textSelectionEnd
        if (ini < 0 || ini > actual.length) ini = actual.length
        if (fin < ini || fin > actual.length) fin = ini
        val nuevo = actual.substring(0, ini) + codigo + actual.substring(fin)

        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, nuevo)
        }
        val ok = campo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        if (ok) {
            val pos = ini + codigo.length
            val sel = Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, pos)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, pos)
            }
            campo.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, sel)
        }
        return ok
    }

    private fun pegar(campo: AccessibilityNodeInfo, codigo: String): Boolean {
        copiar(codigo)
        campo.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        return campo.performAction(AccessibilityNodeInfo.ACTION_PASTE)
    }

    private fun accionPosterior(campo: AccessibilityNodeInfo) {
        campo.refresh()
        when (Prefs.accion(this)) {
            Prefs.ACCION_ENTER -> enter(campo)
            Prefs.ACCION_SIGUIENTE -> {
                @Suppress("WrongConstant")
                val siguiente = runCatching { campo.focusSearch(View.FOCUS_FORWARD) }.getOrNull()
                val movido = siguiente != null && siguiente.isEditable &&
                    (siguiente.performAction(AccessibilityNodeInfo.ACTION_FOCUS) ||
                        siguiente.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                // Si no encontramos el siguiente campo, usamos la tecla "siguiente" del teclado
                if (!movido) enter(campo)
            }
        }
    }

    private fun enter(campo: AccessibilityNodeInfo) {
        if (Build.VERSION.SDK_INT >= 30) {
            val id = AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id
            if (!campo.performAction(id)) {
                campo.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                campo.performAction(id)
            }
        } else {
            Toast.makeText(this, "El Enter automático necesita Android 11 o más nuevo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copiar(codigo: String) {
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("código", codigo))
    }

    private fun avisar() {
        if (Prefs.vibrar(this)) {
            val vib: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
                (getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as? Vibrator
            }
            vib?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        if (Prefs.sonido(this)) {
            runCatching {
                if (tono == null) tono = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
                tono?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            }
        }
    }
}
