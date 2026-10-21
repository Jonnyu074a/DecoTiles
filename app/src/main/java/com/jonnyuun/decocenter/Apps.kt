package com.jonnyuun.decocenter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class AppEntry(val pkg: String, val label: String)

/** Apps con launcher (selector del modo Lanzador), etiquetas e iconos con caché. */
object Apps {
    private const val ICON_PX = 96
    private val icons = LruCache<String, ImageBitmap>(96)
    private val labels = java.util.concurrent.ConcurrentHashMap<String, String>()

    @Suppress("DEPRECATION")
    fun label(c: Context, pkg: String): String? {
        if (pkg.isBlank()) return null
        labels[pkg]?.let { return it } // se consulta en cada recomposición: evita ir al PackageManager cada vez
        return try {
            val pm = c.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString().also { labels[pkg] = it }
        } catch (_: Exception) {
            null
        }
    }

    /** Pesado: llamar fuera del hilo principal. */
    fun launchable(c: Context): List<AppEntry> {
        val pm = c.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(main, 0)
            .map { AppEntry(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.pkg != c.packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }

    /** Pesado: llamar fuera del hilo principal. */
    fun icon(c: Context, pkg: String): ImageBitmap? {
        icons.get(pkg)?.let { return it }
        return try {
            val drawable = c.packageManager.getApplicationIcon(pkg)
            drawable.toImageBitmap(ICON_PX).also { icons.put(pkg, it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun Drawable.toImageBitmap(size: Int): ImageBitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, size, size)
        draw(canvas)
        return bitmap.asImageBitmap()
    }
}
