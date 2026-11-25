/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 * Tela inicial: estado do sistema ao vivo + biblioteca de jogos.
 */
package dev.sempaz.gametour.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.sempaz.gametour.Graph
import dev.sempaz.gametour.data.GameApp
import dev.sempaz.gametour.data.GameProfile
import dev.sempaz.gametour.sys.Batt
import dev.sempaz.gametour.sys.Mem
import dev.sempaz.gametour.sys.Sys
import dev.sempaz.gametour.sys.Thermal
import dev.sempaz.gametour.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

private class Snap(
    val batt: Batt, val dev: Float, val cpu: Float, val freqs: IntArray, val maxF: IntArray,
    val mem: Mem, val thermal: Int, val gpu: Int, val gpuPct: Float,
)

@Composable
fun HomeScreen(onGame: (String) -> Unit, onAdd: () -> Unit) {
    val ctx = LocalContext.current
    val sys = remember { Sys(ctx.applicationContext) }
    var snap by remember { mutableStateOf<Snap?>(null) }
    var games by remember { mutableStateOf<List<GameApp>>(emptyList()) }

    LaunchedEffect(Unit) {
        while (true) {
            snap = withContext(Dispatchers.Default) {
                Snap(sys.batt(), sys.deviceTemp(), sys.cpuUsage(), sys.freqsMhz(), sys.maxFreqsMhz(),
                    sys.mem(), Thermal.read(), sys.gpuMhz(), sys.gpuUtil())
            }
            games = withContext(Dispatchers.Default) { Graph.games.list() }
            delay(2000)
        }
    }

    val p = LocalPal.current
    Screen("Game Tour") {
        val s = snap
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat(tr("Bateria", "Battery"), if (s != null) "${s.batt.pct}%" else "—", Icons.Rounded.BatteryChargingFull,
                Modifier.weight(1f),
                sub = s?.let { String.format(Locale.US, "%.1f W · %.0f °C", it.batt.watts, it.batt.tempC) })
            Stat(tr("Aparelho", "Device"), if (s != null && s.dev > 0f) String.format(Locale.US, "%.0f °C", s.dev) else "—",
                Icons.Rounded.Thermostat, Modifier.weight(1f),
                sub = s?.let { tr("Perfil ", "Profile ") + GameProfile.thermalName(it.thermal) })
        }
        Spacer(Modifier.height(10.dp))
        GCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GIcon(Icons.Rounded.Memory, 15.dp, p.text2)
                    GText("CPU", 12, color = p.text2, modifier = Modifier.weight(1f))
                    if (s != null && s.cpu >= 0f) GText(String.format(Locale.US, "%.0f%%", s.cpu), 14)
                }
                if (s != null && s.cpu >= 0f) Bar(s.cpu / 100f)
                val names = listOf(tr("Pequenos", "Little"), tr("Grandes", "Big"), "Prime")
                for (i in 0..2) {
                    val f = s?.freqs?.get(i) ?: 0
                    val m = s?.maxF?.get(i) ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GText(names[i], 11, color = p.text2, modifier = Modifier.width(62.dp))
                        Bar(if (m > 0) f.toFloat() / m else 0f, Modifier.weight(1f))
                        GText(String.format(Locale.US, "%.1f GHz", f / 1000f), 11, color = p.text2,
                            modifier = Modifier.width(62.dp), align = androidx.compose.ui.text.style.TextAlign.End)
                    }
                }
            }
        }
        // GPU: uso em % e frequência. Só aparece se o aparelho deixar ler (some se o nó for negado).
        if (s != null && (s.gpuPct >= 0f || s.gpu > 0)) {
            Spacer(Modifier.height(10.dp))
            GCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GIcon(Icons.Rounded.Tune, 15.dp, p.text2)
                        GText("GPU", 12, color = p.text2, modifier = Modifier.weight(1f))
                        if (s.gpuPct >= 0f) GText(String.format(Locale.US, "%.0f%%", s.gpuPct), 14)
                    }
                    if (s.gpuPct >= 0f) Bar(s.gpuPct / 100f)
                    if (s.gpu > 0) GText(String.format(Locale.US, "%d MHz", s.gpu), 12, color = p.text2)
                }
            }
        }
        // RAM: uso em %, barra e usado / total
        if (s != null && s.mem.totalMb > 0) {
            val usedMb = s.mem.totalMb - s.mem.freeMb
            val pct = 100f * usedMb / s.mem.totalMb
            Spacer(Modifier.height(10.dp))
            GCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GIcon(Icons.Rounded.Memory, 15.dp, p.text2)
                        GText("RAM", 12, color = p.text2, modifier = Modifier.weight(1f))
                        GText(String.format(Locale.US, "%.0f%%", pct), 14)
                    }
                    Bar(pct / 100f)
                    GText(String.format(Locale.US, tr("%.1f de %.1f GB em uso", "%.1f of %.1f GB in use"),
                        usedMb / 1024f, s.mem.totalMb / 1024f), 12, color = p.text2)
                }
            }
        }
        SectionLabel(tr("Seus jogos", "Your games"))
        if (games.isEmpty()) {
            GCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GText(tr("Nenhum jogo na lista", "No games yet"), 16, androidx.compose.ui.text.font.FontWeight.SemiBold)
                    GText(tr("Adicione um jogo para ter perfil, FPS e relatório.", "Add a game to get a profile, FPS and a report."), 13, color = p.text2)
                    GButton(tr("Adicionar jogo", "Add game"), true, onAdd)
                }
            }
        } else {
            GameGrid(games) { onGame(it.pkg) }
        }
    }
}
