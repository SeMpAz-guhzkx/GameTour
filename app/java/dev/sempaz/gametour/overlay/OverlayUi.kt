/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 * Conteúdo (Compose) das janelas do overlay.
 */
package dev.sempaz.gametour.overlay

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import dev.sempaz.gametour.data.Metric
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.axion.platform.AxPlatformClient
import dev.sempaz.gametour.Graph
import dev.sempaz.gametour.data.GameProfile
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import dev.sempaz.gametour.sys.Bypass
import dev.sempaz.gametour.sys.Freeform
import dev.sempaz.gametour.sys.Thermal
import dev.sempaz.gametour.tr
import dev.sempaz.gametour.ui.GameIcon
import dev.sempaz.gametour.ui.GIcon
import dev.sempaz.gametour.ui.GT
import dev.sempaz.gametour.ui.GText
import dev.sempaz.gametour.ui.LocalPal
import dev.sempaz.gametour.ui.Segmented
import dev.sempaz.gametour.ui.clickableNoRipple
import dev.sempaz.gametour.ui.glass
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Rótulo de medidores: plano e leve, sem desfoque. Mostra só o que o usuário escolheu
 * (Ajustes > Medidores no jogo). Segure e arraste para mover para onde quiser.
 */
@Composable
fun FpsLabel(onDrag: (Float, Float) -> Unit, onDragEnd: () -> Unit) {
    val live by Graph.controller.live.collectAsState()
    val metrics = Graph.prefs.metrics
    val haptic = LocalHapticFeedback.current
    val fps = live.fps
    val white = Color.White
    val chunks = ArrayList<Triple<String, String, Color>>()
    for (m in Metric.values()) {
        if (m !in metrics) continue
        when (m) {
            Metric.FPS -> chunks.add(Triple(String.format(Locale.US, "%.0f", fps), "FPS", Color(0xFF7DFFB9)))
            Metric.FRAME -> if (fps > 1f) chunks.add(Triple(String.format(Locale.US, "%.1f", 1000f / fps), "ms", white))
            Metric.CPU -> if (live.cpu >= 0f) chunks.add(Triple("${live.cpu.roundToInt()}", "% CPU", Color(0xFF8CC8FF)))
            Metric.CPU_FREQ -> if (live.cpuMhz > 0) chunks.add(Triple(String.format(Locale.US, "%.1f", live.cpuMhz / 1000f), "GHz", Color(0xFF8CC8FF)))
            Metric.GPU -> if (live.gpu >= 0f) chunks.add(Triple("${live.gpu.roundToInt()}", "% GPU", Color(0xFFC9A8FF)))
            Metric.GPU_FREQ -> if (live.gpuMhz > 0) chunks.add(Triple("${live.gpuMhz}", "MHz GPU", Color(0xFFC9A8FF)))
            Metric.DEV_TEMP -> if (live.dev > 0f) chunks.add(Triple(String.format(Locale.US, "%.0f", live.dev), "°C", Color(0xFFFFB84D)))
            Metric.BATT_TEMP -> if (live.battTemp > 0f) chunks.add(Triple(String.format(Locale.US, "%.0f", live.battTemp), "°C bat", Color(0xFFFFB84D)))
            Metric.BATT_PCT -> chunks.add(Triple("${live.battPct}", "% bat", Color(0xFF7DFFB9)))
            Metric.WATTS -> chunks.add(Triple(String.format(Locale.US, "%.1f", live.watts), "W", white))
            Metric.RAM -> chunks.add(Triple(String.format(Locale.US, "%.1f", live.ramFreeMb / 1024f), "GB", white))
        }
    }
    if (chunks.isEmpty()) chunks.add(Triple("—", "", white))
    Column(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag = { change, drag -> change.consume(); onDrag(drag.x, drag.y) },
                )
            }
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        chunks.chunked(4).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                rowItems.forEach { (v, u, c) ->
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        GText(v, 14, FontWeight.Bold, c)
                        if (u.isNotEmpty()) GText(u, 10, color = white.copy(alpha = 0.72f))
                    }
                }
            }
        }
    }
}

/** Pílula: toque abre o painel; segure e arraste para colocá-la onde quiser. */
@Composable
fun Pill(onOpen: () -> Unit, onDrag: (Float, Float) -> Unit, onDragEnd: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        Modifier
            .size(width = 38.dp, height = 56.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(Color.Black.copy(alpha = 0.42f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(19.dp))
            .pointerInput(Unit) { detectTapGestures { onOpen() } }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag = { change, drag -> change.consume(); onDrag(drag.x, drag.y) },
                )
            },
        contentAlignment = Alignment.Center,
    ) { GIcon(Icons.Rounded.SportsEsports, 22.dp, Color.White) }
}

/**
 * Alça de borda (estilo Game Turbo): uma faixa fina; puxe para dentro (ou toque) para abrir o painel.
 * Segure e arraste para reposicionar.
 */
@Composable
fun EdgeHandle(
    rightSide: Boolean,
    invisible: Boolean,
    onOpen: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val p = LocalPal.current
    val haptic = LocalHapticFeedback.current
    val dragging = remember { mutableStateOf(false) }
    // Sempre visível, como vidro líquido: translúcida e discreta; fica mais nítida só ao arrastar.
    val glassAlpha = if (dragging.value) 1f else 0.55f
    val shape = if (rightSide) RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)
    else RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)
    Box(
        Modifier
            .size(width = 36.dp, height = 96.dp)
            .pointerInput(rightSide) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { total = 0f },
                    onDragCancel = { total = 0f },
                ) { change, dx ->
                    if (dragging.value) return@detectHorizontalDragGestures
                    total += if (rightSide) -dx else dx
                    if (total > 40.dp.toPx()) { total = Float.NEGATIVE_INFINITY; change.consume(); onOpen() }
                }
            }
            .pointerInput(Unit) { detectTapGestures { onOpen() } }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); dragging.value = true },
                    onDragEnd = { dragging.value = false; onDragEnd() },
                    onDragCancel = { dragging.value = false; onDragEnd() },
                    onDrag = { change, drag -> change.consume(); onDrag(drag.x, drag.y) },
                )
            },
        contentAlignment = if (rightSide) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(width = 6.dp, height = 56.dp)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.34f * glassAlpha),
                            Color.White.copy(alpha = 0.12f * glassAlpha),
                            Color.White.copy(alpha = 0.26f * glassAlpha),
                        ),
                    ),
                )
                .border(0.6.dp, Color.White.copy(alpha = 0.30f * glassAlpha), shape),
        )
    }
}

/** Aviso de chamada: nome e botões Atender / Recusar. */
@Composable
fun CallBanner(name: String, onAccept: () -> Unit, onReject: () -> Unit) {
    val p = LocalPal.current
    Row(
        Modifier
            .widthIn(max = 420.dp)
            .glass(RoundedCornerShape(30.dp), strong = true)
            .background(p.sheet.copy(alpha = 0.80f))
            .padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            GText(tr("Chamada recebida", "Incoming call"), 11, color = p.text2)
            GText(name, 16, FontWeight.SemiBold, maxLines = 1)
        }
        Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFE5484D)).clickableNoRipple { onReject() },
            contentAlignment = Alignment.Center) { GIcon(Icons.Rounded.CallEnd, 22.dp, Color.White) }
        Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF34C759)).clickableNoRipple { onAccept() },
            contentAlignment = Alignment.Center) { GIcon(Icons.Rounded.Call, 22.dp, Color.White) }
    }
}

/** Banner de notificação no topo, discreto; toque para dispensar. */
@Composable
fun NotifBanner(appLabel: String, title: String, text: String, pkg: String, onDismiss: () -> Unit) {
    val p = LocalPal.current
    Row(
        Modifier
            .widthIn(max = 460.dp)
            .glass(RoundedCornerShape(22.dp), strong = true)
            .background(p.sheet.copy(alpha = 0.82f))
            .clickableNoRipple { onDismiss() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GameIcon(pkg, appLabel, 34)
        Column(Modifier.weight(1f)) {
            GText(if (title.isNotBlank()) "$appLabel · $title" else appLabel, 12, FontWeight.SemiBold, p.text2, maxLines = 1)
            if (text.isNotBlank()) GText(text, 13, maxLines = 2)
        }
    }
}

private class Tile(
    val label: String,
    val icon: ImageVector,
    val on: () -> Boolean,
    val action: () -> Unit,
)

/** Painel em jogo, em vidro. A janela já aplica o desfoque do fundo. */
@Composable
fun Panel(pkg: String, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val p = LocalPal.current
    var tick by remember { mutableIntStateOf(0) }   // força releitura dos estados ao tocar num tile
    var bright by remember { mutableFloatStateOf(Graph.tweaks.brightness() / 255f) }
    val side = Graph.prefs.pillSide

    // O cliente da plataforma é assíncrono: se ainda não conectou, a chamada seria descartada.
    // Ele é conectado já na abertura do jogo (GameOverlay.show); aqui só avisamos se faltar.
    fun platform(feature: String, delayMs: Long = 0L) {
        runCatching {
            val c = AxPlatformClient.getInstance()
            c.init(ctx.applicationContext)
            if (!c.isAvailable()) {
                Toast.makeText(ctx, tr("Conectando ao sistema, toque de novo", "Connecting to system, tap again"), Toast.LENGTH_SHORT).show()
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    runCatching { c.toggle(feature) }.onFailure { Log.w("GameTourPanel", "toggle $feature failed", it) }
                }, delayMs)
            }
        }.onFailure {
            Log.w("GameTourPanel", "platform $feature failed", it)
            Toast.makeText(ctx, tr("Indisponível nesta ROM", "Unavailable on this ROM"), Toast.LENGTH_SHORT).show()
        }
    }

    val tiles = listOf(
        Tile(tr("Não perturbe", "Do not disturb"), Icons.Rounded.DoNotDisturbOn,
            { Graph.tweaks.interruptionFilter() > android.app.NotificationManager.INTERRUPTION_FILTER_ALL },
            { Graph.tweaks.setDnd(Graph.tweaks.interruptionFilter() <= android.app.NotificationManager.INTERRUPTION_FILTER_ALL) }),
        Tile(tr("Gravar tela", "Record"), Icons.Rounded.Videocam, { false }, { onClose(); platform(AxPlatformClient.FEATURE_SCREEN_RECORD, 450L) }),
        Tile(tr("Captura", "Screenshot"), Icons.Rounded.PhotoCamera, { false }, { onClose(); platform(AxPlatformClient.FEATURE_SCREENSHOT, 450L) }),
        Tile(tr("Limpar RAM", "Clean RAM"), Icons.Rounded.CleaningServices, { false }, {
            val n = Graph.tweaks.cleanRam(pkg)
            Toast.makeText(ctx, tr("$n apps fechados", "$n apps closed"), Toast.LENGTH_SHORT).show()
        }),
        Tile(tr("FPS na tela", "FPS overlay"), Icons.Rounded.Speed, { Graph.overlay.fpsVisible() }, { Graph.overlay.setFps(!Graph.overlay.fpsVisible()) }),
        Tile(tr("Bypass", "Bypass"), Icons.Rounded.Bolt, { Bypass.enabled() }, {
            if (Bypass.supported()) Bypass.set(!Bypass.enabled())
        }),
        Tile(tr("Espacial", "Spatial"), Icons.Rounded.GraphicEq, { Graph.audio.spatialEnabled() }, {
            if (Graph.audio.spatialAvailable()) Graph.audio.setSpatial(!Graph.audio.spatialEnabled())
        }),
    )

    Box(Modifier.fillMaxSize().clickableNoRipple { onClose() }, contentAlignment = if (side < 0) Alignment.CenterStart else Alignment.CenterEnd) {
        Column(
            Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 380.dp)
                .heightIn(max = (LocalConfiguration.current.screenHeightDp - 24).dp)   // paisagem: nunca passa da tela
                .glass(RoundedCornerShape(30.dp), strong = true)
                .background(p.sheet.copy(alpha = 0.68f))
                .pointerInput(Unit) { detectTapGestures { } }   // não fecha ao tocar dentro do painel
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GText(tr("Painel de jogo", "Game panel"), 17, FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Box(Modifier.size(32.dp).clip(CircleShape).glass(CircleShape).clickableNoRipple { onClose() }, contentAlignment = Alignment.Center) {
                    GIcon(Icons.Rounded.Close, 18.dp)
                }
            }
            tiles.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { t ->
                        @Suppress("UNUSED_EXPRESSION") tick
                        val on = t.on()
                        Column(
                            Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(18.dp))
                                .then(if (on) Modifier.background(Brush.verticalGradient(listOf(lerp(p.accent, Color.White, 0.2f), p.accent2)))
                                else Modifier.glass(RoundedCornerShape(18.dp)))
                                .clickableNoRipple { t.action(); tick++ }
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            GIcon(t.icon, 22.dp, if (on) Color.White else p.text)
                            Spacer(Modifier.height(3.dp))
                            GText(t.label, 10, color = if (on) Color.White else p.text2, maxLines = 2, align = TextAlign.Center)
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            val quick = remember { Graph.prefs.quickApps }
            if (quick.isNotEmpty() && Freeform.available(ctx)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GText(tr("Apps em janela flutuante", "Floating-window apps"), 11, color = p.text2)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        quick.forEach { pkg ->
                            Box(Modifier.clip(CircleShape).clickableNoRipple { if (Freeform.launch(ctx, pkg)) onClose() }) {
                                GameIcon(pkg, Graph.games.label(pkg) ?: pkg, 44)
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GIcon(Icons.Rounded.Brightness6, 18.dp, p.text2)
                GlassSlider(bright, Modifier.weight(1f)) { v ->
                    bright = v
                    Graph.tweaks.setAutoBrightness(false)
                    Graph.tweaks.setBrightness((v * 255f).roundToInt())
                }
                GText("${(bright * 100).roundToInt()}%", 11, color = p.text2, modifier = Modifier.width(34.dp))
            }
            val presets = listOf(GameProfile.THERMAL_DEFAULT, GameProfile.THERMAL_GAME, GameProfile.THERMAL_PERFORMANCE)
            var cur by remember { mutableIntStateOf(Thermal.read()) }
            Segmented(
                listOf(tr("Equilibrado", "Balanced"), tr("Jogo", "Game"), tr("Desempenho", "Performance")),
                presets.indexOf(cur).let { if (it < 0) 1 else it },
                { Thermal.write(presets[it]); cur = presets[it] },
            )
        }
    }
}

@Composable
private fun GlassSlider(value: Float, modifier: Modifier = Modifier, onChange: (Float) -> Unit) {
    val p = LocalPal.current
    var widthPx by remember { mutableIntStateOf(1) }
    Box(
        modifier
            .height(28.dp)
            .pointerInput(Unit) { detectTapGestures { o -> onChange((o.x / widthPx).coerceIn(0.02f, 1f)) } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ -> change.consume(); onChange((change.position.x / widthPx).coerceIn(0.02f, 1f)) }
            }
            .androidxOnSize { widthPx = it },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(p.track))
        Box(Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).height(8.dp).clip(CircleShape)
            .background(Brush.horizontalGradient(listOf(lerp(p.accent, Color.White, 0.3f), p.accent))))
    }
}

private fun Modifier.androidxOnSize(cb: (Int) -> Unit): Modifier =
    this.onSizeChanged { cb(it.width) }
