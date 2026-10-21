package com.jonnyuun.decocenter

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Exportación e importación de las 12 tarjetas en JSON.
 * No se guarda el estado vivo (resultado, temporizador en marcha). El texto de las tarjetas
 * Portapapeles es sensible, así que solo se exporta si se pide expresamente.
 */
object Backup {
    const val FORMAT_VERSION = 1
    private const val APP_ID = "DecoCenter"

    fun toJson(configs: List<TileConfig>, includeSecrets: Boolean): String {
        val tiles = JSONArray()
        for (c in configs) {
            val o = JSONObject()
            o.put("mode", c.mode.name)
            o.put("symbol", c.symbol)
            o.put("label", c.label)
            if (c.mode != Mode.CLIPBOARD || includeSecrets) o.put("text", c.text)
            o.put("pkg", c.pkg)
            o.put("lit", c.lit)
            o.put("count", c.count)
            o.put("dice", c.dice)
            o.put("minutes", c.minutes)
            tiles.put(o)
        }
        val root = JSONObject()
        root.put("app", APP_ID)
        root.put("version", FORMAT_VERSION)
        root.put("tiles", tiles)
        return root.toString(2)
    }

    /**
     * Devuelve las ranuras importadas (saneadas) o null si el archivo no es una copia válida.
     * Las ranuras que el archivo no trae, y el texto de Portapapeles excluido al exportar,
     * se conservan de [fallback].
     */
    fun fromJson(json: String, fallback: List<TileConfig>): List<TileConfig>? {
        require(fallback.size == DecoLogic.SLOTS) { "fallback debe tener ${DecoLogic.SLOTS} ranuras" }
        val root = try {
            JSONObject(json)
        } catch (_: JSONException) {
            return null
        }
        if (root.optString("app") != APP_ID) return null
        if (root.optInt("version", 0) !in 1..FORMAT_VERSION) return null
        val tiles = root.optJSONArray("tiles") ?: return null

        val result = fallback.toMutableList()
        for (i in 0 until minOf(tiles.length(), DecoLogic.SLOTS)) {
            val o = tiles.optJSONObject(i) ?: continue
            val mode = Mode.entries.firstOrNull { it.name == o.optString("mode") } ?: Mode.STATIC
            val text = if (mode == Mode.CLIPBOARD && !o.has("text")) {
                fallback[i].text
            } else {
                o.optString("text", "")
            }
            result[i] = TileConfig(
                mode = mode,
                symbol = o.optString("symbol", DecoLogic.DEFAULT_GLYPH).take(DecoLogic.MAX_SYMBOL),
                label = o.optString("label", "").take(DecoLogic.MAX_LABEL),
                text = text.take(DecoLogic.MAX_TEXT),
                pkg = o.optString("pkg", "").take(DecoLogic.MAX_PACKAGE),
                lit = o.optBoolean("lit", true),
                count = o.optInt("count", 0).coerceIn(0, DecoLogic.MAX_COUNT),
                dice = o.optInt("dice", DecoLogic.DICE_D6).coerceIn(DecoLogic.DICE_D6, DecoLogic.DICE_D20),
                minutes = DecoLogic.clampMinutes(o.optInt("minutes", DecoLogic.DEFAULT_MINUTES)),
            )
        }
        return result
    }
}
