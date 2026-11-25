/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 * Medidores que o usuário pode escolher para mostrar no rótulo em jogo.
 */
package dev.sempaz.gametour.data

import dev.sempaz.gametour.tr

enum class Metric(val id: String) {
    FPS("fps"),
    FRAME("ms"),
    CPU("cpu"),
    CPU_FREQ("cpuf"),
    GPU("gpu"),
    GPU_FREQ("gpuf"),
    DEV_TEMP("dev"),
    BATT_TEMP("bt"),
    BATT_PCT("bp"),
    WATTS("w"),
    RAM("ram");

    fun label(): String = when (this) {
        FPS -> "FPS"
        FRAME -> tr("Tempo de quadro", "Frame time")
        CPU -> tr("Uso da CPU", "CPU usage")
        CPU_FREQ -> tr("Frequência da CPU (prime)", "CPU frequency (prime)")
        GPU -> tr("Uso da GPU", "GPU usage")
        GPU_FREQ -> tr("Frequência da GPU", "GPU frequency")
        DEV_TEMP -> tr("Temperatura do aparelho", "Device temperature")
        BATT_TEMP -> tr("Temperatura da bateria", "Battery temperature")
        BATT_PCT -> tr("Bateria", "Battery")
        WATTS -> tr("Consumo (W)", "Power (W)")
        RAM -> tr("RAM livre", "Free RAM")
    }

    companion object {
        const val DEFAULT = "fps,ms,dev"

        fun parse(raw: String): Set<Metric> =
            raw.split(",").mapNotNull { id -> values().firstOrNull { it.id == id.trim() } }.toSet()

        fun serialize(set: Set<Metric>): String = values().filter { it in set }.joinToString(",") { it.id }
    }
}
