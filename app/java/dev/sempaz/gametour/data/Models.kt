/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 */
package dev.sempaz.gametour.data

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Perfil aplicado ao abrir o jogo e desfeito ao sair. */
data class GameProfile(
    val thermal: Int = THERMAL_GAME,
    val dnd: Boolean = true,
    val bypass: Boolean = false,
    val fps: Boolean = true,
    /** -1 = não mexer, 0 = desligar, 1 = ligar */
    val spatial: Int = -1,
    /** "" = não mexer; senão o nome do codec (AAC, SBC, LDAC, aptX, aptX-HD) */
    val codec: String = "",
    /** 0 = não mexer; 1 padrão, 2 desempenho, 3 bateria (GameManager) */
    val gameMode: Int = 0,
    val manualBrightness: Boolean = false,
    val cleanRam: Boolean = true,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("thermal", thermal).put("dnd", dnd).put("bypass", bypass).put("fps", fps)
        .put("spatial", spatial).put("codec", codec).put("gameMode", gameMode)
        .put("manualBrightness", manualBrightness).put("cleanRam", cleanRam)

    companion object {
        // Valores gravados em /sys/class/thermal/thermal_message/sconfig (mesmos do Xiaomi Parts)
        const val THERMAL_DEFAULT = 50
        const val THERMAL_PERFORMANCE = 18
        const val THERMAL_GAME = 19
        const val THERMAL_BATTERY = 700

        fun fromJson(o: JSONObject) = GameProfile(
            thermal = o.optInt("thermal", THERMAL_GAME),
            dnd = o.optBoolean("dnd", true),
            bypass = o.optBoolean("bypass", false),
            fps = o.optBoolean("fps", true),
            spatial = o.optInt("spatial", -1),
            codec = o.optString("codec", ""),
            gameMode = o.optInt("gameMode", 0),
            manualBrightness = o.optBoolean("manualBrightness", false),
            cleanRam = o.optBoolean("cleanRam", true),
        )

        fun thermalName(v: Int): String = when (v) {
            THERMAL_DEFAULT -> "Equilibrado"
            THERMAL_PERFORMANCE -> "Desempenho"
            THERMAL_GAME -> "Jogo"
            THERMAL_BATTERY -> "Economia"
            else -> "Perfil $v"
        }
    }
}

/** Uma amostra por segundo durante a sessão. */
class Sample(
    val t: Int,
    val fps: Float,
    val dev: Float,
    val batt: Float,
    val pct: Int,
    val watts: Float,
    val cpu: Float,
    val f0: Int,
    val f1: Int,
    val f2: Int,
    /** uso da GPU em %, ou -1 se não for legível */
    val gpu: Float = -1f,
)

/** Resultado do histograma present2present do SurfaceFlinger. */
data class Frame(val total: Int, val avg: Float, val low1: Float, val low01: Float)

data class Drop(val t: Int, val from: Float, val to: Float, val cause: Cause)

enum class Cause { HEAT, LOW_BATTERY, UNKNOWN }

class SessionData(
    val id: Long,
    val pkg: String,
    val label: String,
    val start: Long,
    val end: Long,
    val profile: String,
    val battStart: Int,
    val battEnd: Int,
    val samples: List<Sample>,
    val frame: Frame?,
) {
    val seconds: Int get() = ((end - start) / 1000L).toInt()

    private val live: List<Sample> by lazy { samples.filter { it.fps > 1f }.drop(3) }

    val fpsMax: Float get() = live.maxOfOrNull { it.fps } ?: 0f
    val fpsMin: Float get() = live.minOfOrNull { it.fps } ?: 0f
    val fpsAvg: Float
        get() = if (frame != null && frame.avg > 1f) frame.avg
        else if (live.isEmpty()) 0f else live.map { it.fps }.average().toFloat()
    val devMax: Float get() = samples.maxOfOrNull { it.dev } ?: 0f
    val gpuAvg: Float
        get() = samples.filter { it.gpu >= 0f }.map { it.gpu }.average().let { if (it.isNaN()) -1f else it.toFloat() }
    val wattsAvg: Float get() = samples.filter { it.watts > 0.1f }.map { it.watts }.average().let { if (it.isNaN()) 0f else it.toFloat() }
    val battUsed: Int get() = max(0, battStart - battEnd)

    /** FPS alvo estimado: percentil 90, arredondado para um valor comum. */
    val targetFps: Int by lazy {
        if (live.isEmpty()) 60 else {
            val sorted = live.map { it.fps }.sorted()
            val p90 = sorted[min(sorted.size - 1, (sorted.size * 0.9f).toInt())]
            listOf(30, 45, 60, 90, 120).minByOrNull { kotlin.math.abs(it - p90) } ?: 60
        }
    }

    /** 0..100: tempo no alvo (60%) + regularidade dos quadros (40%). */
    val stability: Int by lazy {
        if (live.isEmpty()) 0 else {
            val share = live.count { it.fps >= targetFps * 0.9f }.toFloat() / live.size
            val reg = frame?.let { (it.low1 / targetFps).coerceIn(0f, 1f) }
            val v = if (reg == null) share else share * 0.6f + reg * 0.4f
            (v * 100f).roundToInt().coerceIn(0, 100)
        }
    }

    fun secondsBelow(limit: Int): Int = live.count { it.fps < limit }

    /** Quedas de FPS com a causa provável. */
    val drops: List<Drop> by lazy {
        if (live.size < 20) return@lazy emptyList()
        val sorted = live.map { it.fps }.sorted()
        val base = sorted[sorted.size / 2]
        val out = ArrayList<Drop>()
        var i = 0
        while (i < live.size) {
            if (live[i].fps < base * 0.72f) {
                var j = i
                var worst = live[i]
                while (j < live.size && live[j].fps < base * 0.85f) {
                    if (live[j].fps < worst.fps) worst = live[j]
                    j++
                }
                if (j - i >= 2) {
                    val before = samples.lastOrNull { it.t <= worst.t - 60 }?.dev ?: samples.first().dev
                    val cause = when {
                        worst.dev - before >= 2.5f || worst.dev >= 43f -> Cause.HEAT
                        worst.pct in 1..15 -> Cause.LOW_BATTERY
                        else -> Cause.UNKNOWN
                    }
                    out.add(Drop(worst.t, base, worst.fps, cause))
                }
                i = max(j, i + 1)
            } else i++
        }
        out.sortedBy { it.to }.take(3).sortedBy { it.t }
    }

    fun toJson(): JSONObject {
        val arr = JSONArray()
        for (s in samples) {
            arr.put(JSONArray().put(s.t).put(s.fps.toDouble()).put(s.dev.toDouble()).put(s.batt.toDouble())
                .put(s.pct).put(s.watts.toDouble()).put(s.cpu.toDouble()).put(s.f0).put(s.f1).put(s.f2)
                .put(s.gpu.toDouble()))
        }
        val o = JSONObject().put("id", id).put("pkg", pkg).put("label", label).put("start", start)
            .put("end", end).put("profile", profile).put("battStart", battStart).put("battEnd", battEnd)
            .put("s", arr)
        frame?.let {
            o.put("frame", JSONObject().put("total", it.total).put("avg", it.avg.toDouble())
                .put("low1", it.low1.toDouble()).put("low01", it.low01.toDouble()))
        }
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): SessionData {
            val arr = o.getJSONArray("s")
            val list = ArrayList<Sample>(arr.length())
            for (k in 0 until arr.length()) {
                val a = arr.getJSONArray(k)
                list.add(Sample(a.getInt(0), a.getDouble(1).toFloat(), a.getDouble(2).toFloat(),
                    a.getDouble(3).toFloat(), a.getInt(4), a.getDouble(5).toFloat(), a.getDouble(6).toFloat(),
                    a.getInt(7), a.getInt(8), a.getInt(9),
                    if (a.length() > 10) a.getDouble(10).toFloat() else -1f))
            }
            val f = o.optJSONObject("frame")?.let {
                Frame(it.getInt("total"), it.getDouble("avg").toFloat(), it.getDouble("low1").toFloat(),
                    it.getDouble("low01").toFloat())
            }
            return SessionData(o.getLong("id"), o.getString("pkg"), o.optString("label", o.getString("pkg")),
                o.getLong("start"), o.getLong("end"), o.optString("profile", ""), o.optInt("battStart"),
                o.optInt("battEnd"), list, f)
        }
    }
}
