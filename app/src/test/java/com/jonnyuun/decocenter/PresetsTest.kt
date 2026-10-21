package com.jonnyuun.decocenter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetsTest {

    @Test
    fun `todos los temas tienen exactamente una definicion por ranura`() {
        assertTrue(Presets.all.isNotEmpty())
        Presets.all.forEach { assertEquals(it.name, DecoLogic.SLOTS, it.tiles.size) }
    }

    @Test
    fun `ningun tema tiene simbolos ni nombres vacios`() {
        Presets.all.forEach { preset ->
            preset.tiles.forEach { tile ->
                assertTrue("${preset.name}: símbolo vacío", tile.symbol.isNotBlank())
                assertTrue("${preset.name}: símbolo demasiado largo", tile.symbol.length <= DecoLogic.MAX_SYMBOL)
                assertTrue("${preset.name}: nombre demasiado largo", tile.label.length <= DecoLogic.MAX_LABEL)
                assertTrue(tile.minutes in DecoLogic.MIN_MINUTES..DecoLogic.MAX_MINUTES)
            }
        }
    }

    @Test
    fun `apply conserva las apps del lanzador y reinicia el estado vivo`() {
        val current = List(DecoLogic.SLOTS) {
            TileConfig(
                mode = Mode.LAUNCHER,
                pkg = "com.example.$it",
                count = 5,
                result = "6",
                timerEnd = 999L,
            )
        }
        val applied = Presets.apply(current, Presets.all.first())
        assertEquals(DecoLogic.SLOTS, applied.size)
        applied.forEachIndexed { i, cfg ->
            assertEquals("com.example.$i", cfg.pkg)
            assertEquals(0, cfg.count)
            assertEquals("", cfg.result)
            assertEquals(0L, cfg.timerEnd)
        }
    }

    @Test
    fun `las funciones anunciadas por el tema Cyber son reales`() {
        val cyber = Presets.all.first { it.name.contains("Cyber") }
        val modes = cyber.tiles.map { it.mode }
        assertTrue(Mode.TORCH in modes)
        assertTrue(Mode.TIMER in modes)
        assertTrue(Mode.TALLY in modes)
        // Un Pomodoro es un temporizador, no un interruptor decorativo.
        val pomodoro = cyber.tiles.first { it.label == "Pomodoro" }
        assertEquals(Mode.TIMER, pomodoro.mode)
        assertEquals(25, pomodoro.minutes)
        assertFalse(cyber.tiles.any { it.mode == Mode.TOGGLE })
    }
}
