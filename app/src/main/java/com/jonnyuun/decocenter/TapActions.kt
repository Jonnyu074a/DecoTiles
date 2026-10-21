package com.jonnyuun.decocenter

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast

/** Lo que hace cada tarjeta al tocarla. Lo comparten el Centro de Control y el simulador de la app. */
object TapActions {

    /** Ejecuta el toque de la ranura [i]. Devuelve la vibración a reproducir, o null si no hay. */
    fun perform(c: Context, i: Int): Haptic? {
        val app = c.applicationContext
        val cfg = DecoStore.config(app, i)
        return when (cfg.mode) {
            Mode.STATIC, Mode.NOTE, Mode.LAUNCHER -> null

            Mode.TOGGLE -> {
                DecoStore.update(app, i, immediate = true) { it.copy(lit = !it.lit) }
                Haptic.CLICK
            }

            Mode.CLIPBOARD -> copy(app, cfg)

            Mode.TALLY -> {
                DecoStore.update(app, i, immediate = true) { it.copy(count = DecoLogic.nextCount(it.count)) }
                Haptic.TICK
            }

            Mode.DICE -> {
                val text = formatRoll(app, cfg.dice, DecoLogic.rollDice(cfg.dice))
                DecoStore.update(app, i, immediate = true) { it.copy(result = text) }
                Haptic.TICK
            }

            Mode.TORCH -> toggleTorch(app, i, cfg)

            Mode.TIMER -> toggleTimer(app, i, cfg)
        }
    }

    private fun copy(app: Context, cfg: TileConfig): Haptic {
        if (cfg.text.isBlank()) {
            toast(app, R.string.toast_no_text)
            return Haptic.ERROR
        }
        val clipboard = app.getSystemService(ClipboardManager::class.java)
        val clip = ClipData.newPlainText("DecoCenter", cfg.text)
        if (Build.VERSION.SDK_INT >= 33) {
            // Evita que el sistema muestre el contenido en la vista previa del portapapeles.
            clip.description.extras = PersistableBundle().apply {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }
        clipboard.setPrimaryClip(clip)
        // Android 13+ ya muestra su propio aviso al copiar.
        if (Build.VERSION.SDK_INT < 33) toast(app, R.string.toast_copied)
        return Haptic.SUCCESS
    }

    private fun toggleTorch(app: Context, i: Int, cfg: TileConfig): Haptic {
        val cm = app.getSystemService(CameraManager::class.java)
        val id = cm?.let { Torch.cameraId(it) }
        if (cm == null || id == null) {
            toast(app, R.string.toast_no_flash)
            return Haptic.ERROR
        }
        val target = !cfg.lit
        return try {
            cm.setTorchMode(id, target)
            DecoStore.update(app, i, immediate = true) { it.copy(lit = target) }
            Haptic.CLICK
        } catch (_: Exception) {
            toast(app, R.string.toast_torch_failed)
            Haptic.ERROR
        }
    }

    private fun toggleTimer(app: Context, i: Int, cfg: TileConfig): Haptic {
        val now = System.currentTimeMillis()
        return if (cfg.timerEnd > now) {
            TimerAlarm.cancel(app, i)
            DecoStore.update(app, i, immediate = true) { it.copy(timerEnd = 0L, result = "") }
            Haptic.CLICK
        } else {
            val end = now + DecoLogic.clampMinutes(cfg.minutes) * 60_000L
            DecoStore.update(app, i, immediate = true) { it.copy(timerEnd = end, result = "") }
            TimerAlarm.schedule(app, i, end)
            Haptic.CLICK
        }
    }

    private fun toast(app: Context, res: Int) {
        Toast.makeText(app, res, Toast.LENGTH_SHORT).show()
    }
}

/** Localiza la cámara con flash trasero para la Linterna (setTorchMode no necesita permisos). */
object Torch {
    private var cached: String? = null
    private var resolved = false

    @Synchronized
    fun cameraId(cm: CameraManager): String? {
        if (resolved) return cached
        return try {
            val id = cm.cameraIdList.firstOrNull { camera ->
                val ch = cm.getCameraCharacteristics(camera)
                ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                    ch.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
            cached = id
            resolved = true
            id
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Alarma del Temporizador. Usa setAndAllowWhileIdle (sin permisos de alarma exacta): puede retrasarse
 * unos minutos en Doze. Si el móvil se reinicia la alarma se pierde y el temporizador se da por terminado
 * al volver a mostrar la tarjeta.
 */
object TimerAlarm {
    const val ACTION = "com.jonnyuun.decocenter.TIMER_DONE"
    const val EXTRA_SLOT = "slot"
    const val EXTRA_END = "end"

    private fun pending(c: Context, slot: Int, end: Long): PendingIntent {
        val intent = Intent(c, TimerReceiver::class.java)
            .setAction(ACTION)
            .putExtra(EXTRA_SLOT, slot)
            .putExtra(EXTRA_END, end)
        return PendingIntent.getBroadcast(
            c,
            slot,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    fun schedule(c: Context, slot: Int, end: Long) {
        val am = c.getSystemService(AlarmManager::class.java) ?: return
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, pending(c, slot, end))
    }

    fun cancel(c: Context, slot: Int) {
        val am = c.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pending(c, slot, 0L))
    }
}

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val slot = intent.getIntExtra(TimerAlarm.EXTRA_SLOT, -1)
        val end = intent.getLongExtra(TimerAlarm.EXTRA_END, 0L)
        if (slot !in 0 until DecoLogic.SLOTS) return
        val app = context.applicationContext
        val cfg = DecoStore.config(app, slot)
        // Si se canceló o se sustituyó por otro temporizador, esta alarma ya no aplica.
        if (cfg.mode != Mode.TIMER || cfg.timerEnd != end) return
        DecoStore.update(app, slot, immediate = true) {
            it.copy(timerEnd = 0L, result = app.getString(R.string.timer_done))
        }
        Haptics.alert(app)
    }
}
