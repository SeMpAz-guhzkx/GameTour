/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 */
package dev.sempaz.gametour

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.sempaz.gametour.data.SessionData
import dev.sempaz.gametour.ui.AddGameSheet
import dev.sempaz.gametour.ui.GIcon
import dev.sempaz.gametour.ui.GText
import dev.sempaz.gametour.ui.GT
import dev.sempaz.gametour.ui.GameTourTheme
import dev.sempaz.gametour.ui.GamesScreen
import dev.sempaz.gametour.ui.GlassBackground
import dev.sempaz.gametour.ui.HistoryScreen
import dev.sempaz.gametour.ui.HomeScreen
import dev.sempaz.gametour.ui.LocalPal
import dev.sempaz.gametour.ui.PreGameSheet
import dev.sempaz.gametour.ui.ReportScreen
import dev.sempaz.gametour.ui.SettingsScreen
import dev.sempaz.gametour.ui.clickableNoRipple
import dev.sempaz.gametour.ui.glass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val openSession = mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Graph.init(this)
        window.setDecorFitsSystemWindows(false)
        openSession.longValue = intent.getLongExtra(EXTRA_SESSION, 0L)
        dev.sempaz.gametour.ui.ThemeHolder.id.value = Graph.prefs.theme
        setContent { GameTourTheme { AppRoot(openSession.longValue) { openSession.longValue = 0L } } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val id = intent.getLongExtra(EXTRA_SESSION, 0L)
        if (id != 0L) openSession.longValue = id
    }

    companion object { const val EXTRA_SESSION = "session_id" }
}

@Composable
private fun AppRoot(sessionId: Long, onSessionHandled: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var pregame by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<SessionData?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(sessionId) {
        if (sessionId != 0L) {
            report = withContext(Dispatchers.IO) { Graph.store.find(sessionId) }
            onSessionHandled()
        }
    }

    BackHandler(enabled = pregame != null || adding || report != null) {
        when {
            adding -> adding = false
            pregame != null -> pregame = null
            report != null -> report = null
        }
    }

    GlassBackground {
        Box(Modifier.fillMaxSize()) {
            when (tab) {
                0 -> HomeScreen(onGame = { pregame = it }, onAdd = { adding = true })
                1 -> GamesScreen(onGame = { pregame = it }, onAdd = { adding = true }, refreshKey = refresh)
                2 -> HistoryScreen(onOpen = { report = it }, refreshKey = refresh)
                else -> SettingsScreen()
            }
            TabBar(tab, { tab = it }, Modifier.align(Alignment.BottomCenter))

            pregame?.let { PreGameSheet(it) { pregame = null; refresh++ } }
            if (adding) AddGameSheet(onClose = { adding = false }, onAdded = { adding = false; refresh++ })
            report?.let { s ->
                GlassBackground {
                    ReportScreen(s, onClose = { report = null }, onDelete = {
                        Graph.store.delete(s.pkg, s.id); report = null; refresh++
                    })
                }
            }
        }
    }
}

@Composable
private fun TabBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val items: List<Pair<ImageVector, String>> = listOf(
        Icons.Rounded.Home to tr("Início", "Home"),
        Icons.Rounded.SportsEsports to tr("Jogos", "Games"),
        Icons.Rounded.History to tr("Histórico", "History"),
        Icons.Rounded.Settings to tr("Ajustes", "Settings"),
    )
    val p = LocalPal.current
    Row(
        modifier
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clip(CircleShape)
            .background(p.sheet.copy(alpha = 0.94f))   // opaca o bastante para não misturar com o conteúdo atrás
            .glass(CircleShape, strong = true)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        items.forEachIndexed { i, (icon, label) ->
            val on = i == selected
            Column(
                Modifier
                    .then(if (on) Modifier.glass(CircleShape, strong = true) else Modifier)
                    .clickableNoRipple { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GIcon(icon, 22.dp, if (on) p.accent else p.text2)
                GText(label, 10, if (on) FontWeight.SemiBold else FontWeight.Normal, if (on) p.accent else p.text2, maxLines = 1)
            }
        }
    }
}
