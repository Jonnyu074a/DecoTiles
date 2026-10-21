package com.jonnyuun.decocenter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonnyuun.decocenter.DecoLogic
import com.jonnyuun.decocenter.R
import com.jonnyuun.decocenter.TileConfig
import com.jonnyuun.decocenter.subtitle
import com.jonnyuun.decocenter.title
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Simulador en vivo: las 12 tarjetas tal como se verán en el Centro de Control; se pueden tocar. */
@Composable
fun PreviewPanel(
    configs: List<TileConfig>,
    now: Long,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (row in 0 until DecoLogic.SLOTS / 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (col in 0..1) {
                        val slot = row * 2 + col
                        PreviewCell(
                            slot = slot,
                            cfg = configs[slot],
                            now = now,
                            onClick = { onTap(slot) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewCell(
    slot: Int,
    cfg: TileConfig,
    now: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scheme = MiuixTheme.colorScheme
    val active = cfg.isActive(now)
    val background = if (active) scheme.primary else scheme.secondaryContainer
    val foreground = if (active) scheme.onPrimary else scheme.onSecondaryContainer
    val circle = foreground.copy(alpha = if (active) 0.22f else 0.10f)

    val title = cfg.title(context, slot)
    val subtitle = cfg.subtitle(context, now)

    Row(
        modifier = modifier
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .clickable(
                role = Role.Button,
                onClickLabel = context.getString(R.string.a11y_tile_tap, title),
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(circle),
            contentAlignment = Alignment.Center,
        ) {
            // El símbolo es decorativo: el lector de pantalla lee el título y el estado.
            Text(
                text = cfg.glyph,
                color = foreground,
                fontSize = (20f * DecoLogic.glyphScale(cfg.glyph)).sp,
                maxLines = 1,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(
                text = title,
                color = foreground.copy(alpha = if (active) 1f else 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = foreground.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
