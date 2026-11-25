/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 * Histórico, jogos, ajustes (temas, apps rápidos, medidores) e as folhas de escolha de app.
 */
package dev.sempaz.gametour.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.sempaz.gametour.Graph
import dev.sempaz.gametour.data.GameApp
import dev.sempaz.gametour.data.GameProfile
import dev.sempaz.gametour.data.Metric
import dev.sempaz.gametour.data.SessionData
import dev.sempaz.gametour.sys.Freeform
import dev.sempaz.gametour.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(onOpen: (SessionData) -> Unit, refreshKey: Int) {
    val p = LocalPal.current
    val sessions by produceState<List<SessionData>?>(null, refreshKey) {
        value = withContext(Dispatchers.IO) { Graph.store.all() }
    }
    Screen(tr("Histórico", "History")) {
        val list = sessions
        if (list == null) {
            GText("…", 15, color = p.text2)
        } else if (list.isEmpty()) {
            GCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GText(tr("Sem sessões ainda", "No sessions yet"), 16, FontWeight.SemiBold)
                    GText(tr("Jogue por mais de 20 segundos e o relatório aparece aqui.", "Play for over 20 seconds and the report shows up here."), 13, color = p.text2)
                }
            }
        } else {
            val fmt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            ListGroup {
                list.forEach { s ->
                    Row(
                        Modifier.fillMaxWidth().clickableNoRipple { onOpen(s) }.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        GameIcon(s.pkg, s.label, 40)
                        Column(Modifier.weight(1f)) {
                            GText(s.label, 15, maxLines = 1)
                            GText(fmt.format(Date(s.start)) + " · ${s.seconds / 60} min", 12, color = p.text2, maxLines = 1)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            GText("${s.fpsAvg.toInt()} FPS", 14, FontWeight.SemiBold)
                            GText(tr("estab. ", "stab. ") + s.stability, 11, color = p.text2)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GamesScreen(onGame: (String) -> Unit, onAdd: () -> Unit, refreshKey: Int) {
    val games by produceState<List<GameApp>>(emptyList(), refreshKey) {
        value = withContext(Dispatchers.Default) { Graph.games.list() }
    }
    Screen(tr("Jogos", "Games"), action = tr("Adicionar", "Add"), onAction = onAdd) {
        if (games.isEmpty()) {
            GText(tr("Nenhum jogo na lista. Toque em Adicionar.", "No games yet. Tap Add."), 14, color = LocalPal.current.text2)
        } else {
            GameGrid(games) { onGame(it.pkg) }
        }
    }
}

private fun metricIcon(m: Metric): ImageVector = when (m) {
    Metric.FPS -> Icons.Rounded.Speed
    Metric.FRAME -> Icons.Rounded.Timer
    Metric.CPU, Metric.CPU_FREQ, Metric.RAM -> Icons.Rounded.Memory
    Metric.GPU, Metric.GPU_FREQ -> Icons.Rounded.Tune
    Metric.DEV_TEMP, Metric.BATT_TEMP -> Icons.Rounded.Thermostat
    Metric.BATT_PCT -> Icons.Rounded.BatteryChargingFull
    Metric.WATTS -> Icons.Rounded.Bolt
}

/** Cartão de pré-visualização de um tema. */
@Composable
private fun ThemeCard(t: ThemeDef, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val pal = t.pal
    val shape = RoundedCornerShape(20.dp)
    val cur = LocalPal.current
    Box(
        modifier
            .height(88.dp)
            .clip(shape)
            .background(pal.base)
            .border(if (selected) 2.dp else 1.dp, if (selected) cur.accent else Color.White.copy(alpha = 0.18f), shape)
            .clickableNoRipple(onClick),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            fun blob(c: Color, center: Offset, r: Float) = drawCircle(
                Brush.radialGradient(listOf(c, Color.Transparent), center = center, radius = r), radius = r, center = center)
            blob(pal.blobs[0], Offset(size.width * 0.15f, 0f), size.width * 0.7f)
            blob(pal.blobs[1], Offset(size.width, size.height * 0.4f), size.width * 0.6f)
            blob(pal.blobs[2], Offset(size.width * 0.4f, size.height), size.width * 0.7f)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            GText(tr(t.pt, t.en), 14, FontWeight.SemiBold, pal.text)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.height(8.dp).padding(end = 0.dp).clip(RoundedCornerShape(50)).background(pal.accent).padding(horizontal = 12.dp))
                Box(Modifier.height(8.dp).clip(RoundedCornerShape(50)).background(pal.accent2).padding(horizontal = 12.dp))
            }
        }
        if (selected) {
            Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) { GIcon(Icons.Rounded.CheckCircle, 20.dp, pal.accent) }
        }
    }
}

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val p = LocalPal.current
    var dnd by remember { mutableStateOf(Graph.prefs.autoDnd) }
    var def by remember { mutableStateOf(Graph.prefs.defaultProfile) }
    var metrics by remember { mutableStateOf(Graph.prefs.metrics) }
    var callsMode by remember { mutableStateOf(Graph.prefs.callsMode) }
    var callBanner by remember { mutableStateOf(Graph.prefs.callBanner) }
    var notifBanner by remember { mutableStateOf(Graph.prefs.notifBanner) }
    var theme by remember { mutableStateOf(ThemeHolder.id.value) }
    var quick by remember { mutableStateOf(Graph.prefs.quickApps) }
    var picking by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Screen(tr("Ajustes", "Settings")) {
            SectionLabel(tr("Tema", "Theme"))
            Themes.all.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { t ->
                        ThemeCard(t, t.id == theme, Modifier.weight(1f)) {
                            theme = t.id
                            ThemeHolder.id.value = t.id
                            Graph.prefs.theme = t.id
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
            }

            SectionLabel(tr("Padrões para novos jogos", "Defaults for new games"))
            ListGroup {
                ListRow(Icons.Rounded.DoNotDisturbOn, tr("Não perturbe automático", "Automatic Do Not Disturb"),
                    trailing = { IosSwitch(dnd) { dnd = it; Graph.prefs.autoDnd = it } })
                ListRow(Icons.Rounded.Speed, tr("FPS na tela", "FPS overlay"),
                    trailing = { IosSwitch(def.fps) { def = def.copy(fps = it); Graph.prefs.defaultProfile = def } })
            }
            val presets = listOf(GameProfile.THERMAL_DEFAULT, GameProfile.THERMAL_GAME, GameProfile.THERMAL_PERFORMANCE)
            Spacer(Modifier.height(10.dp))
            Segmented(
                listOf(tr("Equilibrado", "Balanced"), tr("Jogo", "Game"), tr("Desempenho", "Performance")),
                presets.indexOf(def.thermal).let { if (it < 0) 1 else it },
                { def = def.copy(thermal = presets[it]); Graph.prefs.defaultProfile = def },
            )

            SectionLabel(tr("Alça do painel", "Panel handle"))
            GText(
                tr("No jogo, puxe a alça da borda para dentro para abrir o painel. Segure e arraste para mudar a posição.",
                    "In-game, pull the edge handle inward to open the panel. Long-press and drag to move it."),
                12, color = p.text2, modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 8.dp),
            )
            SectionLabel(tr("Apps rápidos (janela flutuante)", "Quick apps (floating window)"))
            GText(
                tr("Os apps escolhidos aparecem no painel do jogo e abrem em janela flutuante.",
                    "The chosen apps show up in the game panel and open in a floating window."),
                12, color = p.text2, modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 8.dp),
            )
            if (!Freeform.available(ctx)) {
                GText(tr("Janela flutuante indisponível nesta ROM.", "Floating window unavailable on this ROM."), 12, color = GT.warn,
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
            }
            ListGroup {
                quick.forEach { pkg ->
                    val label = Graph.games.label(pkg) ?: pkg
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        GameIcon(pkg, label, 36)
                        GText(label, 15, modifier = Modifier.weight(1f), maxLines = 1)
                        Box(Modifier.clip(RoundedCornerShape(50)).clickableNoRipple {
                            quick = quick - pkg; Graph.prefs.quickApps = quick
                        }.padding(6.dp)) { GIcon(Icons.Rounded.Close, 18.dp, p.text2) }
                    }
                }
                ListRow(Icons.Rounded.Add, tr("Adicionar app", "Add app"), onClick = { picking = true })
            }

            SectionLabel(tr("Durante o jogo", "During the game"))
            GText(tr("Chamadas", "Calls"), 13, color = p.text2, modifier = Modifier.padding(start = 6.dp, bottom = 6.dp))
            Segmented(
                listOf(tr("Normal", "Normal"), tr("Atender", "Answer"), tr("Recusar", "Decline")),
                callsMode,
                { callsMode = it; Graph.prefs.callsMode = it },
            )
            Spacer(Modifier.height(10.dp))
            ListGroup {
                ListRow(Icons.Rounded.Call, tr("Aviso de chamada", "Call banner"),
                    trailing = { IosSwitch(callBanner) { callBanner = it; Graph.prefs.callBanner = it } })
                ListRow(Icons.Rounded.Notifications, tr("Notificações em banner", "Notification banners"),
                    trailing = { IosSwitch(notifBanner) { notifBanner = it; Graph.prefs.notifBanner = it } })
            }

            SectionLabel(tr("Medidores no jogo", "In-game readouts"))
            GText(
                tr("Escolha o que aparece no rótulo durante o jogo. No jogo, segure o rótulo e arraste para colocá-lo onde quiser.",
                    "Pick what the in-game label shows. In-game, long-press the label and drag it anywhere."),
                12, color = p.text2, modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 8.dp),
            )
            ListGroup {
                Metric.values().forEach { m ->
                    ListRow(metricIcon(m), m.label(), trailing = {
                        IosSwitch(m in metrics) { on ->
                            metrics = if (on) metrics + m else metrics - m
                            Graph.prefs.metrics = metrics
                        }
                    })
                }
            }

            SectionLabel(tr("Sobre", "About"))
            GCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GText("Game Tour", 16, FontWeight.SemiBold)
                    GText(tr("Sistema de jogo do POCO X6 Pro. Código aberto (Apache-2.0).", "Game system for the POCO X6 Pro. Open source (Apache-2.0)."), 12, color = p.text2)
                    GText(tr("Créditos: Xiaomi Parts (th4rv4), GameSpace (chaldeaprjkt e AxionOS). Mantenedor: SeMpAz-guhzkx.",
                        "Credits: Xiaomi Parts (th4rv4), GameSpace (chaldeaprjkt and AxionOS). Maintainer: SeMpAz-guhzkx."), 12, color = p.text2)
                }
            }
        }
        if (picking) {
            AppPickerSheet(
                title = tr("Adicionar app rápido", "Add quick app"),
                exclude = quick.toSet(),
                onClose = { picking = false },
                onPick = { pkg -> quick = quick + pkg; Graph.prefs.quickApps = quick; picking = false },
            )
        }
    }
}

/** Folha para escolher qualquer app instalado (apps rápidos). */
@Composable
fun AppPickerSheet(title: String, exclude: Set<String>, onClose: () -> Unit, onPick: (String) -> Unit) {
    val items by produceState<List<GameApp>>(emptyList()) {
        value = withContext(Dispatchers.Default) { Graph.games.launchable().filter { it.pkg !in exclude } }
    }
    Box(Modifier.fillMaxSize()) {
        FullScrim(onClose)
        BottomSheetBox {
            GText(title, 18, FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(items, key = { it.pkg }) { g ->
                    Row(
                        Modifier.fillMaxWidth().clickableNoRipple { onPick(g.pkg) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        GameIcon(g.pkg, g.label, 40)
                        GText(g.label, 15, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun AddGameSheet(onClose: () -> Unit, onAdded: () -> Unit) {
    val items by produceState<List<GameApp>>(emptyList()) { value = withContext(Dispatchers.Default) { Graph.games.candidates() } }
    Box(Modifier.fillMaxSize()) {
        FullScrim(onClose)
        BottomSheetBox {
            GText(tr("Adicionar jogo", "Add game"), 18, FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(items, key = { it.pkg }) { g ->
                    Row(
                        Modifier.fillMaxWidth().clickableNoRipple { Graph.games.add(g.pkg); onAdded() }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        GameIcon(g.pkg, g.label, 40)
                        GText(g.label, 15, maxLines = 1)
                    }
                }
            }
        }
    }
}
