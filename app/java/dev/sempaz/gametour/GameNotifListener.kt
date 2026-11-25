/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 * Banner de notificações durante o jogo (equivalente ao "danmaku" do GameSpace).
 */
package dev.sempaz.gametour

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class GameNotifListener : NotificationListenerService() {
    private val seen = HashMap<String, Long>()

    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!Graph.controller.isActive || !Graph.prefs.notifBanner) return
        if (!sbn.isClearable || sbn.isOngoing || sbn.packageName == packageName) return
        val n = sbn.notification
        if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        val ex = n.extras
        val title = (ex.getCharSequence(Notification.EXTRA_TITLE) ?: ex.getCharSequence(Notification.EXTRA_TITLE_BIG))?.toString().orEmpty()
        val text = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        // evita repetir a mesma notificação em poucos segundos
        val key = sbn.packageName + "|" + title + "|" + text
        val now = System.currentTimeMillis()
        if (now - (seen[key] ?: 0L) < 5000L) return
        seen[key] = now
        if (seen.size > 60) seen.entries.removeAll { now - it.value > 60000L }

        val label = Graph.games.label(sbn.packageName) ?: sbn.packageName
        Graph.overlay.showBanner(label, title, text, sbn.packageName)
    }
}
