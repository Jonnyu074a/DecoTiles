package com.jonnyuun.decocenter.ui

import androidx.annotation.StringRes
import com.jonnyuun.decocenter.Mode
import com.jonnyuun.decocenter.R

@get:StringRes
val Mode.titleRes: Int
    get() = when (this) {
        Mode.STATIC -> R.string.mode_static
        Mode.TOGGLE -> R.string.mode_toggle
        Mode.CLIPBOARD -> R.string.mode_clipboard
        Mode.TALLY -> R.string.mode_tally
        Mode.NOTE -> R.string.mode_note
        Mode.LAUNCHER -> R.string.mode_launcher
        Mode.DICE -> R.string.mode_dice
        Mode.TORCH -> R.string.mode_torch
        Mode.TIMER -> R.string.mode_timer
    }

@get:StringRes
val Mode.hintRes: Int
    get() = when (this) {
        Mode.STATIC -> R.string.mode_static_hint
        Mode.TOGGLE -> R.string.mode_toggle_hint
        Mode.CLIPBOARD -> R.string.mode_clipboard_hint
        Mode.TALLY -> R.string.mode_tally_hint
        Mode.NOTE -> R.string.mode_note_hint
        Mode.LAUNCHER -> R.string.mode_launcher_hint
        Mode.DICE -> R.string.mode_dice_hint
        Mode.TORCH -> R.string.mode_torch_hint
        Mode.TIMER -> R.string.mode_timer_hint
    }
