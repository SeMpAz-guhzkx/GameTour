/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 */
package dev.sempaz.gametour.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.sempaz.gametour.Graph
import dev.sempaz.gametour.data.GameApp

@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)

/** Tela com título grande que rola junto com o conteúdo. */
@Composable
fun Screen(title: String, action: String? = null, onAction: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val p = LocalPal.current
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            GText(title, 32, FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
            if (action != null) {
                GText(action, 15, FontWeight.Medium, p.accent,
                    Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onAction?.invoke() }
                        .padding(8.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        content()
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    GText(text, 13, FontWeight.SemiBold, LocalPal.current.text2, modifier.padding(start = 6.dp, top = 18.dp, bottom = 8.dp))
}

/** Ícone de jogo, ou uma inicial colorida se o ícone não existir. */
@Composable
fun GameIcon(pkg: String, label: String, size: Int = 56) {
    val img = remember(pkg) { Graph.games.icon(pkg) }
    val shape = RoundedCornerShape((size * 0.24f).dp)
    if (img != null) {
        Image(bitmap = img, contentDescription = null, modifier = Modifier.size(size.dp).clip(shape))
    } else {
        Box(
            Modifier.size(size.dp).clip(shape).background(Brush.linearGradient(listOf(Color(0xFFA07BFF), Color(0xFF5A39D6)))),
            contentAlignment = Alignment.Center,
        ) { GText(label.take(2).uppercase(), (size / 3), FontWeight.Bold, Color.White) }
    }
}

/** Grade de jogos, 4 por linha. */
@Composable
fun GameGrid(games: List<GameApp>, onClick: (GameApp) -> Unit) {
    val p = LocalPal.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        games.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { g ->
                    Column(
                        Modifier.weight(1f).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick(g) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        GameIcon(g.pkg, g.label, 60)
                        Spacer(Modifier.height(5.dp))
                        GText(g.label, 11, color = p.text2, maxLines = 1, align = TextAlign.Center)
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun Stat(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, sub: String? = null) {
    val p = LocalPal.current
    GCard(modifier) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GIcon(icon, 15.dp, p.text2)
                GText(label, 12, color = p.text2)
            }
            Spacer(Modifier.height(4.dp))
            GText(value, 24, FontWeight.SemiBold, maxLines = 1)
            if (sub != null) GText(sub, 12, color = p.text2, maxLines = 1)
        }
    }
}

@Composable
fun FullScrim(onClick: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() },
    )
}

@Composable
fun BottomSheetBox(content: @Composable () -> Unit) {
    val p = LocalPal.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier
                .padding(8.dp)
                .navigationBarsPadding()
                .fillMaxWidth()
                .glass(RoundedCornerShape(32.dp), strong = true)
                .background(p.sheet)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 38.dp, height = 5.dp).clip(RoundedCornerShape(50)).background(p.text2.copy(alpha = 0.5f)))
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
