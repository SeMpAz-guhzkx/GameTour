/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 */
package dev.sempaz.gametour.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

class Prefs(ctx: Context) {
    private val sp: SharedPreferences = ctx.createDeviceProtectedStorageContext()
        .getSharedPreferences("gametour", Context.MODE_PRIVATE)

    var autoDnd: Boolean
        get() = sp.getBoolean("auto_dnd", true)
        set(v) = sp.edit().putBoolean("auto_dnd", v).apply()

    var showFps: Boolean
        get() = sp.getBoolean("show_fps", true)
        set(v) = sp.edit().putBoolean("show_fps", v).apply()

    var pingHost: String
        get() = sp.getString("ping_host", "1.1.1.1") ?: "1.1.1.1"
        set(v) = sp.edit().putString("ping_host", v).apply()

    /** Docagem da pílula: -1 esquerda, 1 direita. */
    var pillSide: Int
        get() = sp.getInt("pill_side", 1)
        set(v) = sp.edit().putInt("pill_side", v).apply()

    var pillY: Int
        get() = sp.getInt("pill_y", 260)
        set(v) = sp.edit().putInt("pill_y", v).apply()

    /** Posição da pílula em pixels (x = -1: ainda não escolhida, usa a borda direita). */
    var pillX: Int
        get() = sp.getInt("pill_x", -1)
        set(v) = sp.edit().putInt("pill_x", v).apply()

    /** Posição do rótulo de medidores em pixels (-1: padrão). */
    var labelX: Int
        get() = sp.getInt("label_x", -1)
        set(v) = sp.edit().putInt("label_x", v).apply()

    var labelY: Int
        get() = sp.getInt("label_y", -1)
        set(v) = sp.edit().putInt("label_y", v).apply()

    /** Tema visual: aurora, amoled, cyber, ocean ou ember. */
    var theme: String
        get() = sp.getString("theme", "aurora") ?: "aurora"
        set(v) = sp.edit().putString("theme", v).apply()

    /** true = alça da borda invisível (só puxar da borda abre o painel). */
    var handleHidden: Boolean
        get() = sp.getBoolean("handle_hidden", false)
        set(v) = sp.edit().putBoolean("handle_hidden", v).apply()

    /** Apps rápidos abertos em janela flutuante (pacotes separados por vírgula). */
    var quickApps: List<String>
        get() = (sp.getString("quick_apps", "") ?: "").split(",").filter { it.isNotBlank() }
        set(v) = sp.edit().putString("quick_apps", v.joinToString(",")).apply()

    /** Chamadas no jogo: 0 normal, 1 atender sozinho, 2 recusar sozinho. */
    var callsMode: Int
        get() = sp.getInt("calls_mode", 0)
        set(v) = sp.edit().putInt("calls_mode", v).apply()

    var callBanner: Boolean
        get() = sp.getBoolean("call_banner", true)
        set(v) = sp.edit().putBoolean("call_banner", v).apply()

    var notifBanner: Boolean
        get() = sp.getBoolean("notif_banner", true)
        set(v) = sp.edit().putBoolean("notif_banner", v).apply()

    /** Medidores mostrados no rótulo em jogo. */
    var metrics: Set<Metric>
        get() = Metric.parse(sp.getString("metrics", Metric.DEFAULT) ?: Metric.DEFAULT)
        set(v) = sp.edit().putString("metrics", Metric.serialize(v)).apply()

    var lastSessionId: Long
        get() = sp.getLong("last_session", 0L)
        set(v) = sp.edit().putLong("last_session", v).apply()

    /** Pacote cujo próximo início de jogo deve pular o perfil (botão "Pular"). */
    var skipOnce: String
        get() = sp.getString("skip_once", "") ?: ""
        set(v) = sp.edit().putString("skip_once", v).apply()

    /** Estado anterior ao perfil, para desfazer mesmo se o app morrer. JSON ou vazio. */
    var restore: String
        get() = sp.getString("restore", "") ?: ""
        set(v) = sp.edit().putString("restore", v).apply()

    var defaultProfile: GameProfile
        get() = sp.getString("profile_default", null)?.let { runCatching { GameProfile.fromJson(JSONObject(it)) }.getOrNull() }
            ?: GameProfile()
        set(v) = sp.edit().putString("profile_default", v.toJson().toString()).apply()

    fun profileFor(pkg: String): GameProfile =
        sp.getString("profile_$pkg", null)?.let { runCatching { GameProfile.fromJson(JSONObject(it)) }.getOrNull() }
            ?: defaultProfile

    fun hasProfile(pkg: String): Boolean = sp.contains("profile_$pkg")

    fun setProfile(pkg: String, p: GameProfile) = sp.edit().putString("profile_$pkg", p.toJson().toString()).apply()

    var tileOrder: List<String>
        get() = (sp.getString("tile_order", "") ?: "").split(",").filter { it.isNotBlank() }
        set(v) = sp.edit().putString("tile_order", v.joinToString(",")).apply()
}
