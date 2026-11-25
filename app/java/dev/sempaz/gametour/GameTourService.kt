/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 * Registra o callback no serviço `game_space` do framework (onGameStart/onGameLeave).
 * Adaptado do GameSpaceService do GameSpace (AxionOS).
 */
package dev.sempaz.gametour

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.os.ServiceManager
import android.util.Log
import com.android.internal.app.IGameSpaceCallback
import com.android.internal.app.IGameSpaceService

class GameTourService : Service() {
    private val tag = "GameTourService"
    private val handler = Handler(Looper.getMainLooper())
    private var svc: IGameSpaceService? = null
    private var tries = 0

    private val callback = object : IGameSpaceCallback.Stub() {
        override fun onGameStart(packageName: String) {
            Graph.controller.onGameStart(packageName)
        }

        override fun onGameLeave() {
            Graph.controller.onGameLeave()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
        Graph.controller.restoreIfStale()
        register()
    }

    private fun register() {
        val s = IGameSpaceService.Stub.asInterface(ServiceManager.getService("game_space"))
        if (s == null) {
            if (++tries <= 12) handler.postDelayed({ register() }, 5000)
            else Log.e(tag, "serviço game_space indisponível")
            return
        }
        try {
            s.registerCallback(callback)
            svc = s
            Log.i(tag, "callback registrado")
        } catch (e: RemoteException) {
            Log.e(tag, "falha ao registrar", e)
            if (++tries <= 12) handler.postDelayed({ register() }, 5000)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        try { svc?.unregisterCallback(callback) } catch (e: RemoteException) { /* ignore */ }
        svc = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
