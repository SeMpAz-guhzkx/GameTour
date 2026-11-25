/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 *
 * Controla a sessão de jogo: aplica o perfil ao abrir, amostra 1x por segundo,
 * calcula o relatório e DESFAZ tudo ao sair (ou ao reiniciar, se o app morrer).
 */
package dev.sempaz.gametour.session

import android.app.ActivityTaskManager
import android.app.GameManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.window.TaskFpsCallback
import dev.sempaz.gametour.Graph
import dev.sempaz.gametour.MainActivity
import dev.sempaz.gametour.R
import dev.sempaz.gametour.data.GameProfile
import dev.sempaz.gametour.data.Sample
import dev.sempaz.gametour.data.SessionData
import dev.sempaz.gametour.sys.Bypass
import dev.sempaz.gametour.sys.FrameStats
import dev.sempaz.gametour.sys.Thermal
import dev.sempaz.gametour.tr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.Executor

/** Estado ao vivo que o overlay e o app mostram. */
data class Live(
    val active: Boolean = false,
    val pkg: String = "",
    val fps: Float = 0f,
    val dev: Float = 0f,
    val cpu: Float = -1f,
    val secs: Int = 0,
    val profile: GameProfile? = null,
    val gpu: Float = -1f,
    val gpuMhz: Int = -1,
    val battTemp: Float = 0f,
    val battPct: Int = 0,
    val watts: Float = 0f,
    val ramFreeMb: Long = 0L,
    val cpuMhz: Int = 0,
)

class SessionController(private val ctx: Context) {
    private val tag = "GameTourSession"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val _live = MutableStateFlow(Live())
    val live: StateFlow<Live> = _live

    @Volatile private var curFps = 0f
    private var fpsTask = -1
    private var fpsRegistered = false
    private var job: Job? = null

    private class Active(
        val pkg: String,
        val label: String,
        val start: Long,
        val profile: GameProfile?,
        val battStart: Int,
    ) { val samples = ArrayList<Sample>() }

    @Volatile private var active: Active? = null

    private val fpsCb = object : TaskFpsCallback() {
        override fun onFpsReported(fps: Float) { curFps = fps }
    }

    val isActive: Boolean get() = active != null

    // ------------------------------------------------------------------ início
    @Synchronized
    fun onGameStart(pkg: String) {
        if (active?.pkg == pkg) return
        if (active != null) onGameLeave()
        Log.i(tag, "game start: $pkg")

        val skip = Graph.prefs.skipOnce == pkg
        if (skip) Graph.prefs.skipOnce = ""
        val profile = if (skip) null else Graph.prefs.profileFor(pkg)
        val label = Graph.games.label(pkg) ?: pkg
        val a = Active(pkg, label, System.currentTimeMillis(), profile, Graph.sys.batt().pct)
        active = a
        curFps = 0f

        if (profile != null) apply(pkg, profile)
        Graph.calls.start()
        FrameStats.begin()

        val showFps = profile?.fps ?: false
        _live.value = Live(true, pkg, 0f, 0f, -1f, 0, profile)
        Graph.overlay.show(pkg, showFps)

        job = scope.launch {
            registerFpsWithRetry()
            var t = 0
            while (isActive) {
                delay(1000)
                t++
                val b = Graph.sys.batt()
                val dev = Graph.sys.deviceTemp()
                val f = Graph.sys.freqsMhz()
                val cpu = Graph.sys.cpuUsage()
                val gpu = Graph.sys.gpuUtil()
                val s = Sample(t, curFps, dev, b.tempC, b.pct, b.watts, cpu, f[0], f[1], f[2], gpu)
                synchronized(a.samples) { a.samples.add(s) }
                _live.value = Live(true, pkg, curFps, dev, cpu, t, profile,
                    gpu = gpu, gpuMhz = Graph.sys.gpuMhz(), battTemp = b.tempC, battPct = b.pct,
                    watts = b.watts, ramFreeMb = Graph.sys.mem().freeMb, cpuMhz = f[2])
                if (t % 20 == 0) refreshFpsTask()
            }
        }
    }

    private suspend fun registerFpsWithRetry() {
        for (wait in longArrayOf(0, 1000, 2000, 4000, 8000)) {
            delay(wait)
            if (registerFps()) return
        }
    }

    private fun registerFps(): Boolean = try {
        val id = ActivityTaskManager.getService().focusedRootTaskInfo?.taskId ?: -1
        if (id < 0) false else {
            unregisterFps()
            wm.registerTaskFpsCallback(id, Executor { it.run() }, fpsCb)
            fpsTask = id
            fpsRegistered = true
            true
        }
    } catch (e: Throwable) {
        Log.w(tag, "registerFps failed", e)
        false
    }

    private fun refreshFpsTask() {
        val id = runCatching { ActivityTaskManager.getService().focusedRootTaskInfo?.taskId ?: -1 }.getOrDefault(-1)
        if (id >= 0 && id != fpsTask) registerFps()
    }

    private fun unregisterFps() {
        if (fpsRegistered) runCatching { wm.unregisterTaskFpsCallback(fpsCb) }
        fpsRegistered = false
        fpsTask = -1
    }

    // --------------------------------------------------------------------- fim
    @Synchronized
    fun onGameLeave() {
        val a = active ?: return
        active = null
        Log.i(tag, "game leave: ${a.pkg}")
        job?.cancel()
        job = null
        unregisterFps()
        Graph.overlay.hide()
        Graph.calls.stop()
        revert()
        _live.value = Live()

        val end = System.currentTimeMillis()
        val samples = synchronized(a.samples) { ArrayList(a.samples) }
        val battEnd = Graph.sys.batt().pct
        scope.launch {
            val frame = FrameStats.finish(a.pkg)
            if (samples.size < MIN_SECONDS) return@launch
            val profileName = a.profile?.let { GameProfile.thermalName(it.thermal) } ?: tr("Nenhum", "None")
            val s = SessionData(a.start, a.pkg, a.label, a.start, end, profileName, a.battStart, battEnd, samples, frame)
            Graph.store.save(s)
            Graph.prefs.lastSessionId = s.id
            notifyEnded(s)
        }
    }

    // ---------------------------------------------------------------- perfil
    private fun apply(pkg: String, p: GameProfile) {
        val snap = JSONObject().put("pkg", pkg)
        runCatching {
            val prev = Thermal.read()
            snap.put("thermal", prev)
            if (prev != p.thermal) Thermal.write(p.thermal)
        }
        if (p.dnd && Graph.prefs.autoDnd) runCatching {
            val prev = Graph.tweaks.interruptionFilter()
            snap.put("dnd", prev)
            if (prev == NotificationManager.INTERRUPTION_FILTER_ALL ||
                prev == NotificationManager.INTERRUPTION_FILTER_UNKNOWN) Graph.tweaks.setDnd(true)
        }
        // Banners do sistema desligados durante o jogo: as notificações aparecem no banner do Game Tour
        if (Graph.prefs.notifBanner) runCatching {
            val cr = ctx.contentResolver
            snap.put("headsup", Settings.Global.getInt(cr, Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED, 1))
            Settings.Global.putInt(cr, Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED, 0)
        }
        if (p.bypass && Bypass.supported()) runCatching {
            val was = Bypass.enabled()
            snap.put("bypass", was).put("charge", Bypass.chargeCurrent())
            if (!was) Bypass.set(true)
        }
        if (p.manualBrightness) runCatching {
            snap.put("autoBr", Graph.tweaks.autoBrightness())
            Graph.tweaks.setAutoBrightness(false)
        }
        if (p.gameMode != 0) runCatching {
            snap.put("gameMode", Graph.tweaks.gameMode(pkg))
            Graph.tweaks.setGameMode(pkg, p.gameMode)
        }
        if (p.spatial >= 0) runCatching {
            snap.put("spatial", Graph.audio.spatialEnabled())
            Graph.audio.setSpatial(p.spatial == 1)
        }
        if (p.codec.isNotEmpty()) runCatching {
            Graph.audio.info()?.let { snap.put("codec", it.codec) }
            Graph.audio.setCodec(p.codec)
        }
        Graph.prefs.restore = snap.toString()
        if (p.cleanRam) scope.launch { runCatching { Graph.tweaks.cleanRam(pkg) } }
    }

    /** Desfaz o que o perfil mudou, a partir do estado salvo (vale também após reinício). */
    fun revert() {
        val raw = Graph.prefs.restore
        if (raw.isEmpty()) return
        val s = runCatching { JSONObject(raw) }.getOrNull() ?: run { Graph.prefs.restore = ""; return }
        val pkg = s.optString("pkg")
        if (s.has("thermal")) runCatching { val v = s.getInt("thermal"); if (v >= 0) Thermal.write(v) }
        if (s.has("dnd")) runCatching { Graph.tweaks.setFilter(s.getInt("dnd")) }
        if (s.has("headsup")) runCatching {
            Settings.Global.putInt(ctx.contentResolver, Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED, s.getInt("headsup"))
        }
        if (s.has("bypass")) runCatching {
            if (!s.getBoolean("bypass")) Bypass.set(false, s.optString("charge", Bypass.DEFAULT_CURRENT))
        }
        if (s.has("autoBr")) runCatching { Graph.tweaks.setAutoBrightness(s.getBoolean("autoBr")) }
        if (s.has("gameMode") && pkg.isNotEmpty()) runCatching {
            Graph.tweaks.setGameMode(pkg, s.getInt("gameMode"))
        }
        if (s.has("spatial")) runCatching { Graph.audio.setSpatial(s.getBoolean("spatial")) }
        if (s.has("codec")) runCatching { Graph.audio.setCodec(s.getString("codec")) }
        Graph.prefs.restore = ""
    }

    /** Chamado no boot: se o app morreu com um perfil aplicado, desfaz agora. */
    fun restoreIfStale() {
        if (active == null && Graph.prefs.restore.isNotEmpty()) {
            Log.w(tag, "estado salvo de uma sessão anterior: desfazendo")
            revert()
        }
    }

    // ------------------------------------------------------------ notificação
    private fun notifyEnded(s: SessionData) {
        runCatching {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CHANNEL,
                ctx.getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW))
            val i = Intent(ctx, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_SESSION, s.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pi = PendingIntent.getActivity(ctx, 1, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val text = tr("${s.label}: média ${s.fpsAvg.toInt()} FPS, estabilidade ${s.stability}",
                "${s.label}: avg ${s.fpsAvg.toInt()} FPS, stability ${s.stability}")
            nm.notify(NOTIF_ID, Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle(ctx.getString(R.string.notif_title))
                .setContentText(text)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build())
        }
    }

    companion object {
        const val MIN_SECONDS = 20
        private const val CHANNEL = "gametour_sessions"
        private const val NOTIF_ID = 4101
    }
}
