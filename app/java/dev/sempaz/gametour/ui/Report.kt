/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 * Relatório da sessão: FPS máx/média/mín, 1% e 0,1% low, estabilidade e "por que caiu".
 */
package dev.sempaz.gametour.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.sempaz.gametour.data.Cause
import dev.sempaz.gametour.data.SessionData
import dev.sempaz.gametour.tr
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private fun fmtDur(sec: Int): String = if (sec >= 3600) String.format(Locale.US, "%dh %02dmin", sec / 3600, (sec % 3600) / 60)
else String.format(Locale.US, "%d min %02ds", sec / 60, sec % 60)

private fun f0(v: Float) = String.format(Locale.US, "%.0f", v)

fun summaryText(s: SessionData): String = buildString {
    appendLine("${s.label} · ${fmtDur(s.seconds)}")
    appendLine("FPS: máx ${f0(s.fpsMax)} · média ${f0(s.fpsAvg)} · mín ${f0(s.fpsMin)}")
    s.frame?.let { appendLine("1% low ${f0(it.low1)} · 0,1% low ${f0(it.low01)}") }
    appendLine("Estabilidade ${s.stability} · temp. máx ${f0(s.devMax)} °C · bateria -${s.battUsed}%")
    append("Perfil: ${s.profile}")
}

@Composable
fun ReportScreen(s: SessionData, onClose: () -> Unit, onDelete: () -> Unit) {
    val ctx = LocalContext.current
    val p = LocalPal.current
    var copied by remember { mutableStateOf(false) }
    val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(s.start))

    Screen(tr("Sessão encerrada", "Session ended"), action = tr("Fechar", "Close"), onAction = onClose) {
        GText("${s.label} · ${fmtDur(s.seconds)} · $date", 13, color = p.text2)
        Spacer(Modifier.height(10.dp))

        GCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Ring(s.stability)
                Column {
                    GText(tr("FPS médio", "Average FPS"), 12, color = p.text2)
                    GText(f0(s.fpsAvg), 32, FontWeight.Bold)
                    GText(tr("máx ", "max ") + f0(s.fpsMax) + tr(" · mín ", " · min ") + f0(s.fpsMin), 12, color = p.text2)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat(if (s.frame != null) f0(s.frame.low1) else "—", "1% low", Modifier.weight(1f))
            MiniStat(if (s.frame != null) f0(s.frame.low01) else "—", tr("0,1% low", "0.1% low"), Modifier.weight(1f))
            MiniStat("${f0(s.devMax)}°", tr("temp. máx.", "max temp"), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat(if (s.battUsed == 0) "0%" else "-${s.battUsed}%", tr("bateria", "battery"), Modifier.weight(1f))
            MiniStat(String.format(Locale.US, "%.1f W", s.wattsAvg), tr("consumo", "power"), Modifier.weight(1f))
            MiniStat(s.profile, tr("perfil", "profile"), Modifier.weight(1f))
        }
        if (s.gpuAvg >= 0f) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat(String.format(Locale.US, "%.0f%%", s.gpuAvg), tr("GPU média", "GPU avg"), Modifier.weight(1f))
                Spacer(Modifier.weight(2f))
            }
        }

        SectionLabel(tr("FPS e temperatura", "FPS and temperature"))
        GCard(Modifier.fillMaxWidth()) {
            Column {
                FpsChart(s, Modifier.fillMaxWidth().height(150.dp))
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Legend(GT.good, "FPS"); Legend(GT.warn, tr("Temperatura", "Temperature")); Legend(GT.bad, tr("Queda", "Drop"))
                }
                val b30 = s.secondsBelow(30); val b45 = s.secondsBelow(45); val b60 = s.secondsBelow(60)
                if (s.targetFps >= 60) {
                    Spacer(Modifier.height(6.dp))
                    GText(tr("Abaixo de 30 / 45 / 60 FPS: ", "Below 30 / 45 / 60 FPS: ") + "${b30}s / ${b45}s / ${b60}s", 11, color = p.text2)
                }
            }
        }

        SectionLabel(tr("Por que o FPS caiu?", "Why did FPS drop?"))
        GCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (s.drops.isEmpty()) {
                    GText(tr("Sem quedas relevantes. A sessão ficou estável.", "No significant drops. The session was stable."), 14)
                } else s.drops.forEach { d ->
                    val cause = when (d.cause) {
                        Cause.HEAT -> tr("provável calor", "likely heat")
                        Cause.LOW_BATTERY -> tr("bateria baixa", "low battery")
                        Cause.UNKNOWN -> tr("causa não identificada pelo sistema (rede ou o próprio jogo)", "no system cause found (network or the game itself)")
                    }
                    GText("${d.t / 60}:${String.format(Locale.US, "%02d", d.t % 60)}  ${f0(d.from)} → ${f0(d.to)} FPS · $cause", 13)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        GCard(Modifier.fillMaxWidth(), padding = 12.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GIcon(Icons.Rounded.Lightbulb, 20.dp, GT.warn)
                GText(suggestion(s), 13, modifier = Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GButton(if (copied) tr("Copiado", "Copied") else tr("Copiar resumo", "Copy summary"), false, {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Game Tour", summaryText(s)))
                copied = true
            }, Modifier.weight(1f))
            GButton(tr("Apagar", "Delete"), false, onDelete, Modifier.weight(1f))
        }
    }
}

private fun suggestion(s: SessionData): String {
    val causes = s.drops.map { it.cause }.toSet()
    return when {
        Cause.HEAT in causes -> tr("Caiu por calor. Tente o perfil Desempenho ou ligue o bypass de carga para esquentar menos.",
            "Dropped due to heat. Try the Performance profile or turn on charge bypass to run cooler.")
        Cause.LOW_BATTERY in causes -> tr("A bateria estava baixa. Jogue carregando ou com mais de 20%.",
            "Battery was low. Play while charging or above 20%.")
        causes.isNotEmpty() -> tr("A queda não veio de calor nem de bateria. Pode ser rede ou o próprio jogo.",
            "The drop wasn't heat or battery. It may be network or the game itself.")
        s.stability >= 85 -> tr("Sessão estável. Nada a ajustar.", "Stable session. Nothing to adjust.")
        else -> tr("O FPS oscilou sem uma queda clara. Compare com outras sessões no histórico.",
            "FPS fluctuated without a clear drop. Compare with other sessions in History.")
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier) {
    GCard(modifier, padding = 10.dp) {
        Column {
            GText(value, if (value.length > 7) 14 else 18, FontWeight.SemiBold, maxLines = 1)
            GText(label, 11, color = LocalPal.current.text2, maxLines = 1)
        }
    }
}

@Composable
private fun Legend(c: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(8.dp).background(c, CircleShape))
        GText(label, 11, color = LocalPal.current.text2)
    }
}

@Composable
fun FpsChart(s: SessionData, modifier: Modifier = Modifier) {
    val p = LocalPal.current
    Canvas(modifier) {
        val n = s.samples
        if (n.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val top = (maxOf(s.targetFps * 1.1f, s.fpsMax * 1.05f)).coerceAtLeast(30f)
        val tMax = n.last().t.coerceAtLeast(1)
        fun x(t: Int) = w * t / tMax
        fun yFps(v: Float) = h - h * (v / top).coerceIn(0f, 1f)
        for (i in 1..3) drawLine(p.track, Offset(0f, h * i / 4f), Offset(w, h * i / 4f), 1.dp.toPx())

        val minT = n.minOf { it.dev }.coerceAtMost(n.maxOf { it.dev } - 1f)
        val maxT = n.maxOf { it.dev }.coerceAtLeast(minT + 1f)
        val tp = Path()
        n.forEachIndexed { i, sm ->
            val yy = h - h * 0.9f * ((sm.dev - minT) / (maxT - minT)) - h * 0.05f
            if (i == 0) tp.moveTo(x(sm.t), yy) else tp.lineTo(x(sm.t), yy)
        }
        drawPath(tp, GT.warn, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))

        val fp = Path()
        var started = false
        for (sm in n) {
            if (sm.fps <= 1f) continue
            if (!started) { fp.moveTo(x(sm.t), yFps(sm.fps)); started = true } else fp.lineTo(x(sm.t), yFps(sm.fps))
        }
        drawPath(fp, GT.good, style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        for (d in s.drops) {
            val c = Offset(x(d.t), yFps(d.to))
            drawCircle(Color.White, 5.dp.toPx(), c)
            drawCircle(GT.bad, 5.dp.toPx(), c, style = Stroke(2.5.dp.toPx()))
        }
    }
}
