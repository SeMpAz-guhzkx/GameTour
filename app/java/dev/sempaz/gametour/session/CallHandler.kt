/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 * Chamadas durante o jogo. Lógica adaptada do CallListener do GameSpace (chaldeaprjkt / AxionOS).
 * Modos: 0 = normal (só mostra o aviso), 1 = atender sozinho (no viva-voz, sem fone com fio), 2 = recusar sozinho.
 */
package dev.sempaz.gametour.session

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioSystem
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import dev.sempaz.gametour.Graph
import dev.sempaz.gametour.tr

class CallHandler(private val ctx: Context) {
    private val tag = "GameTourCalls"
    private val handler = Handler(Looper.getMainLooper())
    private val tm = ctx.getSystemService(TelephonyManager::class.java)
    private val telecom = ctx.getSystemService(TelecomManager::class.java)
    private val audio = ctx.getSystemService(AudioManager::class.java)

    private var prevMode = AudioManager.MODE_NORMAL
    private var listener: PhoneStateListener? = null

    @Suppress("DEPRECATION")
    fun start() {
        handler.post {
            if (listener != null || tm == null) return@post
            prevMode = audio.mode
            val l = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, incomingNumber: String?) {
                    when (state) {
                        TelephonyManager.CALL_STATE_RINGING -> incoming(incomingNumber.orEmpty())
                        TelephonyManager.CALL_STATE_OFFHOOK -> offhook()
                        TelephonyManager.CALL_STATE_IDLE -> idle()
                    }
                }
            }
            listener = l
            runCatching { tm.listen(l, PhoneStateListener.LISTEN_CALL_STATE) }
                .onFailure { Log.w(tag, "listen failed", it) }
        }
    }

    @Suppress("DEPRECATION")
    fun stop() {
        handler.post {
            listener?.let { l -> runCatching { tm?.listen(l, PhoneStateListener.LISTEN_NONE) } }
            listener = null
            Graph.overlay.hideCall()
        }
    }

    private fun toast(msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    private fun incoming(number: String) {
        val name = lookupName(number).ifEmpty { number }.ifEmpty { tr("Número desconhecido", "Unknown number") }
        when (Graph.prefs.callsMode) {
            1 -> {
                runCatching { telecom?.acceptRingingCall() }
                toast(tr("Chamada atendida: $name", "Call answered: $name"))
                return
            }
            2 -> {
                runCatching { telecom?.endCall() }
                toast(tr("Chamada recusada: $name", "Call declined: $name"))
                return
            }
        }
        if (Graph.prefs.callBanner) {
            Graph.overlay.showCall(
                name,
                onAccept = { runCatching { telecom?.acceptRingingCall() }; offhook() },
                onReject = { runCatching { telecom?.endCall() } },
            )
        }
    }

    private fun offhook() {
        Graph.overlay.hideCall()
        if (Graph.prefs.callsMode != 1) return
        runCatching {
            audio.mode = AudioManager.MODE_IN_COMMUNICATION
            if (wiredHeadset()) {
                audio.isSpeakerphoneOn = false
                AudioSystem.setForceUse(AudioSystem.FOR_COMMUNICATION, AudioSystem.FORCE_NONE)
            } else {
                audio.isSpeakerphoneOn = true
                AudioSystem.setForceUse(AudioSystem.FOR_COMMUNICATION, AudioSystem.FORCE_SPEAKER)
            }
        }
    }

    private fun idle() {
        Graph.overlay.hideCall()
        if (Graph.prefs.callsMode != 1) return
        runCatching {
            audio.mode = prevMode
            AudioSystem.setForceUse(AudioSystem.FOR_COMMUNICATION, AudioSystem.FORCE_NONE)
        }
    }

    private fun wiredHeadset(): Boolean = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
        it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_USB_HEADSET
    }

    private fun lookupName(number: String): String {
        if (number.isBlank()) return ""
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        return try {
            ctx.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0).orEmpty() else "" } ?: ""
        } catch (e: Throwable) { "" }
    }
}
