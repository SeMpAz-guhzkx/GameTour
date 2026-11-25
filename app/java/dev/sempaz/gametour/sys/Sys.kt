/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 * Leituras do sistema (só leitura). Os nós seguem o Xiaomi Parts (th4rv4) para o MT6897.
 */
package dev.sempaz.gametour.sys

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.io.FileInputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.abs

data class Batt(val pct: Int, val tempC: Float, val watts: Float, val charging: Boolean)
data class Mem(val freeMb: Long, val totalMb: Long)

class Sys(private val ctx: Context) {
    private val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private var lastTotal = 0L
    private var lastIdle = 0L

    fun batt(): Batt {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val temp = (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val mv = i?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, 0) ?: 0
        val ua = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val watts = abs(ua) / 1_000_000f * (mv / 1000f)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return Batt(if (scale > 0) level * 100 / scale else level, temp, watts, charging)
    }

    /** Temperatura do aparelho em °C (nó da placa, o mesmo do Parts). */
    fun deviceTemp(): Float {
        val raw = readFile(BOARD_TEMP)?.trim()?.toFloatOrNull() ?: return 0f
        return if (raw > 1000f) raw / 1000f else raw
    }

    /** Frequência atual em MHz: pequenos (policy0), grandes (policy4) e prime (policy7). */
    fun freqsMhz(): IntArray = intArrayOf(freq(0), freq(4), freq(7))

    private fun freq(policy: Int): Int {
        val v = readFile("/sys/devices/system/cpu/cpufreq/policy$policy/scaling_cur_freq")?.trim()?.toLongOrNull()
            ?: return 0
        return (v / 1000L).toInt()
    }

    fun maxFreqsMhz(): IntArray = intArrayOf(maxFreq(0), maxFreq(4), maxFreq(7))

    private fun maxFreq(policy: Int): Int {
        val v = readFile("/sys/devices/system/cpu/cpufreq/policy$policy/cpuinfo_max_freq")?.trim()?.toLongOrNull()
            ?: return 0
        return (v / 1000L).toInt()
    }

    /** Uso de CPU em %, ou -1 se o /proc/stat não puder ser lido (SELinux). */
    fun cpuUsage(): Float {
        val line = readFile("/proc/stat")?.lineSequence()?.firstOrNull { it.startsWith("cpu ") } ?: return -1f
        val p = line.trim().split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
        if (p.size < 5) return -1f
        val idle = p[3] + p.getOrElse(4) { 0L }
        val total = p.sum()
        val dT = total - lastTotal
        val dI = idle - lastIdle
        lastTotal = total
        lastIdle = idle
        if (dT <= 0L || dT == total) return -1f
        return (100f * (dT - dI) / dT).coerceIn(0f, 100f)
    }

    private var gpuLogged = false
    private var gpuDiagDone = false

    /** Diagnóstico único: por que o nó de uso da GPU pode não ser lido (vai para o log, tag GameTourSys). */
    private fun gpuDiag() {
        if (gpuDiagDone) return
        gpuDiagDone = true
        val t = "GameTourSys"
        val f = File(GPU_UTIL)
        android.util.Log.i(t, "diag: exists=${f.exists()} isFile=${f.isFile} canRead=${f.canRead()}")
        try {
            val st = android.system.Os.stat(GPU_UTIL)
            android.util.Log.i(t, "diag: stat mode=0${Integer.toOctalString(st.st_mode and 0xFFF)} uid=${st.st_uid} gid=${st.st_gid}")
        } catch (e: Throwable) {
            android.util.Log.i(t, "diag: stat falhou: ${e.javaClass.simpleName}: ${e.message}")
        }
        try {
            android.util.Log.i(t, "diag: pasta=${File("/proc/mtk_mali").list()?.sorted()?.joinToString(",")}")
        } catch (e: Throwable) {
            android.util.Log.i(t, "diag: lista da pasta falhou: ${e.message}")
        }
        try {
            FileInputStream(f).use { ins ->
                val b = ByteArray(256)
                val n = ins.read(b)
                android.util.Log.i(t, "diag: leitura n=$n texto='${if (n > 0) String(b, 0, n).trim() else ""}'")
            }
        } catch (e: Throwable) {
            android.util.Log.i(t, "diag: leitura falhou: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /**
     * Uso da GPU em %, lido de /sys/kernel/ged/hal/gpu_utilization (nó do GED da MediaTek, o mesmo
     * que a libged.so usa). O formato exato é validado no aparelho: o primeiro número lido é usado
     * e o texto bruto é registrado uma vez no log (tag GameTourSys). -1 se não for legível.
     */
    fun gpuUtil(): Float {
        gpuDiag()
        // tenta o nó do /proc e depois os do GED (qual existir e for legível neste kernel)
        for (path in GPU_UTIL_PATHS) {
            val raw = readFile(path)?.trim() ?: continue
            if (!gpuLogged) {
                gpuLogged = true
                android.util.Log.i("GameTourSys", "gpu util lido de $path: $raw")
            }
            // formatos vistos na MediaTek: "37" ou "37 0 0" ou "gpu_loading = 37"
            val n = Regex("-?\\d+(\\.\\d+)?").find(raw)?.value?.toFloatOrNull() ?: continue
            if (n in 0f..100f) return n
        }
        return -1f
    }

    fun gpuMhz(): Int {
        val v = readFile("/sys/class/devfreq/13000000.mali/cur_freq")?.trim()?.toLongOrNull() ?: return -1
        return (v / 1_000_000L).toInt()
    }

    fun mem(): Mem {
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return Mem(mi.availMem / 1048576L, mi.totalMem / 1048576L)
    }

    fun storageFreeGb(): Float {
        val s = StatFs(Environment.getDataDirectory().path)
        return s.availableBytes / 1073741824f
    }

    fun networkType(): String {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "—"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Dados"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Cabo"
            else -> "—"
        }
    }

    /** Latência TCP até o host (ms) ou -1. Chamar fora da thread principal. */
    fun latencyMs(host: String): Int = try {
        val t0 = System.nanoTime()
        Socket().use { it.connect(InetSocketAddress(host, 443), 1500) }
        ((System.nanoTime() - t0) / 1_000_000L).toInt()
    } catch (e: Exception) { -1 }

    companion object {
        const val BOARD_TEMP = "/sys/class/thermal/thermal_message/board_sensor_temp"
        // Nó de uso da GPU do driver Mali da MediaTek (confirmado no aparelho: /proc/mtk_mali/gpu_utilization)
        const val GPU_UTIL = "/proc/mtk_mali/gpu_utilization"
        val GPU_UTIL_PATHS = listOf(
            GPU_UTIL,
            "/sys/kernel/ged/hal/gpu_utilization",
            "/sys/module/ged/parameters/gpu_loading",
        )

        fun readFile(path: String): String? = try {
            FileInputStream(File(path)).bufferedReader().use { it.readText() }
        } catch (e: Throwable) { null }
    }
}
