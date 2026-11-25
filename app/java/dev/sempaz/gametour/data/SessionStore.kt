/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 */
package dev.sempaz.gametour.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.Locale

/** Sessões guardadas como JSON em files/sessions/<pacote>/<id>.json. */
class SessionStore(ctx: Context) {
    private val root = File(ctx.filesDir, "sessions").apply { mkdirs() }

    fun save(s: SessionData) {
        val dir = File(root, s.pkg).apply { mkdirs() }
        File(dir, "${s.id}.json").writeText(s.toJson().toString())
        trim(dir)
    }

    private fun trim(dir: File) {
        val files = dir.listFiles()?.sortedByDescending { it.name } ?: return
        files.drop(MAX_PER_GAME).forEach { it.delete() }
    }

    fun load(pkg: String, id: Long): SessionData? = runCatching {
        SessionData.fromJson(JSONObject(File(File(root, pkg), "$id.json").readText()))
    }.getOrNull()

    fun find(id: Long): SessionData? {
        root.listFiles()?.forEach { d ->
            val f = File(d, "$id.json")
            if (f.exists()) return runCatching { SessionData.fromJson(JSONObject(f.readText())) }.getOrNull()
        }
        return null
    }

    fun all(): List<SessionData> {
        val out = ArrayList<SessionData>()
        root.listFiles()?.forEach { d ->
            d.listFiles()?.forEach { f ->
                runCatching { out.add(SessionData.fromJson(JSONObject(f.readText()))) }
            }
        }
        return out.sortedByDescending { it.start }
    }

    fun delete(pkg: String, id: Long) {
        File(File(root, pkg), "$id.json").delete()
    }

    fun csv(s: SessionData): String {
        val sb = StringBuilder("segundo,fps,temp_aparelho,temp_bateria,bateria_pct,watts,cpu_pct,mhz_pequenos,mhz_grandes,mhz_prime\n")
        for (x in s.samples) {
            sb.append(String.format(Locale.US, "%d,%.1f,%.1f,%.1f,%d,%.2f,%.1f,%d,%d,%d\n",
                x.t, x.fps, x.dev, x.batt, x.pct, x.watts, x.cpu, x.f0, x.f1, x.f2))
        }
        return sb.toString()
    }

    companion object { const val MAX_PER_GAME = 40 }
}
