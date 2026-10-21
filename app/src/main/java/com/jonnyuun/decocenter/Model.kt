package com.jonnyuun.decocenter

/** Función que ejecuta una tarjeta al tocarla. El orden de declaración es el del selector de la UI. */
enum class Mode(val key: Int) {
    STATIC(0),
    TOGGLE(1),
    CLIPBOARD(2),
    TALLY(3),
    NOTE(4),
    LAUNCHER(5),
    DICE(6),
    TORCH(7),
    TIMER(8);

    companion object {
        fun of(key: Int): Mode = entries.firstOrNull { it.key == key } ?: STATIC
    }
}

enum class Haptic { TICK, CLICK, SUCCESS, ERROR }

/**
 * Configuración inmutable de una ranura.
 * [lit] es el estado vivo en Interruptor y Linterna, y el aspecto fijo en el resto de modos.
 * En Temporizador el estado activo se deduce de [timerEnd] (instante de fin en ms; 0 = parado).
 */
data class TileConfig(
    val mode: Mode = Mode.STATIC,
    val symbol: String = DecoLogic.DEFAULT_GLYPH,
    val label: String = "",
    val text: String = "",
    val pkg: String = "",
    val lit: Boolean = true,
    val count: Int = 0,
    val result: String = "",
    val dice: Int = DecoLogic.DICE_D6,
    val minutes: Int = DecoLogic.DEFAULT_MINUTES,
    val timerEnd: Long = 0L,
) {
    val glyph: String get() = symbol.trim().ifBlank { DecoLogic.DEFAULT_GLYPH }

    fun isTimerRunning(now: Long): Boolean = mode == Mode.TIMER && timerEnd > now

    fun isActive(now: Long): Boolean = if (mode == Mode.TIMER) timerEnd > now else lit
}
