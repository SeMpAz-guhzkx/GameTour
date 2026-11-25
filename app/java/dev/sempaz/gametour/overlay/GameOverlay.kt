/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 *
 * Overlay em jogo: rótulo de FPS (plano e leve), pílula lateral e painel de vidro.
 * O desfoque real só existe na janela do painel aberto (blur-behind); o rótulo de FPS e a
 * pílula fechada são só cor translúcida, para não pesar no jogo.
 * Padrão de janela adaptado do GameSideBar (GameSpace / AxionOS).
 */
package dev.sempaz.gametour.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import dev.sempaz.gametour.Graph
import dev.sempaz.gametour.ui.GameTourTheme
import dev.sempaz.gametour.ui.ThemeHolder

private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)

    init {
        controller.performAttach()
        controller.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED
    }

    fun start() { registry.currentState = Lifecycle.State.RESUMED }
    fun stop() { registry.currentState = Lifecycle.State.CREATED }

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry
}

class GameOverlay(private val appCtx: Context) {
    private val handler = Handler(Looper.getMainLooper())

    // O LifecycleRegistry exige a thread principal: só criar dentro de handler.post.
    private var ownerRef: OverlayOwner? = null
    private fun owner(): OverlayOwner = ownerRef ?: OverlayOwner().also { ownerRef = it }

    private val wctx: Context by lazy {
        val dm = appCtx.getSystemService(DisplayManager::class.java)
        appCtx.createDisplayContext(dm.getDisplay(Display.DEFAULT_DISPLAY))
            .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
    }
    private val wm: WindowManager by lazy { wctx.getSystemService(WindowManager::class.java) }
    private val density: Float get() = wctx.resources.displayMetrics.density

    private var fpsView: View? = null
    private var pillView: View? = null
    private var pillLp: WindowManager.LayoutParams? = null
    private var fpsLp: WindowManager.LayoutParams? = null
    private var listening = false

    // O jogo costuma abrir em retrato e girar para paisagem: reencosta a alça e o rótulo na tela nova.
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) {
            handler.postDelayed({ redock() }, 250L)
            handler.postDelayed({ redock() }, 900L)
        }
    }

    private fun redock() {
        val b = screenBounds()
        val w = dp(HANDLE_W_DP)
        val h = dp(HANDLE_H_DP)
        pillLp?.let { lp ->
            lp.x = if (Graph.prefs.pillSide >= 0) b.width() - w else 0
            lp.y = lp.y.coerceIn(0, (b.height() - h).coerceAtLeast(0))
            pillView?.let { runCatching { wm.updateViewLayout(it, lp) } }
        }
        fpsLp?.let { lp ->
            val vw = fpsView?.width ?: 0
            val vh = fpsView?.height ?: 0
            lp.x = lp.x.coerceIn(0, (b.width() - vw).coerceAtLeast(0))
            lp.y = lp.y.coerceIn(0, (b.height() - vh).coerceAtLeast(0))
            fpsView?.let { runCatching { wm.updateViewLayout(it, lp) } }
        }
    }
    private var panelView: View? = null
    private var pkg: String = ""

    private fun dp(v: Int) = (v * density).toInt()

    fun show(pkg: String, showFps: Boolean) {
        handler.post {
            hideNow()
            this.pkg = pkg
            if (!listening) {
                listening = true
                runCatching { appCtx.getSystemService(DisplayManager::class.java).registerDisplayListener(displayListener, handler) }
            }
            // conecta o cliente da plataforma (captura/gravação) antes de o usuário precisar dele
            runCatching { com.android.axion.platform.AxPlatformClient.getInstance().init(appCtx) }
            owner().start()
            if (showFps) addFps()
            addPill()
        }
    }

    fun hide() {
        handler.post {
            hideNow()
            if (listening) {
                listening = false
                runCatching { appCtx.getSystemService(DisplayManager::class.java).unregisterDisplayListener(displayListener) }
            }
        }
    }

    fun fpsVisible(): Boolean = fpsView != null

    fun setFps(on: Boolean) {
        handler.post { if (on && fpsView == null) addFps() else if (!on) removeFps() }
    }

    private fun hideNow() {
        removeCall()
        removeBanner()
        removePanel()
        removeFps()
        pillView?.let { runCatching { wm.removeViewImmediate(it) } }
        pillView = null
        pillLp = null
        ownerRef?.stop()
    }

    private fun compose(content: @Composable () -> Unit): ComposeView {
        ThemeHolder.id.value = Graph.prefs.theme
        return ComposeView(wctx).apply {
            val o = owner()
            setViewTreeLifecycleOwner(o)
            setViewTreeSavedStateRegistryOwner(o)
            setContent { GameTourTheme(forceDark = true) { content() } }
        }
    }

    private fun params(w: Int, h: Int, flags: Int) = WindowManager.LayoutParams(
        w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        preferMinimalPostProcessing = true
    }

    // ---- rótulo de FPS (plano) ----
    private fun screenBounds() = wm.currentWindowMetrics.bounds

    /** Move a janela por (dx, dy) px, sem deixar sair da tela. */
    private fun moveWindow(view: View?, lp: WindowManager.LayoutParams, dx: Float, dy: Float) {
        val b = screenBounds()
        val w = view?.width ?: 0
        val h = view?.height ?: 0
        lp.x = (lp.x + dx.toInt()).coerceIn(0, (b.width() - w).coerceAtLeast(0))
        lp.y = (lp.y + dy.toInt()).coerceIn(0, (b.height() - h).coerceAtLeast(0))
        view?.let { runCatching { wm.updateViewLayout(it, lp) } }
    }

    private fun addFps() {
        if (fpsView != null) return
        lateinit var lp: WindowManager.LayoutParams
        val v = compose {
            FpsLabel(
                onDrag = { dx, dy -> moveWindow(fpsView, lp, dx, dy) },
                onDragEnd = { Graph.prefs.labelX = lp.x; Graph.prefs.labelY = lp.y },
            )
        }
        // tocável (para o arrasto por toque longo); só ocupa a área do próprio rótulo
        lp = params(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = Graph.prefs.labelX.takeIf { it >= 0 } ?: dp(14)
            y = Graph.prefs.labelY.takeIf { it >= 0 } ?: dp(14)
            title = "GameTourFps"
        }
        runCatching { wm.addView(v, lp); fpsView = v; fpsLp = lp }
    }

    private fun removeFps() {
        fpsView?.let { runCatching { wm.removeViewImmediate(it) } }
        fpsView = null
        fpsLp = null
    }

    // ---- pílula ----
    /**
     * Alça discreta na borda da tela (estilo Game Turbo): puxe para dentro para abrir o painel.
     * Segure e arraste para mudar de altura ou de lado; ao soltar, ela encosta na borda mais próxima.
     * Com "alça invisível" ligada, só se vê o gesto: puxar da borda abre o painel.
     */
    private fun addPill() {
        val w = dp(HANDLE_W_DP)
        val h = dp(HANDLE_H_DP)
        lateinit var lp: WindowManager.LayoutParams
        val sideState = androidx.compose.runtime.mutableStateOf(Graph.prefs.pillSide)
        // a alça fica SEMPRE visível (vidro translúcido, estilo Game Turbo); o modo "invisível" foi retirado
        val v = compose {
            EdgeHandle(
                rightSide = sideState.value >= 0,
                invisible = false,
                onOpen = { handler.post { addPanel() } },
                onDrag = { dx, dy -> moveWindow(pillView, lp, dx, dy) },
                onDragEnd = {
                    val b = screenBounds()
                    val right = lp.x + w / 2 > b.width() / 2
                    lp.x = if (right) b.width() - w else 0
                    sideState.value = if (right) 1 else -1
                    Graph.prefs.pillSide = sideState.value
                    Graph.prefs.pillY = lp.y
                    pillView?.let { runCatching { wm.updateViewLayout(it, lp) } }
                },
            )
        }
        // não deixar o gesto de voltar do sistema roubar o puxão da borda
        v.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            view.systemGestureExclusionRects = listOf(android.graphics.Rect(0, 0, view.width, view.height))
        }
        val b = screenBounds()
        lp = params(
            w, h,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (Graph.prefs.pillSide >= 0) b.width() - w else 0
            y = Graph.prefs.pillY.coerceIn(0, (b.height() - h).coerceAtLeast(0))
            title = "GameTourPill"
        }
        runCatching { wm.addView(v, lp); pillView = v; pillLp = lp }
    }

    companion object {
        private const val HANDLE_W_DP = 36
        private const val HANDLE_H_DP = 96
    }

    // ---- painel (com blur-behind real da janela) ----
    private fun addPanel() {
        if (panelView != null) return
        val v = compose { Panel(pkg = pkg, onClose = { handler.post { removePanel() } }) }
        val lp = params(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_BLUR_BEHIND or
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
        ).apply {
            blurBehindRadius = dp(28)
            dimAmount = 0.30f
            title = "GameTourPanel"
        }
        runCatching { wm.addView(v, lp); panelView = v }
    }

    // ---- aviso de chamada ----
    private var callView: View? = null

    fun showCall(name: String, onAccept: () -> Unit, onReject: () -> Unit) {
        handler.post {
            if (callView != null) return@post
            val v = compose {
                CallBanner(
                    name = name,
                    onAccept = { onAccept(); handler.post { removeCall() } },
                    onReject = { onReject(); handler.post { removeCall() } },
                )
            }
            val lp = params(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = dp(14); title = "GameTourCall" }
            runCatching { wm.addView(v, lp); callView = v }
        }
    }

    fun hideCall() { handler.post { removeCall() } }

    private fun removeCall() {
        callView?.let { runCatching { wm.removeViewImmediate(it) } }
        callView = null
    }

    // ---- banner de notificação ----
    private var bannerView: View? = null
    private val bannerTimeout = Runnable { removeBanner() }

    fun showBanner(appLabel: String, title: String, text: String, pkg: String) {
        handler.post {
            removeBanner()
            val v = compose { NotifBanner(appLabel, title, text, pkg) { handler.post { removeBanner() } } }
            val lp = params(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = dp(12); this.title = "GameTourBanner" }
            runCatching { wm.addView(v, lp); bannerView = v }
            handler.removeCallbacks(bannerTimeout)
            handler.postDelayed(bannerTimeout, 4500L)
        }
    }

    private fun removeBanner() {
        handler.removeCallbacks(bannerTimeout)
        bannerView?.let { runCatching { wm.removeViewImmediate(it) } }
        bannerView = null
    }

    private fun removePanel() {
        panelView?.let { runCatching { wm.removeViewImmediate(it) } }
        panelView = null
    }
}
