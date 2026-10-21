package com.jonnyuun.decocenter.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonnyuun.decocenter.AppEntry
import com.jonnyuun.decocenter.Apps
import com.jonnyuun.decocenter.DecoLogic
import com.jonnyuun.decocenter.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.basic.Close
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

// ------------------------------------------------------------------ selector de símbolos

private const val COLUMNS = 6

private val SYMBOL_GROUPS: List<Pair<Int, List<String>>> = listOf(
    R.string.symbols_sky to listOf(
        "✦", "✧", "★", "☆", "✩", "✪", "☾", "☽", "☀", "☁", "☂", "❄",
        "⚡", "✨", "🌙", "☄", "🌈", "🔥",
    ),
    R.string.symbols_shapes to listOf(
        "◆", "◇", "●", "○", "▲", "▼", "■", "□", "◈", "❖", "▪", "◐",
        "◑", "━", "✕", "✚", "✓", "∞",
    ),
    R.string.symbols_hearts to listOf(
        "♡", "♥\uFE0E", "❤", "✿", "❀", "❁", "☘", "🌸", "🌷", "🌼", "☺", "☻",
    ),
    R.string.symbols_tools to listOf(
        "🔦", "🔋", "⏱", "📋", "🎲", "🪙", "💧", "✅", "📝", "🚀", "🧠", "☕",
        "🎧", "📷", "🔒", "⭐", "🎯", "♪",
    ),
)

@Composable
fun SymbolSheet(
    show: Boolean,
    current: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    OverlayBottomSheet(
        show = show,
        title = stringResource(R.string.pick_symbol),
        onDismissRequest = onDismiss,
        startAction = {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = MiuixIcons.Basic.Close,
                    contentDescription = stringResource(R.string.cancel),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 440.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            for ((titleRes, glyphs) in SYMBOL_GROUPS) {
                SmallTitle(text = stringResource(titleRes))
                for (line in glyphs.chunked(COLUMNS)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (glyph in line) {
                            GlyphCell(
                                glyph = glyph,
                                selected = glyph == current,
                                onClick = { onPick(glyph) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(COLUMNS - line.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlyphCell(
    glyph: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MiuixTheme.colorScheme
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) scheme.primary else scheme.secondaryContainer)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            color = if (selected) scheme.onPrimary else scheme.onSecondaryContainer,
            fontSize = (22f * DecoLogic.glyphScale(glyph)).sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

// ------------------------------------------------------------------ selector de aplicación

@Composable
fun AppSheet(
    show: Boolean,
    currentPkg: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    onClear: () -> Unit,
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }

    // La lista se carga fuera del hilo principal cada vez que se abre el selector.
    val apps by produceState<List<AppEntry>?>(initialValue = null, key1 = show) {
        if (show) value = withContext(Dispatchers.Default) { Apps.launchable(context) }
    }
    val filtered = remember(apps, query) {
        apps.orEmpty().filter { it.label.contains(query.trim(), ignoreCase = true) }
    }

    OverlayBottomSheet(
        show = show,
        title = stringResource(R.string.pick_app),
        onDismissRequest = onDismiss,
        startAction = {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = MiuixIcons.Basic.Close,
                    contentDescription = stringResource(R.string.cancel),
                )
            }
        },
        endAction = {
            if (currentPkg.isNotBlank()) {
                TextButton(text = stringResource(R.string.remove_app), onClick = onClear)
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            TextField(
                value = query,
                onValueChange = { query = it },
                label = stringResource(R.string.search_apps),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = MiuixIcons.Basic.Search,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            if (apps == null) {
                Text(
                    text = stringResource(R.string.loading),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    items(filtered, key = { it.pkg }) { app ->
                        BasicComponent(
                            title = app.label,
                            startAction = { AppIcon(app.pkg) },
                            endActions = {
                                if (app.pkg == currentPkg) {
                                    Icon(
                                        imageVector = MiuixIcons.Basic.Check,
                                        contentDescription = stringResource(R.string.a11y_selected),
                                        tint = MiuixTheme.colorScheme.primary,
                                    )
                                }
                            },
                            onClick = { onPick(app.pkg) },
                            role = Role.Button,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AppIcon(pkg: String) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = pkg) {
        value = withContext(Dispatchers.Default) { Apps.icon(context, pkg) }
    }
    Box(modifier = Modifier.size(40.dp)) {
        bitmap?.let {
            Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize())
        }
    }
}
