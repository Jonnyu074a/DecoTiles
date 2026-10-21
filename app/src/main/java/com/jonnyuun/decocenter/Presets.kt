package com.jonnyuun.decocenter

class PresetTile(
    val symbol: String,
    val label: String,
    val mode: Mode = Mode.STATIC,
    val text: String = "",
    val lit: Boolean = true,
    val dice: Int = DecoLogic.DICE_D6,
    val minutes: Int = DecoLogic.DEFAULT_MINUTES,
)

class Preset(val name: String, val glyph: String, val tiles: List<PresetTile>)

/** Temas rápidos. Todo lo que anuncian una función (linterna, temporizador, contador...) la tiene de verdad. */
object Presets {

    private val y2k = Preset(
        "Y2K / Astros", "✦",
        listOf(
            PresetTile("✦", "Estrella", lit = true),
            PresetTile("☾", "Luna", lit = false),
            PresetTile("★", "Astro", lit = true),
            PresetTile("✧", "Destello", lit = false),
            PresetTile("☆", "Cometa", lit = true),
            PresetTile("♡", "Amor", lit = false),
            PresetTile("✿", "Flor", lit = true),
            PresetTile("❀", "Pétalo", lit = false),
            PresetTile("∞", "Infinito", lit = true),
            PresetTile("☁", "Nube", lit = false),
            PresetTile("♪", "Nota", lit = true),
            PresetTile("☺", "Sonrisa", lit = false),
        ),
    )

    private val cyber = Preset(
        "Productividad Cyber", "⚡",
        listOf(
            PresetTile("🔦", "Linterna", Mode.TORCH, lit = false),
            PresetTile("🔋", "Batería", Mode.NOTE, "Cargar al 80%"),
            PresetTile("⏱", "Pomodoro", Mode.TIMER, minutes = 25),
            PresetTile("📋", "Correo", Mode.CLIPBOARD, "correo@ejemplo.com"),
            PresetTile("🎲", "Dado", Mode.DICE, dice = DecoLogic.DICE_D6),
            PresetTile("🪙", "Moneda", Mode.DICE, dice = DecoLogic.DICE_COIN),
            PresetTile("💧", "Agua", Mode.TALLY),
            PresetTile("☕", "Descanso", Mode.TIMER, minutes = 5),
            PresetTile("📝", "Pendiente", Mode.NOTE, "Escribe aquí tu nota"),
            PresetTile("🚀", "Lanzar", Mode.LAUNCHER),
            PresetTile("🧠", "Ideas", Mode.NOTE, "Idea del día"),
            PresetTile("✅", "Hábito", Mode.TALLY),
        ),
    )

    private val kaomoji = Preset(
        "Kaomoji", "◕‿◕",
        listOf(
            PresetTile("◕‿◕", "(˶ᵔ ᵕ ᵔ˶)", lit = true),
            PresetTile("•ᴥ•", "ʕ•ᴥ•ʔ", lit = false),
            PresetTile("◠‿◠", "(◕‿◕✿)", lit = true),
            PresetTile("♡", "(づ｡◕‿‿◕｡)づ", lit = false),
            PresetTile("◔‿◔", "(◔‿◔)", lit = true),
            PresetTile("✧", "(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧", lit = false),
            PresetTile("ω", "(=^･ω･^=)", lit = true),
            PresetTile("☺", "(◍•ᴗ•◍)", lit = false),
            PresetTile("★", "(≧▽≦)", lit = true),
            PresetTile("◉_◉", "(◉_◉)", lit = false),
            PresetTile("ᴗ", "(ᵕ—ᴗ—)", lit = true),
            PresetTile("✿", "(✿◠‿◠)", lit = false),
        ),
    )

    private val luxury = Preset(
        "Minimal Dark Luxury", "◆",
        listOf(
            PresetTile("◆", "Noir", lit = true),
            PresetTile("◇", "Onyx", lit = false),
            PresetTile("▪", "Basalto", lit = false),
            PresetTile("●", "Ébano", lit = false),
            PresetTile("○", "Carbón", lit = false),
            PresetTile("▲", "Grafito", lit = false),
            PresetTile("◈", "Obsidiana", lit = false),
            PresetTile("✕", "Humo", lit = false),
            PresetTile("━", "Ceniza", lit = false),
            PresetTile("❖", "Medianoche", lit = false),
            PresetTile("◐", "Eclipse", lit = false),
            PresetTile("◑", "Penumbra", lit = false),
        ),
    )

    val all: List<Preset> = listOf(y2k, cyber, kaomoji, luxury)

    /**
     * Devuelve la configuración resultante de aplicar [p] sobre [current].
     * Conserva las apps elegidas del Lanzador y reinicia contadores, resultados y temporizadores.
     */
    fun apply(current: List<TileConfig>, p: Preset): List<TileConfig> =
        current.mapIndexed { i, old ->
            val t = p.tiles[i]
            TileConfig(
                mode = t.mode,
                symbol = t.symbol,
                label = t.label,
                text = t.text,
                pkg = old.pkg,
                lit = t.lit,
                count = 0,
                result = "",
                dice = t.dice,
                minutes = t.minutes,
                timerEnd = 0L,
            )
        }
}
