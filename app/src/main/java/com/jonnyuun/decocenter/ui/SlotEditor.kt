@file:OptIn(ExperimentalFoundationApi::class)

package com.jonnyuun.decocenter.ui

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonnyuun.decocenter.Apps
import com.jonnyuun.decocenter.DecoLogic
import com.jonnyuun.decocenter.DecoStore
import com.jonnyuun.decocenter.Hx
import com.jonnyuun.decocenter.Mode
import com.jonnyuun.decocenter.R
import com.jonnyuun.decocenter.TileConfig
import com.jonnyuun.decocenter.TimerAlarm
import com.jonnyuun.decocenter.subtitle
import com.jonnyuun.decocenter.title
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

/** Una fila de la lista: cabecera pulsable y, desplegado, el editor completo de la tarjeta. */
@Composable
fun SlotItem(
    slot: Int,
    cfg: TileConfig,
    now: Long,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPickSymbol: () -> Unit,
    onPickApp: () -> Unit,
    onAddToPanel: () -> Unit,
) {
    val context = LocalContext.current
    val requester = remember { BringIntoViewRequester() }
    // Al desplegar, la fila se desplaza a la vista una vez terminada la animación.
    LaunchedEffect(expanded) {
        if (expanded) {
            delay(250)
            requester.bringIntoView()
        }
    }

    val modeTitle = stringResource(cfg.mode.titleRes)
    val sub = cfg.subtitle(context, now)
    val summary = if (sub != null && cfg.mode != Mode.CLIPBOARD) "$modeTitle · $sub" else modeTitle
    val rotation by animateFloatAsState(targetValue = if (expanded) 90f else 0f, label = "chevron")

    Column(modifier = Modifier.bringIntoViewRequester(requester)) {
        BasicComponent(
            title = cfg.title(context, slot),
            summary = summary,
            startAction = { GlyphBadge(cfg.glyph) },
            endActions = {
                Icon(
                    imageVector = MiuixIcons.Basic.ArrowRight,
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer { rotationZ = rotation },
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                )
            },
            onClick = onToggle,
            onClickLabel = stringResource(if (expanded) R.string.a11y_collapse else R.string.a11y_expand),
            role = Role.Button,
        )
        AnimatedVisibility(visible = expanded) {
            SlotEditor(
                slot = slot,
                cfg = cfg,
                onPickSymbol = onPickSymbol,
                onPickApp = onPickApp,
                onAddToPanel = onAddToPanel,
            )
        }
    }
}

@Composable
private fun GlyphBadge(glyph: String) {
    Box(
        modifier = Modifier.size(44.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Decorativo: el título de la fila ya identifica la tarjeta.
        Text(
            text = glyph,
            color = MiuixTheme.colorScheme.onSurface,
            fontSize = (22f * DecoLogic.glyphScale(glyph)).sp,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

@Composable
private fun SlotEditor(
    slot: Int,
    cfg: TileConfig,
    onPickSymbol: () -> Unit,
    onPickApp: () -> Unit,
    onAddToPanel: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current

    fun edit(transform: (TileConfig) -> TileConfig) {
        DecoStore.update(context, slot, transform = transform)
    }

    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
        // Símbolo + nombre
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextField(
                value = cfg.symbol,
                onValueChange = { v -> edit { it.copy(symbol = v.take(DecoLogic.MAX_SYMBOL)) } },
                label = stringResource(R.string.field_symbol),
                singleLine = true,
                modifier = Modifier.weight(0.42f),
            )
            TextField(
                value = cfg.label,
                onValueChange = { v -> edit { it.copy(label = v.take(DecoLogic.MAX_LABEL)) } },
                label = stringResource(R.string.field_name),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        TextButton(
            text = stringResource(R.string.pick_symbol),
            onClick = onPickSymbol,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        // Función al tocar
        val modeItems = Mode.entries.map { stringResource(it.titleRes) }
        OverlayDropdownPreference(
            items = modeItems,
            selectedIndex = cfg.mode.ordinal,
            title = stringResource(R.string.field_mode),
            summary = stringResource(cfg.mode.hintRes),
            onSelectedIndexChange = { index ->
                val newMode = Mode.entries[index]
                if (cfg.mode == Mode.TIMER) TimerAlarm.cancel(context, slot)
                edit {
                    it.copy(
                        mode = newMode,
                        result = "",
                        timerEnd = 0L,
                        lit = if (newMode == Mode.TORCH) false else it.lit,
                    )
                }
            },
        )

        when (cfg.mode) {
            Mode.CLIPBOARD -> SecretTextField(
                value = cfg.text,
                onValueChange = { v -> edit { it.copy(text = v.take(DecoLogic.MAX_TEXT)) } },
            )

            Mode.NOTE -> TextField(
                value = cfg.text,
                onValueChange = { v -> edit { it.copy(text = v.take(DecoLogic.MAX_TEXT)) } },
                label = stringResource(R.string.field_note),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )

            Mode.LAUNCHER -> ArrowPreference(
                title = stringResource(R.string.field_app),
                summary = Apps.label(context, cfg.pkg) ?: stringResource(R.string.pick_app),
                onClick = onPickApp,
            )

            Mode.DICE -> {
                val diceItems = listOf(
                    stringResource(R.string.dice_d6),
                    stringResource(R.string.dice_coin),
                    stringResource(R.string.dice_d20),
                )
                OverlayDropdownPreference(
                    items = diceItems,
                    selectedIndex = cfg.dice.coerceIn(0, diceItems.lastIndex),
                    title = stringResource(R.string.field_dice),
                    onSelectedIndexChange = { index -> edit { it.copy(dice = index, result = "") } },
                )
            }

            Mode.TALLY -> ArrowPreference(
                title = stringResource(R.string.reset_counter, cfg.count),
                onClick = {
                    Hx.confirm(view)
                    edit { it.copy(count = 0) }
                },
            )

            Mode.TIMER -> SliderPreference(
                value = cfg.minutes.toFloat(),
                onValueChange = { v -> edit { it.copy(minutes = DecoLogic.clampMinutes(v.roundToInt())) } },
                title = stringResource(R.string.field_duration),
                valueText = stringResource(R.string.minutes_value, cfg.minutes),
                valueRange = DecoLogic.MIN_MINUTES.toFloat()..DecoLogic.MAX_MINUTES.toFloat(),
            )

            else -> Unit
        }

        // Estado resaltado: en Linterna y Temporizador el estado es real y no se edita a mano.
        if (cfg.mode != Mode.TORCH && cfg.mode != Mode.TIMER) {
            SwitchPreference(
                checked = cfg.lit,
                onCheckedChange = { on ->
                    Hx.toggle(view, on)
                    edit { it.copy(lit = on) }
                },
                title = stringResource(
                    if (cfg.mode == Mode.TOGGLE) R.string.lit_state else R.string.lit_look,
                ),
            )
        }

        if (Build.VERSION.SDK_INT >= 33) {
            Button(
                onClick = {
                    Hx.confirm(view)
                    onAddToPanel()
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Text(text = stringResource(R.string.add_to_panel), color = MiuixTheme.colorScheme.onPrimary)
            }
        }
    }
}

/** Texto sensible (DNI, IBAN...): enmascarado por defecto y con botón para mostrarlo. */
@Composable
private fun SecretTextField(value: String, onValueChange: (String) -> Unit) {
    var reveal by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = stringResource(R.string.field_clipboard),
            singleLine = true,
            visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.weight(1f),
        )
        TextButton(
            text = stringResource(if (reveal) R.string.hide else R.string.show),
            onClick = { reveal = !reveal },
        )
    }
}
