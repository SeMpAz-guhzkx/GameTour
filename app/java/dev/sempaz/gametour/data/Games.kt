/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 *
 * A lista de jogos usa as MESMAS chaves do serviço `game_space` do framework
 * (Settings.System "gamespace_game_list" e "gamespace_denied_list"), por isso a
 * detecção automática do framework continua funcionando.
 */
package dev.sempaz.gametour.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.UserHandle
import android.provider.Settings
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class GameApp(val pkg: String, val label: String, val mode: Int)

class Games(private val ctx: Context) {
    private val pm: PackageManager = ctx.packageManager
    private val icons = LruCache<String, ImageBitmap>(48)

    // Se o app não tiver o uid system (ex.: versão de preview), cai para um armazenamento local.
    private val fallback = ctx.getSharedPreferences("gametour_fallback", Context.MODE_PRIVATE)

    private fun getKey(key: String): String = try {
        Settings.System.getStringForUser(ctx.contentResolver, key, UserHandle.USER_CURRENT) ?: ""
    } catch (e: Throwable) {
        fallback.getString(key, "") ?: ""
    }

    private fun putKey(key: String, v: String) {
        try {
            Settings.System.putStringForUser(ctx.contentResolver, key, v, UserHandle.USER_CURRENT)
        } catch (e: Throwable) {
            fallback.edit().putString(key, v).apply()
        }
    }

    private fun parse(raw: String): LinkedHashMap<String, Int> {
        val m = LinkedHashMap<String, Int>()
        for (e in raw.split(";")) {
            val p = e.split("=")
            if (p.size == 2 && p[0].matches(Regex("[a-zA-Z0-9_.]+")) && p[1].matches(Regex("\\d+"))) {
                m[p[0].trim()] = p[1].trim().toInt()
            }
        }
        return m
    }

    fun list(): List<GameApp> = parse(getKey(KEY_LIST)).mapNotNull { (pkg, mode) ->
        label(pkg)?.let { GameApp(pkg, it, mode) }
    }.sortedBy { it.label.lowercase() }

    fun isGame(pkg: String) = parse(getKey(KEY_LIST)).containsKey(pkg)

    fun add(pkg: String) {
        val m = parse(getKey(KEY_LIST))
        m[pkg] = 2
        putKey(KEY_LIST, m.entries.joinToString(";") { "${it.key}=${it.value}" })
        val denied = getKey(KEY_DENIED).split(";").filter { it.isNotBlank() && it != pkg }
        putKey(KEY_DENIED, denied.joinToString(";"))
    }

    fun remove(pkg: String) {
        val m = parse(getKey(KEY_LIST))
        m.remove(pkg)
        putKey(KEY_LIST, m.entries.joinToString(";") { "${it.key}=${it.value}" })
        val denied = getKey(KEY_DENIED).split(";").filter { it.isNotBlank() }.toMutableSet()
        denied.add(pkg)
        putKey(KEY_DENIED, denied.joinToString(";"))
    }

    /** Apps instalados que ainda não estão na lista. */
    fun candidates(): List<GameApp> {
        val have = parse(getKey(KEY_LIST)).keys
        val launch = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PackageManager.MATCH_ALL
        )
        return launch.mapNotNull { it.activityInfo?.packageName }
            .distinct()
            .filter { it !in have && it != ctx.packageName }
            .mapNotNull { p -> label(p)?.let { GameApp(p, it, 2) } }
            .sortedBy { it.label.lowercase() }
    }

    /** Todos os apps com ícone no lançador (para escolher os apps rápidos). */
    fun launchable(): List<GameApp> {
        val launch = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PackageManager.MATCH_ALL
        )
        return launch.mapNotNull { it.activityInfo?.packageName }
            .distinct()
            .filter { it != ctx.packageName }
            .mapNotNull { p -> label(p)?.let { GameApp(p, it, 0) } }
            .sortedBy { it.label.lowercase() }
    }

    fun isGameCategory(pkg: String): Boolean = try {
        val ai = pm.getApplicationInfo(pkg, 0)
        ai.category == ApplicationInfo.CATEGORY_GAME || (ai.flags and ApplicationInfo.FLAG_IS_GAME) != 0
    } catch (e: Exception) { false }

    fun label(pkg: String): String? = try {
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) { null }

    fun launch(pkg: String): Boolean {
        val i = pm.getLaunchIntentForPackage(pkg) ?: return false
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
        return true
    }

    fun icon(pkg: String): ImageBitmap? {
        icons.get(pkg)?.let { return it }
        return try {
            val d = pm.getApplicationIcon(pkg)
            val size = 160
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            d.setBounds(0, 0, size, size)
            d.draw(c)
            bmp.asImageBitmap().also { icons.put(pkg, it) }
        } catch (e: Exception) { null }
    }

    companion object {
        const val KEY_LIST = "gamespace_game_list"
        const val KEY_DENIED = "gamespace_denied_list"
    }
}
