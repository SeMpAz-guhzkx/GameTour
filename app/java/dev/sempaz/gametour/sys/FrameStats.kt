/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 *
 * Tempo de quadro pelo TimeStats do SurfaceFlinger (validado no aparelho em 24/11):
 *   dumpsys SurfaceFlinger --timestats -clear / -enable / -dump -maxlayers 12 / -disable
 * `--latency` NAO serve no Android 16. É um estado global: ligar só durante a sessão.
 */
package dev.sempaz.gametour.sys

import android.os.ParcelFileDescriptor
import android.os.ServiceManager
import android.util.Log
import dev.sempaz.gametour.data.Frame

object FrameStats {
    private const val TAG = "GameTourFrames"

    fun begin() {
        sf("--timestats", "-clear")
        sf("--timestats", "-enable")
    }

    /** Devolve o resumo do jogo e desliga o TimeStats. */
    fun finish(pkg: String): Frame? {
        val text = sf("--timestats", "-dump", "-maxlayers", "12")
        sf("--timestats", "-disable")
        return parse(text, pkg)
    }

    private fun sf(vararg args: String): String {
        val binder = ServiceManager.getService("SurfaceFlinger") ?: return ""
        val pipe = ParcelFileDescriptor.createPipe()
        val reader = pipe[0]
        val writer = pipe[1]
        val out = StringBuilder()
        val t = Thread {
            runCatching {
                ParcelFileDescriptor.AutoCloseInputStream(reader).bufferedReader().use { out.append(it.readText()) }
            }
        }
        t.start()
        try {
            binder.dump(writer.fileDescriptor, arrayOf(*args))
        } catch (e: Throwable) {
            Log.w(TAG, "dump failed", e)
        } finally {
            runCatching { writer.close() }
        }
        t.join(4000)
        return out.toString()
    }

    /** Escolhe a camada do jogo (SurfaceView do pacote com mais quadros) e lê o histograma. */
    internal fun parse(text: String, pkg: String): Frame? {
        if (text.isBlank()) return null
        val blocks = text.split("layerName = ").drop(1)
        var best: String? = null
        var bestFrames = 0
        for (b in blocks) {
            val header = b.lineSequence().firstOrNull() ?: continue
            if (!header.contains(pkg)) continue
            val frames = Regex("totalFrames = (\\d+)").find(b)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            if (frames > bestFrames) { bestFrames = frames; best = b }
        }
        val b = best ?: return null
        val avg = Regex("averageFPS = ([0-9.]+)").find(b)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        val lines = b.lines()
        val idx = lines.indexOfFirst { it.startsWith("present2present histogram") }
        if (idx < 0 || idx + 1 >= lines.size) return Frame(bestFrames, avg, 0f, 0f)
        val bins = Regex("(\\d+)ms=(\\d+)").findAll(lines[idx + 1])
            .map { it.groupValues[1].toInt() to it.groupValues[2].toLong() }
            .filter { it.second > 0 && it.first > 0 }
            .toList().sortedBy { it.first }
        val total = bins.sumOf { it.second }
        if (total <= 0) return Frame(bestFrames, avg, 0f, 0f)
        return Frame(bestFrames, avg, lowFps(bins, total, 0.99), lowFps(bins, total, 0.999))
    }

    /** FPS correspondente ao percentil `p` do tempo entre quadros (p = 0,99 dá o "1% low"). */
    private fun lowFps(bins: List<Pair<Int, Long>>, total: Long, p: Double): Float {
        val target = total * p
        var acc = 0L
        for ((ms, n) in bins) {
            acc += n
            // a faixa "Nms" cobre [N, N+1) ms: usar o meio da faixa evita superestimar o FPS
            if (acc >= target) return 1000f / (ms + 0.5f)
        }
        return 1000f / (bins.last().first + 0.5f)
    }
}
