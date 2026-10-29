package ar.escanerflotante

import android.content.Context

/** Ajustes guardados en el celular. */
object Prefs {
    const val ACCION_NADA = 0
    const val ACCION_ENTER = 1
    const val ACCION_SIGUIENTE = 2

    private fun sp(c: Context) = c.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    fun accion(c: Context) = sp(c).getInt("accion", ACCION_NADA)
    fun setAccion(c: Context, v: Int) = sp(c).edit().putInt("accion", v).apply()

    fun vibrar(c: Context) = sp(c).getBoolean("vibrar", true)
    fun setVibrar(c: Context, v: Boolean) = sp(c).edit().putBoolean("vibrar", v).apply()

    fun sonido(c: Context) = sp(c).getBoolean("sonido", true)
    fun setSonido(c: Context, v: Boolean) = sp(c).edit().putBoolean("sonido", v).apply()

    fun mostrarBurbuja(c: Context) = sp(c).getBoolean("burbuja", true)
    fun setMostrarBurbuja(c: Context, v: Boolean) = sp(c).edit().putBoolean("burbuja", v).apply()

    fun burbujaX(c: Context) = sp(c).getInt("bx", -1)
    fun burbujaY(c: Context) = sp(c).getInt("by", -1)
    fun setPosicion(c: Context, x: Int, y: Int) =
        sp(c).edit().putInt("bx", x).putInt("by", y).apply()
}
