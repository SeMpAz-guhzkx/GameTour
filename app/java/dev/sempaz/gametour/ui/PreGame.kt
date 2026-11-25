/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 * Pré-jogo: checklist, perfil e botões Iniciar / Pular.
 */
package dev.sempaz.gametour.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.sempaz.gametour.Graph
import dev.sempaz.gametour.data.GameProfile
import dev.sempaz.gametour.sys.Bypass
import dev.sempaz.gametour.sys.BtInfo
import dev.sempaz.gametour.sys.Sys
import dev.sempaz.gametour.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private class Check(val label: String, val value: String, val level: Int) // 0 ok, 1 atenção

@Composable
fun PreGameSheet(pkg: String, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val p = LocalPal.current
    val label = remember(pkg) { Graph.games.label(pkg) ?: pkg }
    var profile by remember(pkg) { mutableStateOf(Graph.prefs.profileFor(pkg)) }
    fun update(n: GameProfile) { profile = n; Graph.prefs.setProfile(pkg, n) }

    val checks by produceState(emptyList<Check>(), pkg) {
        value = withContext(Dispatchers.Default) {
            val sys = Sys(ctx.applicationContext)
            val b = sys.batt()
            val dev = sys.deviceTemp()
            val m = sys.mem()
            val st = sys.storageFreeGb()
            val ms = sys.latencyMs(Graph.prefs.pingHost)
            val net = sys.networkType()
            listOf(
                Check(tr("Bateria", "Battery"), "${b.pct}%" + if (b.charging) " ⚡" else "", if (b.pct >= 30 || b.charging) 0 else 1),
                Check(tr("Temperatura", "Temperature"), String.format(Locale.US, "%.0f °C", if (dev > 0f) dev else b.tempC), if ((if (dev > 0f) dev else b.tempC) < 40f) 0 else 1),
                Check(tr("RAM livre", "Free RAM"), String.format(Locale.US, "%.1f GB", m.freeMb / 1024f), if (m.freeMb >= 2048) 0 else 1),
                Check(tr("Espaço livre", "Free space"), String.format(Locale.US, "%.0f GB", st), if (st >= 5f) 0 else 1),
                Check(net, if (ms >= 0) "$ms ms" else tr("sem resposta", "no reply"), if (ms in 0..80) 0 else 1),
            )
        }
    }
    val bt by produceState<BtInfo?>(null, pkg) { value = withContext(Dispatchers.Default) { Graph.audio.info() } }
    val spatialOk = remember { Graph.audio.spatialAvailable() }

    Box(Modifier.fillMaxSize()) {
        FullScrim(onClose)
        BottomSheetBox {
            Column(
                Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GameIcon(pkg, label, 52)
                    Column(Modifier.weight(1f)) {
                        GText(label, 18, FontWeight.SemiBold, maxLines = 1)
                        GText(tr("Antes de começar", "Before you start"), 12, color = p.text2)
                    }
                }

                // checklist
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    checks.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { c ->
                                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    GIcon(if (c.level == 0) Icons.Rounded.CheckCircle else Icons.Rounded.Warning, 16.dp,
                                        if (c.level == 0) GT.good else GT.warn)
                                    GText("${c.label} ${c.value}", 12, maxLines = 1)
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                val presets = listOf(GameProfile.THERMAL_DEFAULT, GameProfile.THERMAL_GAME, GameProfile.THERMAL_PERFORMANCE)
                Segmented(
                    listOf(tr("Equilibrado", "Balanced"), tr("Jogo", "Game"), tr("Desempenho", "Performance")),
                    presets.indexOf(profile.thermal).let { if (it < 0) 1 else it },
                    { update(profile.copy(thermal = presets[it])) },
                )

                ListGroup {
                    ListRow(Icons.Rounded.DoNotDisturbOn, tr("Não perturbe", "Do not disturb"),
                        trailing = { IosSwitch(profile.dnd) { update(profile.copy(dnd = it)) } })
                    if (Bypass.supported()) ListRow(Icons.Rounded.Bolt, tr("Bypass de carga", "Charge bypass"),
                        trailing = { IosSwitch(profile.bypass) { update(profile.copy(bypass = it)) } })
                    ListRow(Icons.Rounded.Speed, tr("FPS na tela", "FPS overlay"),
                        trailing = { IosSwitch(profile.fps) { update(profile.copy(fps = it)) } })
                    ListRow(Icons.Rounded.CleaningServices, tr("Limpar RAM ao abrir", "Clean RAM on start"),
                        trailing = { IosSwitch(profile.cleanRam) { update(profile.copy(cleanRam = it)) } })
                    ListRow(Icons.Rounded.Brightness6, tr("Brilho manual", "Manual brightness"),
                        trailing = { IosSwitch(profile.manualBrightness) { update(profile.copy(manualBrightness = it)) } })
                    if (spatialOk) ListRow(Icons.Rounded.GraphicEq, tr("Som espacial", "Spatial audio"),
                        trailing = { IosSwitch(profile.spatial == 1) { update(profile.copy(spatial = if (it) 1 else -1)) } })
                }

                bt?.let { info ->
                    GCard(Modifier.fillMaxWidth(), padding = 12.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GIcon(Icons.Rounded.BluetoothAudio, 18.dp, p.text2)
                                GText(info.name, 14, maxLines = 1, modifier = Modifier.weight(1f))
                                GText(tr("Codec: ", "Codec: ") + info.codec, 12, color = p.text2)
                            }
                            val opts = listOf(tr("Auto", "Auto")) + info.available
                            Segmented(opts, opts.indexOf(profile.codec).let { if (it < 0) 0 else it },
                                { update(profile.copy(codec = if (it == 0) "" else opts[it])) })
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GButton(tr("Pular", "Skip"), false, {
                        Graph.prefs.skipOnce = pkg
                        if (!Graph.games.isGame(pkg)) Graph.games.add(pkg)
                        Graph.games.launch(pkg); onClose()
                    }, Modifier.weight(1f))
                    GButton(tr("Iniciar", "Start"), true, {
                        Graph.prefs.setProfile(pkg, profile)
                        if (!Graph.games.isGame(pkg)) Graph.games.add(pkg)
                        Graph.games.launch(pkg); onClose()
                    }, Modifier.weight(1f))
                }
                if (Graph.games.isGame(pkg)) {
                    GText(tr("Remover da lista", "Remove from list"), 13, FontWeight.Medium, GT.bad,
                        Modifier.fillMaxWidth().clickableNoRipple { Graph.games.remove(pkg); onClose() }.padding(8.dp),
                        align = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}
