/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 * Instâncias únicas do app (sem Hilt, para manter o build do Soong simples).
 */
package dev.sempaz.gametour

import android.content.Context
import dev.sempaz.gametour.data.Games
import dev.sempaz.gametour.data.Prefs
import dev.sempaz.gametour.data.SessionStore
import dev.sempaz.gametour.overlay.GameOverlay
import dev.sempaz.gametour.session.CallHandler
import dev.sempaz.gametour.session.SessionController
import dev.sempaz.gametour.sys.Audio
import dev.sempaz.gametour.sys.Sys
import dev.sempaz.gametour.sys.Tweaks
import java.util.Locale

object Graph {
    private var inited = false
    lateinit var app: Context
        private set
    val prefs by lazy { Prefs(app) }
    val games by lazy { Games(app) }
    val store by lazy { SessionStore(app) }
    val sys by lazy { Sys(app) }
    val tweaks by lazy { Tweaks(app) }
    val audio by lazy { Audio(app).also { it.connect() } }
    val overlay by lazy { GameOverlay(app) }
    val controller by lazy { SessionController(app) }
    val calls by lazy { CallHandler(app) }

    @Synchronized
    fun init(ctx: Context) {
        if (inited) return
        app = ctx.applicationContext
        inited = true
    }
}

/** Texto em português ou inglês conforme o idioma do sistema. */
fun tr(pt: String, en: String): String = if (Locale.getDefault().language == "pt") pt else en
