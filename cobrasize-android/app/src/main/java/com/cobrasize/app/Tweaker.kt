package com.cobrasize.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.os.Build

/**
 * Ajustes reais de sistema, todos reversíveis. Os valores originais são salvos antes
 * de qualquer mudança e restaurados ao desativar, fechar o app ou abrir de novo (caso o app morra).
 */
object Tweaker {
    private const val PREFS = "tweaker"
    private val ANIM_KEYS = listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale")
    private val REFRESH_KEYS = listOf("peak_refresh_rate", "min_refresh_rate")
    private const val LONG_PRESS = "long_press_timeout"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun hasSecure(c: Context) =
        c.packageManager.checkPermission(Manifest.permission.WRITE_SECURE_SETTINGS, c.packageName) == PackageManager.PERMISSION_GRANTED

    private fun backup(c: Context) {
        val p = prefs(c)
        if (p.getBoolean("saved", false)) return
        val cr = c.contentResolver
        val e = p.edit()
        ANIM_KEYS.forEach { e.putString("g_$it", Settings.Global.getString(cr, it)) }
        e.putString("s_$LONG_PRESS", Settings.Secure.getString(cr, LONG_PRESS))
        REFRESH_KEYS.forEach { e.putString("y_$it", Settings.System.getString(cr, it)) }
        e.putBoolean("saved", true).apply()
    }

    fun apply(c: Context, sensi: Int, gpu: String, fps: Int): String {
        val cr = c.contentResolver
        val done = mutableListOf<String>()
        val missing = mutableListOf<String>()
        backup(c)

        if (hasSecure(c)) {
            try {
                val scale = when (gpu) { "turbo" -> "0.25"; "stable" -> "0.5"; else -> null }
                if (scale != null) {
                    ANIM_KEYS.forEach { Settings.Global.putString(cr, it, scale) }
                    done += "animações ${scale}x"
                }
                val lp = 500 - (sensi.coerceIn(0, 200) * 250 / 200)
                Settings.Secure.putString(cr, LONG_PRESS, lp.toString())
                done += "toque longo ${lp}ms"
            } catch (e: Exception) {
                missing += "WRITE_SECURE_SETTINGS"
            }
        } else {
            missing += "WRITE_SECURE_SETTINGS (animações e toque longo)"
        }

        if (fps >= 90) {
            if (Settings.System.canWrite(c)) {
                try {
                    val wm = c.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
                    val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) c.display else wm.defaultDisplay
                    val supported = display?.supportedModes
                        ?.map { it.refreshRate }
                        ?.filter { it > 0f && it <= fps.toFloat() + 0.5f }
                        ?.maxOrNull()
                    val current = display?.refreshRate ?: 60f
                    val targetRate = supported ?: minOf(current, fps.toFloat())
                    REFRESH_KEYS.forEach { Settings.System.putFloat(cr, it, targetRate) }
                    done += "taxa de atualização ${targetRate.toInt()}Hz (modo compatível detectado)"
                } catch (e: Exception) {
                    missing += "Modificar configurações do sistema"
                }
            } else {
                missing += "Modificar configurações do sistema (taxa de atualização)"
            }
        }

        return when {
            done.isEmpty() -> "Nada aplicado. Falta permissão: ${missing.joinToString(", ")}. Veja a dica no Painel."
            missing.isEmpty() -> "✅ Aplicado: ${done.joinToString(", ")}. Volta ao normal ao desativar ou fechar."
            else -> "✅ Aplicado: ${done.joinToString(", ")}. Faltou: ${missing.joinToString(", ")}."
        }
    }

    fun restore(c: Context) {
        val p = prefs(c)
        if (!p.getBoolean("saved", false)) return
        val cr = c.contentResolver
        try {
            ANIM_KEYS.forEach { Settings.Global.putString(cr, it, p.getString("g_$it", null)) }
            Settings.Secure.putString(cr, LONG_PRESS, p.getString("s_$LONG_PRESS", null))
        } catch (e: Exception) { }
        try {
            REFRESH_KEYS.forEach { Settings.System.putString(cr, it, p.getString("y_$it", null)) }
        } catch (e: Exception) { }
        p.edit().clear().apply()
    }
}
