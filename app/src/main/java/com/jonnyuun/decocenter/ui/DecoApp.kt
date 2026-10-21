package com.jonnyuun.decocenter.ui

import android.app.StatusBarManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jonnyuun.decocenter.Apps
import com.jonnyuun.decocenter.Backup
import com.jonnyuun.decocenter.DecoIcons
import com.jonnyuun.decocenter.DecoLogic
import com.jonnyuun.decocenter.DecoStore
import com.jonnyuun.decocenter.Haptics
import com.jonnyuun.decocenter.Hx
import com.jonnyuun.decocenter.Mode
import com.jonnyuun.decocenter.Presets
import com.jonnyuun.decocenter.R
import com.jonnyuun.decocenter.TapActions
import com.jonnyuun.decocenter.TileConfig
import com.jonnyuun.decocenter.title
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.SnackbarResult
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

private const val MAX_BACKUP_BYTES = 1 shl 20
private const val SLOTS_ITEM_INDEX = 2

@Composable
fun DecoApp(focusSlot: Int, onFocusConsumed: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    val configs = DecoStore.configs

    var expanded by rememberSaveable { mutableIntStateOf(-1) }
    var includeSecrets by rememberSaveable { mutableStateOf(false) }

    var showPresetDialog by rememberSaveable { mutableStateOf(false) }
    var presetIndex by rememberSaveable { mutableIntStateOf(0) }

    var showSymbolSheet by rememberSaveable { mutableStateOf(false) }
    var showAppSheet by rememberSaveable { mutableStateOf(false) }
    var sheetSlot by rememberSaveable { mutableIntStateOf(0) }

    // Reloj para las cuentas atrás; solo corre mientras haya algún temporizador en marcha.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val anyTimer = configs.any { it.isTimerRunning(now) }
    LaunchedEffect(anyTimer) {
        while (anyTimer) {
            delay(1000)
            now = System.currentTimeMillis()
            DecoStore.expireTimers(context, now)
        }
    }

    // Pulsación larga sobre una tarjeta del Centro de Control: abre su ajuste.
    LaunchedEffect(focusSlot) {
        if (focusSlot >= 0) {
            expanded = focusSlot
            listState.animateScrollToItem(SLOTS_ITEM_INDEX)
            onFocusConsumed()
        }
    }

    fun snack(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        scope.launch {
            val result = snackbar.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                withDismissAction = actionLabel == null,
            )
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }

    /** Reemplaza las 12 tarjetas y ofrece deshacer. */
    fun replaceWithUndo(newConfigs: List<TileConfig>, message: String) {
        val before = DecoStore.configs
        DecoStore.replaceAll(context, newConfigs)
        snack(message, context.getString(R.string.undo)) { DecoStore.replaceAll(context, before) }
    }

    fun requestAdd(slot: Int) {
        if (Build.VERSION.SDK_INT < 33) return
        DecoStore.flush(context) // la tarjeta debe nacer con la configuración vigente
        val cfg = DecoStore.configs[slot]
        try {
            context.getSystemService(StatusBarManager::class.java).requestAddTileService(
                DecoStore.component(context, slot),
                cfg.title(context, slot),
                DecoIcons.icon(cfg.glyph),
                context.mainExecutor,
            ) { }
        } catch (_: Exception) {
            snack(context.getString(R.string.add_manual_hint))
        }
    }

    fun onPreviewTap(slot: Int) {
        val cfg = DecoStore.configs[slot]
        if (cfg.mode == Mode.LAUNCHER) {
            Hx.tap(view)
            val app = Apps.label(context, cfg.pkg)
            snack(
                if (app != null) {
                    context.getString(R.string.would_open, app)
                } else {
                    context.getString(R.string.pick_app_in_card)
                },
            )
            return
        }
        val haptic = TapActions.perform(context, slot)
        if (haptic != null) Haptics.play(context, haptic) else Hx.tap(view)
    }

    // Copia de seguridad ------------------------------------------------------------------

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = Backup.toJson(DecoStore.configs, includeSecrets)
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri, "wt")!!.use {
                            it.write(json.toByteArray(Charsets.UTF_8))
                        }
                    }.isSuccess
                }
                snack(context.getString(if (ok) R.string.backup_exported else R.string.backup_export_failed))
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { it.readLimited(MAX_BACKUP_BYTES) }
                    }.getOrNull()
                }
                val imported = text?.let { Backup.fromJson(it, DecoStore.configs) }
                if (imported == null) {
                    snack(context.getString(R.string.backup_invalid))
                } else {
                    replaceWithUndo(imported, context.getString(R.string.backup_imported))
                }
            }
        }
    }

    // Interfaz --------------------------------------------------------------------------

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.app_name),
                subtitle = stringResource(R.string.app_subtitle),
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(state = snackbar) },
    ) { innerPadding ->
        val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        Box {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = maxOf(innerPadding.calculateBottomPadding(), imeBottom) + 24.dp,
                ),
            ) {
                item(key = "preview") {
                    SmallTitle(text = stringResource(R.string.section_preview))
                    PreviewPanel(
                        configs = configs,
                        now = now,
                        onTap = { slot -> onPreviewTap(slot) },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    SmallTitle(text = stringResource(R.string.section_presets))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(Presets.all) { index, preset ->
                            TextButton(
                                text = "${preset.glyph}  ${preset.name}",
                                onClick = {
                                    Hx.tap(view)
                                    presetIndex = index
                                    showPresetDialog = true
                                },
                            )
                        }
                    }
                }

                item(key = "slots-title") {
                    SmallTitle(text = stringResource(R.string.section_cards))
                }

                item(key = "slots") {
                    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                        for (slot in 0 until DecoLogic.SLOTS) {
                            SlotItem(
                                slot = slot,
                                cfg = configs[slot],
                                now = now,
                                expanded = expanded == slot,
                                onToggle = {
                                    Hx.tap(view)
                                    expanded = if (expanded == slot) -1 else slot
                                },
                                onPickSymbol = {
                                    sheetSlot = slot
                                    showSymbolSheet = true
                                },
                                onPickApp = {
                                    sheetSlot = slot
                                    showAppSheet = true
                                },
                                onAddToPanel = { requestAdd(slot) },
                            )
                        }
                    }
                }

                item(key = "backup") {
                    SmallTitle(text = stringResource(R.string.section_backup))
                    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                        ArrowPreference(
                            title = stringResource(R.string.backup_export),
                            summary = stringResource(R.string.backup_export_summary),
                            onClick = { exportLauncher.launch(context.getString(R.string.backup_filename)) },
                        )
                        SwitchPreference(
                            checked = includeSecrets,
                            onCheckedChange = { includeSecrets = it },
                            title = stringResource(R.string.backup_include_secrets),
                            summary = stringResource(R.string.backup_include_secrets_summary),
                        )
                        ArrowPreference(
                            title = stringResource(R.string.backup_import),
                            summary = stringResource(R.string.backup_import_summary),
                            onClick = { importLauncher.launch(arrayOf("*/*")) },
                        )
                    }
                }

                item(key = "help") {
                    SmallTitle(text = stringResource(R.string.section_help))
                    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                        Text(
                            text = stringResource(R.string.help_text),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            // Diálogos y hojas (se dibujan en el host de popups del Scaffold).
            val preset = Presets.all[presetIndex.coerceIn(0, Presets.all.lastIndex)]
            OverlayDialog(
                show = showPresetDialog,
                title = stringResource(R.string.preset_title, preset.name),
                summary = stringResource(R.string.preset_message),
                onDismissRequest = { showPresetDialog = false },
            ) {
                Row {
                    TextButton(
                        text = stringResource(R.string.cancel),
                        onClick = { showPresetDialog = false },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(20.dp))
                    TextButton(
                        text = stringResource(R.string.apply),
                        onClick = {
                            showPresetDialog = false
                            Hx.confirm(view)
                            replaceWithUndo(
                                Presets.apply(DecoStore.configs, preset),
                                context.getString(R.string.preset_applied, preset.name),
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }

            SymbolSheet(
                show = showSymbolSheet,
                current = configs[sheetSlot].symbol,
                onDismiss = { showSymbolSheet = false },
                onPick = { glyph ->
                    showSymbolSheet = false
                    Hx.confirm(view)
                    DecoStore.update(context, sheetSlot) { it.copy(symbol = glyph) }
                },
            )

            AppSheet(
                show = showAppSheet,
                currentPkg = configs[sheetSlot].pkg,
                onDismiss = { showAppSheet = false },
                onPick = { pkg ->
                    showAppSheet = false
                    Hx.confirm(view)
                    DecoStore.update(context, sheetSlot) { it.copy(pkg = pkg) }
                },
                onClear = {
                    showAppSheet = false
                    DecoStore.update(context, sheetSlot) { it.copy(pkg = "") }
                },
            )
        }
    }
}

/** Lee como máximo [max] bytes; devuelve null si el archivo es más grande (no es una copia válida). */
private fun InputStream.readLimited(max: Int): String? {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val n = read(buffer)
        if (n < 0) break
        if (out.size() + n > max) return null
        out.write(buffer, 0, n)
    }
    return out.toString("UTF-8")
}
