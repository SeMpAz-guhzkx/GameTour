/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 * Codec Bluetooth e som espacial (validados em leitura no aparelho em 24/11).
 * Tudo aqui é tolerante a falha: sem fone ou sem a API, o recurso some da tela.
 */
package dev.sempaz.gametour.sys

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothCodecConfig
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioManager
import android.util.Log

data class BtInfo(
    val name: String,
    val codec: String,
    val available: List<String>,
)

class Audio(private val ctx: Context) {
    private val tag = "GameTourAudio"
    private var a2dp: BluetoothA2dp? = null
    private val adapter: BluetoothAdapter? = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val listener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.A2DP) a2dp = proxy as BluetoothA2dp
        }
        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.A2DP) a2dp = null
        }
    }

    fun connect() {
        runCatching { adapter?.getProfileProxy(ctx, listener, BluetoothProfile.A2DP) }
    }

    fun close() {
        runCatching { adapter?.closeProfileProxy(BluetoothProfile.A2DP, a2dp) }
        a2dp = null
    }

    private fun activeDevice(): BluetoothDevice? =
        runCatching { adapter?.getActiveDevices(BluetoothProfile.A2DP)?.firstOrNull() }.getOrNull()
            ?: runCatching { a2dp?.connectedDevices?.firstOrNull() }.getOrNull()

    private fun codecName(type: Int): String = when (type) {
        BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC -> "SBC"
        BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC -> "AAC"
        BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX -> "aptX"
        BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD -> "aptX HD"
        BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC -> "LDAC"
        else -> "Codec $type"
    }

    private fun codecType(name: String): Int = when (name.lowercase().replace(" ", "").replace("-", "")) {
        "sbc" -> BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC
        "aac" -> BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC
        "aptx" -> BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX
        "aptxhd" -> BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD
        "ldac" -> BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC
        else -> -1
    }

    /** Fone A2DP ativo com o codec atual e os codecs que o fone aceita, ou null. */
    fun info(): BtInfo? = try {
        val dev = activeDevice()
        val st = if (dev != null) a2dp?.getCodecStatus(dev) else null
        if (dev == null || st == null) null else BtInfo(
            name = runCatching { dev.alias }.getOrNull() ?: dev.name ?: "Fone",
            codec = st.codecConfig?.let { codecName(it.codecType) } ?: "—",
            available = st.codecsSelectableCapabilities.map { codecName(it.codecType) }.distinct(),
        )
    } catch (e: Throwable) {
        Log.w(tag, "info failed", e)
        null
    }

    /** Troca o codec preferido do fone ativo. VERIFY: só vale se o fone aceitar. */
    fun setCodec(name: String): Boolean = try {
        val dev = activeDevice()
        val type = codecType(name)
        val st = if (dev != null) a2dp?.getCodecStatus(dev) else null
        val cap = st?.codecsSelectableCapabilities?.firstOrNull { it.codecType == type }
        if (dev == null || cap == null) false else {
            val cfg = BluetoothCodecConfig.Builder()
                .setCodecType(type)
                .setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST)
                .setSampleRate(highestBit(cap.sampleRate))
                .setBitsPerSample(highestBit(cap.bitsPerSample))
                .setChannelMode(highestBit(cap.channelMode))
                .build()
            a2dp?.setCodecConfigPreference(dev, cfg)
            true
        }
    } catch (e: Throwable) {
        Log.w(tag, "setCodec failed", e)
        false
    }

    private fun highestBit(mask: Int): Int = if (mask == 0) 0 else Integer.highestOneBit(mask)

    // ---- Som espacial ----
    private val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun spatialAvailable(): Boolean = runCatching { am.spatializer.isAvailable }.getOrDefault(false)

    fun spatialEnabled(): Boolean = runCatching { am.spatializer.isEnabled }.getOrDefault(false)

    fun setSpatial(on: Boolean): Boolean = try {
        am.spatializer.setEnabled(on)
        true
    } catch (e: Throwable) {
        Log.w(tag, "setSpatial failed", e)
        false
    }
}
