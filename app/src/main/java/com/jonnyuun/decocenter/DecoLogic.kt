package com.jonnyuun.decocenter

import kotlin.random.Random

/**
 * Lógica pura de DecoCenter: sin Context ni clases de Android, para poder probarla con tests JVM.
 */
object DecoLogic {
    const val SLOTS = 12
    const val DEFAULT_GLYPH = "✦"

    const val MAX_COUNT = 99_999
    const val MAX_LABEL = 32
    const val MAX_SYMBOL = 12
    const val MAX_TEXT = 500
    const val MAX_PACKAGE = 256
    const val MIN_MINUTES = 1
    const val MAX_MINUTES = 120
    const val DEFAULT_MINUTES = 25

    const val DICE_D6 = 0
    const val DICE_COIN = 1
    const val DICE_D20 = 2

    /** +1 con cada toque; al llegar al máximo vuelve a 0. */
    fun nextCount(count: Int): Int = if (count >= MAX_COUNT) 0 else count + 1

    /**
     * Tirada en bruto: d6 → 1..6, d20 → 1..20, moneda → 0 (cara) o 1 (cruz).
     * El texto localizado lo construye la capa de Android.
     */
    fun rollDice(dice: Int, random: Random = Random.Default): Int = when (dice) {
        DICE_COIN -> random.nextInt(0, 2)
        DICE_D20 -> random.nextInt(1, 21)
        else -> random.nextInt(1, 7)
    }

    /**
     * true para emojis gráficos a color; false para símbolos monocromáticos (✦ ★ ∞ ♥︎).
     * El selector U+FE0E fuerza texto, U+FE0F fuerza emoji, U+200D une emojis compuestos.
     */
    fun isColorEmoji(s: String): Boolean {
        if (s.isEmpty()) return false
        if (s.contains('\uFE0E')) return false
        if (s.contains('\uFE0F') || s.contains('\u200D')) return true
        return s.codePointAt(0) >= 0x1F000
    }

    /** Factor de tamaño para que los símbolos largos (kaomoji) quepan en un círculo. */
    fun glyphScale(glyph: String): Float {
        if (isColorEmoji(glyph)) return 1f
        return when (glyph.codePointCount(0, glyph.length)) {
            0, 1 -> 1f
            2 -> 0.72f
            3 -> 0.55f
            else -> 0.42f
        }
    }

    fun clampMinutes(minutes: Int): Int = minutes.coerceIn(MIN_MINUTES, MAX_MINUTES)

    /** Milisegundos que faltan para [end]; 0 si no hay temporizador o ya terminó. */
    fun remainingMillis(end: Long, now: Long): Long = if (end <= 0L) 0L else (end - now).coerceAtLeast(0L)

    /** mm:ss redondeando hacia arriba (sin depender del locale). */
    fun formatRemaining(millis: Long): String {
        val total = ((millis + 999L) / 1000L).coerceAtLeast(0L)
        val minutes = total / 60L
        val seconds = total % 60L
        return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}
