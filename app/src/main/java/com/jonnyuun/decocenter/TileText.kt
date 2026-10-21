package com.jonnyuun.decocenter

import android.content.Context

/** Textos que muestra una tarjeta (Centro de Control y simulador), localizados con recursos. */

fun TileConfig.title(c: Context, slot: Int): String =
    label.ifBlank { c.getString(R.string.tile_default_title, slot + 1) }

fun TileConfig.subtitle(c: Context, now: Long): String? = when (mode) {
    Mode.STATIC -> null
    Mode.TOGGLE, Mode.TORCH -> c.getString(if (lit) R.string.state_on else R.string.state_off)
    Mode.CLIPBOARD -> c.getString(if (text.isBlank()) R.string.sub_no_text else R.string.sub_tap_copy)
    Mode.TALLY -> count.toString()
    Mode.NOTE -> text.ifBlank { null }
    Mode.LAUNCHER -> Apps.label(c, pkg) ?: c.getString(R.string.sub_no_app)
    Mode.DICE -> result.ifBlank { c.getString(R.string.sub_tap_roll) }
    Mode.TIMER ->
        if (timerEnd > now) {
            DecoLogic.formatRemaining(timerEnd - now)
        } else {
            result.ifBlank { c.getString(R.string.sub_minutes, minutes) }
        }
}

fun formatRoll(c: Context, dice: Int, value: Int): String = when (dice) {
    DecoLogic.DICE_COIN -> c.getString(if (value == 0) R.string.coin_heads else R.string.coin_tails)
    DecoLogic.DICE_D20 -> c.getString(R.string.roll_d20, value)
    else -> c.getString(R.string.roll_d6, value)
}
