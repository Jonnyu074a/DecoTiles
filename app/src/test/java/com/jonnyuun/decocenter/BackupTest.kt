package com.jonnyuun.decocenter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class BackupTest {

    private fun blank() = List(DecoLogic.SLOTS) { TileConfig() }

    private fun sample(): List<TileConfig> = blank().toMutableList().also {
        it[0] = TileConfig(mode = Mode.TORCH, symbol = "🔦", label = "Linterna", lit = true)
        it[1] = TileConfig(mode = Mode.CLIPBOARD, symbol = "📋", label = "IBAN", text = "ES00 1234")
        it[2] = TileConfig(mode = Mode.TIMER, symbol = "⏱", label = "Pomodoro", minutes = 50, timerEnd = 123L)
        it[3] = TileConfig(mode = Mode.TALLY, count = 7, result = "no se exporta")
        it[4] = TileConfig(mode = Mode.NOTE, text = "Hola \"mundo\"\ncon salto de línea")
        it[5] = TileConfig(mode = Mode.LAUNCHER, pkg = "com.example.app")
        it[6] = TileConfig(mode = Mode.DICE, dice = DecoLogic.DICE_D20)
    }

    @Test
    fun `ida y vuelta conserva la configuracion con los secretos`() {
        val original = sample()
        val restored = Backup.fromJson(Backup.toJson(original, includeSecrets = true), blank())
        assertNotNull(restored)
        restored!!
        assertEquals(DecoLogic.SLOTS, restored.size)
        assertEquals(original[0], restored[0])
        assertEquals("ES00 1234", restored[1].text)
        assertEquals(50, restored[2].minutes)
        assertEquals(7, restored[3].count)
        assertEquals(original[4].text, restored[4].text)
        assertEquals("com.example.app", restored[5].pkg)
        assertEquals(DecoLogic.DICE_D20, restored[6].dice)
    }

    @Test
    fun `no se exporta el estado vivo`() {
        val restored = Backup.fromJson(Backup.toJson(sample(), includeSecrets = true), blank())!!
        assertEquals(0L, restored[2].timerEnd)
        assertEquals("", restored[3].result)
    }

    @Test
    fun `sin secretos el texto del portapapeles no aparece en el archivo`() {
        val json = Backup.toJson(sample(), includeSecrets = false)
        assertFalse(json.contains("ES00 1234"))
        // El resto de textos sí se exportan.
        assertTrue(json.contains("Hola"))
    }

    @Test
    fun `al importar sin secretos se conserva el texto actual de la tarjeta Portapapeles`() {
        val current = sample()
        val json = Backup.toJson(sample(), includeSecrets = false)
        val restored = Backup.fromJson(json, current)!!
        assertEquals("ES00 1234", restored[1].text)
    }

    @Test
    fun `un archivo que no es JSON se rechaza`() {
        assertNull(Backup.fromJson("esto no es json", blank()))
        assertNull(Backup.fromJson("", blank()))
    }

    @Test
    fun `un JSON de otra app o sin tarjetas se rechaza`() {
        assertNull(Backup.fromJson("""{"app":"Otra","version":1,"tiles":[]}""", blank()))
        assertNull(Backup.fromJson("""{"app":"DecoCenter","version":1}""", blank()))
    }

    @Test
    fun `una version futura se rechaza`() {
        assertNull(Backup.fromJson("""{"app":"DecoCenter","version":99,"tiles":[]}""", blank()))
    }

    @Test
    fun `los valores fuera de rango se sanean`() {
        val json = """
            {"app":"DecoCenter","version":1,"tiles":[
              {"mode":"TIMER","minutes":99999,"count":-4,"dice":77,"symbol":"abcdefghijklmnopqrstuvwxyz","label":"${"x".repeat(100)}"},
              {"mode":"NO_EXISTE"}
            ]}
        """.trimIndent()
        val restored = Backup.fromJson(json, blank())!!
        assertEquals(DecoLogic.MAX_MINUTES, restored[0].minutes)
        assertEquals(0, restored[0].count)
        assertEquals(DecoLogic.DICE_D20, restored[0].dice)
        assertEquals(DecoLogic.MAX_SYMBOL, restored[0].symbol.length)
        assertEquals(DecoLogic.MAX_LABEL, restored[0].label.length)
        assertEquals(Mode.STATIC, restored[1].mode)
    }

    @Test
    fun `las ranuras que faltan en el archivo se conservan`() {
        val current = sample()
        val json = """{"app":"DecoCenter","version":1,"tiles":[{"mode":"NOTE","text":"nuevo"}]}"""
        val restored = Backup.fromJson(json, current)!!
        assertEquals("nuevo", restored[0].text)
        assertEquals(current[5], restored[5])
        assertEquals(current[11], restored[11])
    }

    @Test
    fun `ignora ranuras de mas`() {
        val tiles = (1..20).joinToString(",") { """{"mode":"STATIC","label":"t$it"}""" }
        val restored = Backup.fromJson("""{"app":"DecoCenter","version":1,"tiles":[$tiles]}""", blank())!!
        assertEquals(DecoLogic.SLOTS, restored.size)
        assertEquals("t12", restored[11].label)
    }
}
