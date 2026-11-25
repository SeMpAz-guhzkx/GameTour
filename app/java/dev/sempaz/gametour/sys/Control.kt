/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 * Controles que mudam o sistema. Mecânica do perfil térmico e do bypass copiada do
 * Xiaomi Parts (th4rv4, Apache-2.0): sconfig e /proc/mtk_battery_cmd/current_cmd.
 */
package dev.sempaz.gametour.sys

import android.app.ActivityManager
import android.app.GameManager
import android.app.NotificationManager
import android.content.Context
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import java.io.File
import java.io.FileWriter

object Thermal {
    const val PATH = "/sys/class/thermal/thermal_message/sconfig"

    fun read(): Int = Sys.readFile(PATH)?.trim()?.toIntOrNull() ?: -1

    fun write(value: Int): Boolean = try {
        FileWriter(PATH).use { it.write(value.toString()) }
        true
    } catch (e: Throwable) {
        Log.w("GameTour", "sconfig write failed", e)
        false
    }
}

object Bypass {
    private const val NODE = "/proc/mtk_battery_cmd/current_cmd"
    private const val CHARGE_CURRENT = "/sys/class/power_supply/battery/constant_charge_current"
    const val DEFAULT_CURRENT = "21000000"

    fun supported() = File(NODE).exists()

    fun enabled(): Boolean {
        val parts = Sys.readFile(NODE)?.lineSequence()?.firstOrNull()?.trim()?.split(Regex("\\s+")) ?: return false
        return parts.size > 1 && parts[1] == "1"
    }

    fun chargeCurrent(): String = Sys.readFile(CHARGE_CURRENT)?.trim() ?: DEFAULT_CURRENT

    /** Liga/desliga o bypass; ao desligar, restaura a corrente de carga informada. */
    fun set(enabled: Boolean, restoreCurrent: String = DEFAULT_CURRENT): Boolean = try {
        FileWriter(NODE).use { it.write(if (enabled) "0 1" else "0 0") }
        FileWriter(CHARGE_CURRENT).use { it.write(if (enabled) "0" else restoreCurrent) }
        true
    } catch (e: Throwable) {
        Log.w("GameTour", "bypass write failed", e)
        false
    }
}

/** Não perturbe, brilho, modo do GameManager e limpeza de memória. */
class Tweaks(private val ctx: Context) {
    private val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun interruptionFilter(): Int = nm.currentInterruptionFilter

    fun setDnd(on: Boolean) = runCatching {
        nm.setInterruptionFilter(if (on) NotificationManager.INTERRUPTION_FILTER_PRIORITY
        else NotificationManager.INTERRUPTION_FILTER_ALL)
    }

    fun setFilter(filter: Int) = runCatching { nm.setInterruptionFilter(filter) }

    // Leituras/escritas à prova de falha: sem permissão (ex.: versão de preview) não derruba o app.
    private fun getInt(name: String, def: Int): Int = try {
        Settings.System.getIntForUser(ctx.contentResolver, name, def, UserHandle.USER_CURRENT)
    } catch (e: Throwable) {
        runCatching { Settings.System.getInt(ctx.contentResolver, name, def) }.getOrDefault(def)
    }

    private fun putInt(name: String, v: Int) {
        try {
            Settings.System.putIntForUser(ctx.contentResolver, name, v, UserHandle.USER_CURRENT)
        } catch (e: Throwable) {
            runCatching { Settings.System.putInt(ctx.contentResolver, name, v) }
        }
    }

    fun autoBrightness(): Boolean = getInt(Settings.System.SCREEN_BRIGHTNESS_MODE,
        Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC

    fun setAutoBrightness(auto: Boolean) = putInt(Settings.System.SCREEN_BRIGHTNESS_MODE,
        if (auto) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)

    fun brightness(): Int = getInt(Settings.System.SCREEN_BRIGHTNESS, 128)

    fun setBrightness(v: Int) = putInt(Settings.System.SCREEN_BRIGHTNESS, v.coerceIn(1, 255))

    fun gameMode(pkg: String): Int = runCatching {
        (ctx.getSystemService(Context.GAME_SERVICE) as GameManager).getGameMode(pkg)
    }.getOrDefault(GameManager.GAME_MODE_STANDARD)

    fun setGameMode(pkg: String, mode: Int) = runCatching {
        (ctx.getSystemService(Context.GAME_SERVICE) as GameManager).setGameMode(pkg, mode)
    }

    /** Fecha processos em segundo plano (menos o jogo). Devolve quantos foram pedidos. */
    fun cleanRam(keepPkg: String): Int {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        var n = 0
        am.runningAppProcesses?.forEach { p ->
            if (p.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                p.pkgList?.contains(keepPkg) != true && p.pkgList?.contains(ctx.packageName) != true) {
                p.pkgList?.forEach { am.killBackgroundProcesses(it) }
                n++
            }
        }
        return n
    }
}
