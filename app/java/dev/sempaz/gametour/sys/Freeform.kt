/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 * Abre apps em janela flutuante usando o LMOFreeform que já vem na ROM
 * (mesmo mecanismo do GameSpace: broadcast com.libremobileos.freeform.START_FREEFORM).
 */
package dev.sempaz.gametour.sys

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.util.Log

object Freeform {
    private const val PKG = "com.libremobileos.freeform"
    private const val ACTION = "com.libremobileos.freeform.START_FREEFORM"

    fun available(ctx: Context): Boolean =
        runCatching { ctx.packageManager.getPackageInfo(PKG, 0); true }.getOrDefault(false)

    /** Pede ao LMOFreeform para abrir o app numa janela flutuante. */
    fun launch(ctx: Context, pkg: String): Boolean = try {
        val launch = ctx.packageManager.getLaunchIntentForPackage(pkg)
        val activity = launch?.component?.className
        if (activity == null) false else {
            ctx.sendBroadcast(Intent(ACTION).apply {
                setPackage(PKG)
                putExtra("packageName", pkg)
                putExtra("activityName", activity)
                putExtra("userId", UserHandle.myUserId())
            })
            true
        }
    } catch (e: Throwable) {
        Log.w("GameTourFreeform", "launch failed for $pkg", e)
        false
    }
}
