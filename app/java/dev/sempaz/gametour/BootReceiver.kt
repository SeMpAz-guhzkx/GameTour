/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 */
package dev.sempaz.gametour

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserHandle

/** Concede ao próprio app o acesso de ouvinte de notificações (para o banner no jogo). */
internal fun grantNotificationAccess(ctx: Context) {
    runCatching {
        ctx.getSystemService(android.app.NotificationManager::class.java)
            .setNotificationListenerAccessGranted(ComponentName(ctx, GameNotifListener::class.java), true)
    }
}

class GameTourApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
        grantNotificationAccess(this)
        runCatching { startService(Intent(this, GameTourService::class.java)) }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Graph.init(context)
        runCatching { Graph.controller.restoreIfStale() }
        runCatching { context.startServiceAsUser(Intent(context, GameTourService::class.java), UserHandle.CURRENT) }
    }
}
