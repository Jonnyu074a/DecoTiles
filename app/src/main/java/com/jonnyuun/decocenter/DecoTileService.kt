package com.jonnyuun.decocenter

import android.content.Intent
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.app.PendingIntent

/** Base de las 12 tarjetas (DecoTile1…12 se generan con tools/gen_tiles.py). */
abstract class DecoTileService : TileService() {

    /** Índice de ranura 0..11. */
    abstract val slot: Int

    private val handler = Handler(Looper.getMainLooper())
    private var torchCallback: CameraManager.TorchCallback? = null

    private val ticker = object : Runnable {
        override fun run() {
            refresh()
            if (isTimerRunning()) handler.postDelayed(this, 1000L)
        }
    }

    override fun onTileAdded() {
        refresh()
    }

    override fun onStartListening() {
        startTorchSync()
        refresh()
        handler.removeCallbacks(ticker)
        if (isTimerRunning()) handler.postDelayed(ticker, 1000L)
    }

    override fun onStopListening() {
        stopTorchSync()
        handler.removeCallbacks(ticker)
    }

    override fun onClick() {
        val cfg = DecoStore.config(this, slot)
        if (cfg.mode == Mode.LAUNCHER) {
            launchApp(cfg)
            return
        }
        val haptic = TapActions.perform(this, slot)
        if (haptic != null) Haptics.play(this, haptic)
        refresh()
        handler.removeCallbacks(ticker)
        if (isTimerRunning()) handler.postDelayed(ticker, 1000L)
    }

    private fun isTimerRunning(): Boolean =
        DecoStore.config(this, slot).isTimerRunning(System.currentTimeMillis())

    private fun refresh() {
        val tile = qsTile ?: return
        DecoStore.expireTimers(this)
        val cfg = DecoStore.config(this, slot)
        val now = System.currentTimeMillis()
        val title = cfg.title(this, slot)
        val sub = cfg.subtitle(this, now)
        tile.icon = DecoIcons.icon(cfg.glyph)
        if (Build.VERSION.SDK_INT >= 29) {
            tile.label = title
            tile.subtitle = sub
        } else {
            tile.label = if (sub != null) "$title · $sub" else title
        }
        tile.state = if (cfg.isActive(now)) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    // ------------------------------------------------------------ linterna: mantener el estado real

    private fun startTorchSync() {
        stopTorchSync()
        if (DecoStore.config(this, slot).mode != Mode.TORCH) return
        val cm = getSystemService(CameraManager::class.java) ?: return
        val torchId = Torch.cameraId(cm) ?: return
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId != torchId) return
                DecoStore.update(this@DecoTileService, slot) { it.copy(lit = enabled) }
                refresh()
            }
        }
        torchCallback = callback
        try {
            cm.registerTorchCallback(callback, handler)
        } catch (_: Exception) {
            torchCallback = null
        }
    }

    private fun stopTorchSync() {
        val callback = torchCallback ?: return
        torchCallback = null
        try {
            getSystemService(CameraManager::class.java)?.unregisterTorchCallback(callback)
        } catch (_: Exception) {
        }
    }

    // ------------------------------------------------------------ lanzador

    private fun launchApp(cfg: TileConfig) {
        val intent = if (cfg.pkg.isNotBlank()) packageManager.getLaunchIntentForPackage(cfg.pkg) else null
        if (intent == null) {
            Haptics.play(this, Haptic.ERROR)
            // Sin app elegida (o desinstalada): abre el ajuste de esta tarjeta.
            val settings = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startTileActivity(settings)
            return
        }
        Haptics.play(this, Haptic.CLICK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startTileActivity(intent)
    }

    private fun startTileActivity(intent: Intent) {
        val launch = {
            if (Build.VERSION.SDK_INT >= 34) {
                val pending = PendingIntent.getActivity(
                    this,
                    slot,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                startActivityAndCollapse(pending)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
        if (isLocked) unlockAndRun { launch() } else launch()
    }
}
