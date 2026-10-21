package com.jonnyuun.decocenter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DecoLogicTest {

    @Test
    fun `el contador suma uno y vuelve a cero al llegar al maximo`() {
        assertEquals(1, DecoLogic.nextCount(0))
        assertEquals(42, DecoLogic.nextCount(41))
        assertEquals(DecoLogic.MAX_COUNT, DecoLogic.nextCount(DecoLogic.MAX_COUNT - 1))
        assertEquals(0, DecoLogic.nextCount(DecoLogic.MAX_COUNT))
    }

    @Test
    fun `las tiradas siempre caen dentro de su rango`() {
        val random = Random(1234)
        repeat(2_000) {
            assertTrue(DecoLogic.rollDice(DecoLogic.DICE_D6, random) in 1..6)
            assertTrue(DecoLogic.rollDice(DecoLogic.DICE_D20, random) in 1..20)
            assertTrue(DecoLogic.rollDice(DecoLogic.DICE_COIN, random) in 0..1)
        }
    }

    @Test
    fun `un tipo de dado desconocido se trata como d6`() {
        val random = Random(7)
        repeat(200) { assertTrue(DecoLogic.rollDice(99, random) in 1..6) }
    }

    @Test
    fun `la moneda produce cara y cruz`() {
        val random = Random(99)
        val seen = (1..200).map { DecoLogic.rollDice(DecoLogic.DICE_COIN, random) }.toSet()
        assertEquals(setOf(0, 1), seen)
    }

    @Test
    fun `isColorEmoji distingue emojis de simbolos`() {
        assertTrue(DecoLogic.isColorEmoji("🔦"))
        assertTrue(DecoLogic.isColorEmoji("⭐\uFE0F"))
        assertTrue(DecoLogic.isColorEmoji("👨\u200D💻"))
        assertFalse(DecoLogic.isColorEmoji("✦"))
        assertFalse(DecoLogic.isColorEmoji("♥\uFE0E"))
        assertFalse(DecoLogic.isColorEmoji("◕‿◕"))
        assertFalse(DecoLogic.isColorEmoji(""))
    }

    @Test
    fun `glyphScale reduce los simbolos largos pero no los emojis`() {
        assertEquals(1f, DecoLogic.glyphScale("✦"), 0f)
        assertEquals(1f, DecoLogic.glyphScale("🔦"), 0f)
        assertEquals(0.72f, DecoLogic.glyphScale("ab"), 0f)
        assertEquals(0.55f, DecoLogic.glyphScale("◕‿◕"), 0f)
        assertEquals(0.42f, DecoLogic.glyphScale("abcd"), 0f)
    }

    @Test
    fun `formatRemaining redondea hacia arriba y no depende del locale`() {
        assertEquals("25:00", DecoLogic.formatRemaining(25 * 60_000L))
        assertEquals("00:01", DecoLogic.formatRemaining(1))
        assertEquals("00:01", DecoLogic.formatRemaining(1000))
        assertEquals("00:02", DecoLogic.formatRemaining(1001))
        assertEquals("120:00", DecoLogic.formatRemaining(120 * 60_000L))
        assertEquals("00:00", DecoLogic.formatRemaining(0))
        assertEquals("00:00", DecoLogic.formatRemaining(-500))
    }

    @Test
    fun `remainingMillis ignora temporizadores parados o vencidos`() {
        assertEquals(0L, DecoLogic.remainingMillis(0L, 1_000L))
        assertEquals(0L, DecoLogic.remainingMillis(500L, 1_000L))
        assertEquals(4_000L, DecoLogic.remainingMillis(5_000L, 1_000L))
    }

    @Test
    fun `clampMinutes respeta los limites`() {
        assertEquals(DecoLogic.MIN_MINUTES, DecoLogic.clampMinutes(-5))
        assertEquals(DecoLogic.MIN_MINUTES, DecoLogic.clampMinutes(0))
        assertEquals(25, DecoLogic.clampMinutes(25))
        assertEquals(DecoLogic.MAX_MINUTES, DecoLogic.clampMinutes(10_000))
    }

    @Test
    fun `TileConfig deduce el estado activo segun el modo`() {
        val toggle = TileConfig(mode = Mode.TOGGLE, lit = true)
        assertTrue(toggle.isActive(now = 10L))

        val runningTimer = TileConfig(mode = Mode.TIMER, lit = false, timerEnd = 5_000L)
        assertTrue(runningTimer.isActive(now = 1_000L))
        assertTrue(runningTimer.isTimerRunning(now = 1_000L))
        assertFalse(runningTimer.isActive(now = 5_000L))
        assertFalse(runningTimer.isTimerRunning(now = 6_000L))

        // Fuera del modo Temporizador, timerEnd no cuenta.
        assertFalse(TileConfig(mode = Mode.NOTE, lit = false, timerEnd = 5_000L).isActive(now = 1_000L))
    }

    @Test
    fun `glyph usa el simbolo por defecto cuando esta vacio`() {
        assertEquals(DecoLogic.DEFAULT_GLYPH, TileConfig(symbol = "   ").glyph)
        assertEquals("★", TileConfig(symbol = " ★ ").glyph)
    }

    @Test
    fun `Mode of conoce todas las claves y cae en STATIC con las desconocidas`() {
        Mode.entries.forEach { assertEquals(it, Mode.of(it.key)) }
        assertEquals(Mode.STATIC, Mode.of(-1))
        assertEquals(Mode.STATIC, Mode.of(999))
    }
}
