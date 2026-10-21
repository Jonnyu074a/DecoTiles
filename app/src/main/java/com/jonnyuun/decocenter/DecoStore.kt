package com.jonnyuun.decocenter

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.TileService
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Fuente única de verdad de las 12 tarjetas. [configs] es estado de Compose: la interfaz se actualiza
 * sola cuando cambia algo, también desde un toque en el Centro de Control (mismo proceso).
 * Las escrituras a disco se agrupan 300 ms (una sola escritura y un solo IPC por ráfaga de ediciones).
 */
object DecoStore {
    private const val PREFS = "decocenter"
    private const val FLUSH_DELAY_MS = 300L

    var configs: List<TileConfig> by mutableStateOf(List(DecoLogic.SLOTS) { TileConfig() })
        private set

    private var loaded = false
    private var appContext: Context? = null
    private val dirty = BooleanArray(DecoLogic.SLOTS)
    private val components = arrayOfNulls<ComponentName>(DecoLogic.SLOTS)
    private val handler = Handler(Looper.getMainLooper())
    private val flushTask = Runnable { flushPending() }

    private val defaultGlyphs = listOf(
        "✿", "★", "∞", "♥\uFE0E", "✦", "☾",
        "♪", "☁", "✧", "❀", "☺", "♡",
    )

    // ---------------------------------------------------------------- carga

    private fun prefs(c: Context): SharedPreferences =
        c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun init(c: Context) {
        if (loaded) return
        val app = c.applicationContext
        appContext = app
        val p = prefs(app)
        configs = List(DecoLogic.SLOTS) { readSlot(p, it) }
        loaded = true
        // Migración: textos de Portapapeles guardados en claro por versiones anteriores → se cifran.
        for (i in 0 until DecoLogic.SLOTS) {
            if (configs[i].mode == Mode.CLIPBOARD && p.contains("text$i")) persistSlot(app, i)
        }
    }

    private fun readSlot(p: SharedPreferences, i: Int): TileConfig {
        val secret = p.getString("stext$i", null)
        val text = if (secret != null) {
            Secrets.decrypt(secret).orEmpty()
        } else {
            p.getString("text$i", "").orEmpty()
        }
        return TileConfig(
            mode = Mode.of(p.getInt("mode$i", 0)),
            symbol = p.getString("sym$i", null) ?: defaultGlyphs[i],
            label = p.getString("label$i", "").orEmpty(),
            text = text,
            pkg = p.getString("pkg$i", "").orEmpty(),
            lit = p.getBoolean("lit$i", true),
            count = p.getInt("count$i", 0),
            result = p.getString("result$i", "").orEmpty(),
            dice = p.getInt("dice$i", DecoLogic.DICE_D6),
            minutes = DecoLogic.clampMinutes(p.getInt("min$i", DecoLogic.DEFAULT_MINUTES)),
            timerEnd = p.getLong("end$i", 0L),
        )
    }

    // ---------------------------------------------------------------- lectura y escritura

    @Synchronized
    fun config(c: Context, i: Int): TileConfig {
        init(c)
        return configs[i]
    }

    @Synchronized
    fun update(c: Context, i: Int, immediate: Boolean = false, transform: (TileConfig) -> TileConfig) {
        init(c)
        val old = configs[i]
        val new = transform(old)
        if (new == old) return
        configs = configs.toMutableList().also { it[i] = new }
        dirty[i] = true
        schedule(immediate)
    }

    @Synchronized
    fun replaceAll(c: Context, list: List<TileConfig>) {
        init(c)
        require(list.size == DecoLogic.SLOTS) { "Se esperaban ${DecoLogic.SLOTS} ranuras" }
        for (i in 0 until DecoLogic.SLOTS) if (configs[i] != list[i]) dirty[i] = true
        configs = list.toList()
        schedule(immediate = false)
    }

    /** Marca como terminados los temporizadores cuyo instante de fin ya pasó. Devuelve true si cambió alguno. */
    @Synchronized
    fun expireTimers(c: Context, now: Long = System.currentTimeMillis()): Boolean {
        init(c)
        var changed = false
        for (i in 0 until DecoLogic.SLOTS) {
            val cfg = configs[i]
            if (cfg.mode == Mode.TIMER && cfg.timerEnd in 1..now) {
                update(c, i, immediate = true) {
                    it.copy(timerEnd = 0L, result = c.applicationContext.getString(R.string.timer_done))
                }
                changed = true
            }
        }
        return changed
    }

    // ---------------------------------------------------------------- persistencia

    private fun schedule(immediate: Boolean) {
        handler.removeCallbacks(flushTask)
        if (immediate) flushPending() else handler.postDelayed(flushTask, FLUSH_DELAY_MS)
    }

    /** Escribe ya lo pendiente (p. ej. en onPause o antes de añadir una tarjeta al panel). */
    @Synchronized
    fun flush(c: Context) {
        init(c)
        handler.removeCallbacks(flushTask)
        flushPending()
    }

    @Synchronized
    private fun flushPending() {
        val app = appContext ?: return
        for (i in 0 until DecoLogic.SLOTS) {
            if (dirty[i]) {
                dirty[i] = false
                persistSlot(app, i)
                refreshTile(app, i)
            }
        }
    }

    private fun persistSlot(app: Context, i: Int) {
        val c = configs[i]
        val e = prefs(app).edit()
            .putInt("mode$i", c.mode.key)
            .putString("sym$i", c.symbol)
            .putString("label$i", c.label)
            .putString("pkg$i", c.pkg)
            .putBoolean("lit$i", c.lit)
            .putInt("count$i", c.count)
            .putString("result$i", c.result)
            .putInt("dice$i", c.dice)
            .putInt("min$i", c.minutes)
            .putLong("end$i", c.timerEnd)
        if (c.mode == Mode.CLIPBOARD) {
            // El texto sensible nunca se guarda en claro. Si el Keystore falla, solo vive en memoria.
            val token = if (c.text.isBlank()) null else Secrets.encrypt(c.text)
            if (token != null) e.putString("stext$i", token) else e.remove("stext$i")
            e.remove("text$i")
        } else {
            e.putString("text$i", c.text).remove("stext$i")
        }
        e.apply()
    }

    // ---------------------------------------------------------------- tarjetas del Centro de Control

    @Synchronized
    fun component(c: Context, i: Int): ComponentName =
        components[i] ?: ComponentName(c.applicationContext, TILE_CLASSES[i]).also { components[i] = it }

    /** Pide al sistema que vuelva a leer la tarjeta (un solo IPC por llamada). */
    fun refreshTile(c: Context, i: Int) {
        try {
            TileService.requestListeningState(c.applicationContext, component(c, i))
        } catch (_: Exception) {
        }
    }
}
