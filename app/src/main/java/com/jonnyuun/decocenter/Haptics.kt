package com.jonnyuun.decocenter

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View

/** Vibración de las tarjetas (no hay vista en un TileService, así que se usa el Vibrator). */
object Haptics {
    @Suppress("DEPRECATION")
    fun play(c: Context, h: Haptic) {
        val v = c.applicationContext.getSystemService(Vibrator::class.java) ?: return
        if (!v.hasVibrator()) return
        when {
            Build.VERSION.SDK_INT >= 29 -> {
                val id = when (h) {
                    Haptic.TICK -> VibrationEffect.EFFECT_TICK
                    Haptic.CLICK -> VibrationEffect.EFFECT_CLICK
                    Haptic.SUCCESS -> VibrationEffect.EFFECT_DOUBLE_CLICK
                    Haptic.ERROR -> VibrationEffect.EFFECT_HEAVY_CLICK
                }
                v.vibrate(VibrationEffect.createPredefined(id))
            }
            Build.VERSION.SDK_INT >= 26 -> {
                val effect = when (h) {
                    Haptic.TICK -> VibrationEffect.createOneShot(10, 90)
                    Haptic.CLICK -> VibrationEffect.createOneShot(20, 160)
                    Haptic.SUCCESS -> VibrationEffect.createWaveform(longArrayOf(0, 15, 60, 25), -1)
                    Haptic.ERROR -> VibrationEffect.createOneShot(45, 255)
                }
                v.vibrate(effect)
            }
            else -> v.vibrate(if (h == Haptic.TICK) 10L else 25L)
        }
    }

    /** Aviso más largo para cuando termina un temporizador. */
    @Suppress("DEPRECATION")
    fun alert(c: Context) {
        val v = c.applicationContext.getSystemService(Vibrator::class.java) ?: return
        if (!v.hasVibrator()) return
        val pattern = longArrayOf(0, 200, 120, 200, 120, 400)
        if (Build.VERSION.SDK_INT >= 26) {
            v.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            v.vibrate(pattern, -1)
        }
    }
}

/** Respuesta háptica de la interfaz (HapticFeedbackConstants: respeta los ajustes del sistema). */
object Hx {
    fun tap(v: View) {
        v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    fun confirm(v: View) {
        v.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS,
        )
    }

    fun toggle(v: View, on: Boolean) {
        v.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 34) {
                if (on) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            },
        )
    }
}
