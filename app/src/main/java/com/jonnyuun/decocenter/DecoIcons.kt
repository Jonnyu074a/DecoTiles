package com.jonnyuun.decocenter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.util.LruCache

/**
 * Iconos de las tarjetas. Símbolos: máscara blanca sobre transparente (el sistema la tiñe con el tema
 * dinámico). Emojis: se dibujan con sus propios colores; si la ROM tiñe los iconos se verán en silueta.
 */
object DecoIcons {
    private const val SIZE = 192
    private val cache = LruCache<String, Icon>(24)

    @Synchronized
    fun icon(glyph: String): Icon {
        cache.get(glyph)?.let { return it }
        val emoji = DecoLogic.isColorEmoji(glyph)
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.LEFT
            textSize = SIZE * (if (emoji) 0.66f else 0.62f)
        }
        val bounds = Rect()
        paint.getTextBounds(glyph, 0, glyph.length, bounds)
        val limit = SIZE * (if (emoji) 0.9f else 0.82f)
        if (bounds.width() > 0 && bounds.height() > 0) {
            val scale = minOf(1f, limit / bounds.width(), limit / bounds.height())
            if (scale < 1f) {
                paint.textSize *= scale
                paint.getTextBounds(glyph, 0, glyph.length, bounds)
            }
        }
        canvas.drawText(
            glyph,
            SIZE / 2f - bounds.exactCenterX(),
            SIZE / 2f - bounds.exactCenterY(),
            paint,
        )
        return Icon.createWithBitmap(bmp).also { cache.put(glyph, it) }
    }
}
