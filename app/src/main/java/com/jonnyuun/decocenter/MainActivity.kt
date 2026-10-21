package com.jonnyuun.decocenter

import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.jonnyuun.decocenter.ui.DecoApp
import com.jonnyuun.decocenter.ui.DecoTheme

class MainActivity : ComponentActivity() {

    /** Ranura cuya configuración se abrió con una pulsación larga en el Centro de Control (-1 = ninguna). */
    private var requestedSlot by mutableIntStateOf(-1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DecoStore.init(this)
        requestedSlot = slotFrom(intent)

        setContent {
            val dark = isSystemInDarkTheme()
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
                onDispose { }
            }
            DecoTheme {
                DecoApp(
                    focusSlot = requestedSlot,
                    onFocusConsumed = { requestedSlot = -1 },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedSlot = slotFrom(intent)
    }

    override fun onResume() {
        super.onResume()
        DecoStore.expireTimers(this)
    }

    override fun onPause() {
        super.onPause()
        DecoStore.flush(this)
    }

    private fun slotFrom(intent: Intent?): Int {
        intent ?: return -1
        val name: ComponentName? = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_COMPONENT, ComponentName::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_COMPONENT)
        }
        return TILE_CLASSES.indexOfFirst { it.name == name?.className }
    }

    private companion object {
        const val EXTRA_COMPONENT = "android.intent.extra.COMPONENT_NAME"
    }
}
