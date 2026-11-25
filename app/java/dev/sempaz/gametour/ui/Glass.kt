/*
 * Game Tour - SPDX-License-Identifier: Apache-2.0
 * Componentes no estilo Liquid Glass (inspirado no visual do iOS 26, sem ativos da Apple).
 * O vidro é desenhado com translucidez + realce especular; o desfoque de fundo real
 * só é usado na janela do painel em jogo (blur-behind), para não pesar no jogo.
 * Cinco temas: Aurora (segue claro/escuro do sistema), Escuro (AMOLED), Cyberpunk, Oceano e GTA 6.
 */
package dev.sempaz.gametour.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
class Pal(
    val dark: Boolean,
    val text: Color,
    val text2: Color,
    val glass: Color,
    val glass2: Color,
    val hi: Color,
    val track: Color,
    val sheet: Color,
    val accent: Color,
    val accent2: Color,
    val base: Color,
    val blobs: List<Color>,
)

// ---------------------------------------------------------------- temas
private val white = Color.White

private val AuroraDark = Pal(
    dark = true, text = white, text2 = white.copy(alpha = 0.66f),
    glass = white.copy(alpha = 0.12f), glass2 = white.copy(alpha = 0.22f), hi = white.copy(alpha = 0.40f),
    track = white.copy(alpha = 0.20f), sheet = Color(0xFF181D3A).copy(alpha = 0.96f),
    accent = Color(0xFF3D93FF), accent2 = Color(0xFF2F7CF6),
    base = Color(0xFF10142B), blobs = listOf(Color(0xFF6C4DFF), Color(0xFF00C2B8), Color(0xFFFF5F8F)),
)

private val AuroraLight = Pal(
    dark = false, text = Color(0xFF10131C), text2 = Color(0xFF10131C).copy(alpha = 0.62f),
    glass = white.copy(alpha = 0.50f), glass2 = white.copy(alpha = 0.72f), hi = white.copy(alpha = 0.90f),
    track = Color(0xFF10131C).copy(alpha = 0.12f), sheet = Color(0xFFF6F7FD).copy(alpha = 0.97f),
    accent = Color(0xFF2478F0), accent2 = Color(0xFF1C64D8),
    base = Color(0xFFEEF0FB), blobs = listOf(Color(0xFFB9A8FF), Color(0xFF8FF0E6), Color(0xFFFFC1D6)),
)

private val AmoledPal = Pal(
    dark = true, text = white, text2 = white.copy(alpha = 0.62f),
    glass = white.copy(alpha = 0.08f), glass2 = white.copy(alpha = 0.15f), hi = white.copy(alpha = 0.30f),
    track = white.copy(alpha = 0.16f), sheet = Color(0xFF0B0B10).copy(alpha = 0.97f),
    accent = Color(0xFF4DA3FF), accent2 = Color(0xFF2F7CF6),
    base = Color(0xFF000000), blobs = listOf(Color(0xFF1B2540), Color(0xFF111A2E), Color(0xFF1A1A1F)),
)

private val CyberPal = Pal(
    dark = true, text = Color(0xFFEAF6FF), text2 = Color(0xFF8FD3E6).copy(alpha = 0.88f),
    glass = Color(0xFF00E5FF).copy(alpha = 0.07f), glass2 = Color(0xFF00E5FF).copy(alpha = 0.14f),
    hi = Color(0xFF00E5FF).copy(alpha = 0.55f), track = Color(0xFF00E5FF).copy(alpha = 0.18f),
    sheet = Color(0xFF0C0620).copy(alpha = 0.96f),
    accent = Color(0xFF00E5FF), accent2 = Color(0xFFFF2BD6),
    base = Color(0xFF07030F), blobs = listOf(Color(0xFFFF2BD6), Color(0xFF00E5FF), Color(0xFF7B2CFF)),
)

private val OceanPal = Pal(
    dark = true, text = white, text2 = white.copy(alpha = 0.68f),
    glass = white.copy(alpha = 0.10f), glass2 = white.copy(alpha = 0.18f), hi = Color(0xFFB6F3EE).copy(alpha = 0.45f),
    track = white.copy(alpha = 0.18f), sheet = Color(0xFF08202F).copy(alpha = 0.96f),
    accent = Color(0xFF2EC4B6), accent2 = Color(0xFF1D8FD6),
    base = Color(0xFF04131F), blobs = listOf(Color(0xFF0EA5A0), Color(0xFF1D6FD6), Color(0xFF3BC9DB)),
)

// GTA 6: pôr do sol de Vice City — rosa choque, laranja e roxo
private val Gta6Pal = Pal(
    dark = true, text = Color(0xFFFFF3FA), text2 = Color(0xFFFFC2E2).copy(alpha = 0.80f),
    glass = Color(0xFFFF4FA3).copy(alpha = 0.10f), glass2 = Color(0xFFFF8A3D).copy(alpha = 0.18f),
    hi = Color(0xFFFFB8DC).copy(alpha = 0.50f), track = Color(0xFFFF4FA3).copy(alpha = 0.22f),
    sheet = Color(0xFF1A0824).copy(alpha = 0.97f),
    accent = Color(0xFFFF4FA3), accent2 = Color(0xFFFF8A3D),
    base = Color(0xFF12051C), blobs = listOf(Color(0xFFFF2E93), Color(0xFFFF9A3D), Color(0xFF5B2A9E)),
)

class ThemeDef(val id: String, val pt: String, val en: String, val pal: Pal, val light: Pal? = null)

object Themes {
    val all: List<ThemeDef> = listOf(
        ThemeDef("aurora", "Aurora", "Aurora", AuroraDark, AuroraLight),
        ThemeDef("amoled", "Escuro", "Dark", AmoledPal),
        ThemeDef("cyber", "Cyberpunk", "Cyberpunk", CyberPal),
        ThemeDef("ocean", "Oceano", "Ocean", OceanPal),
        ThemeDef("gta6", "GTA 6", "GTA 6", Gta6Pal),
    )

    fun palFor(id: String, dark: Boolean): Pal {
        val key = if (id == "ember") "gta6" else id   // migra quem usava o antigo "Brasa"
        val t = all.firstOrNull { it.id == key } ?: all[0]
        return if (!dark && t.light != null) t.light else t.pal
    }
}

/** Tema escolhido (estado compartilhado pelo app e pelo overlay). */
object ThemeHolder {
    val id = mutableStateOf("aurora")
}

val LocalPal = staticCompositionLocalOf { AuroraDark }

object GT {
    val good = Color(0xFF4EE0A0)
    val warn = Color(0xFFFFB84D)
    val bad = Color(0xFFFF5F6D)
}

@Composable
fun GameTourTheme(forceDark: Boolean = false, content: @Composable () -> Unit) {
    val sys = isSystemInDarkTheme()
    val pal = Themes.palFor(ThemeHolder.id.value, sys || forceDark)
    CompositionLocalProvider(LocalPal provides pal, content = content)
}

/** Fundo colorido atrás do vidro (blobs de gradiente), conforme o tema. */
@Composable
fun GlassBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val p = LocalPal.current
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(p.base)
            fun blob(c: Color, center: Offset, r: Float) = drawCircle(
                Brush.radialGradient(listOf(c, Color.Transparent), center = center, radius = r),
                radius = r, center = center,
            )
            val w = size.width
            val h = size.height
            blob(p.blobs[0], Offset(w * 0.1f, 0f), w * 0.95f)
            blob(p.blobs[1], Offset(w, h * 0.30f), w * 0.85f)
            blob(p.blobs[2], Offset(w * 0.3f, h), w * 1.0f)
        }
        content()
    }
}

/** Superfície de vidro: translucidez + borda com realce especular. */
@Composable
fun Modifier.glass(shape: Shape = RoundedCornerShape(24.dp), strong: Boolean = false): Modifier {
    val p = LocalPal.current
    val top = if (strong) p.glass2 else p.glass
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(top, p.glass)))
        .border(1.dp, Brush.verticalGradient(listOf(p.hi, Color.White.copy(alpha = 0.06f))), shape)
}

@Composable
fun GText(
    text: String,
    size: Int = 14,
    weight: FontWeight = FontWeight.Normal,
    color: Color = LocalPal.current.text,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign = TextAlign.Unspecified,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(color = color, fontSize = size.sp, fontWeight = weight, textAlign = align),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun GIcon(v: ImageVector, size: Dp = 20.dp, tint: Color = LocalPal.current.text, modifier: Modifier = Modifier) {
    Image(
        painter = rememberVectorPainter(v),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size),
    )
}

@Composable
fun GCard(modifier: Modifier = Modifier, strong: Boolean = false, padding: Dp = 14.dp, content: @Composable () -> Unit) {
    Box(modifier.glass(RoundedCornerShape(24.dp), strong).padding(padding)) { content() }
}

@Composable
fun IosSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val f by animateFloatAsState(if (checked) 1f else 0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium), label = "sw")
    Box(
        Modifier
            .size(width = 46.dp, height = 28.dp)
            .clip(CircleShape)
            .background(lerp(Color.White.copy(alpha = 0.28f), Color(0xFF34C759), f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onChange(!checked) }
            .padding(2.dp),
    ) {
        Box(Modifier.offset(x = 18.dp * f).size(24.dp).clip(CircleShape).background(Color.White))
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPal.current
    Row(
        modifier.fillMaxWidth().clip(CircleShape).background(p.track).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .then(if (on) Modifier.glass(CircleShape, strong = true) else Modifier)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(i) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                GText(label, 13, if (on) FontWeight.SemiBold else FontWeight.Normal, if (on) p.text else p.text2, maxLines = 1)
            }
        }
    }
}

@Composable
fun GButton(label: String, primary: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPal.current
    val shape = CircleShape
    val base = modifier.height(48.dp).clip(shape)
    val styled = if (primary) base
        .background(Brush.verticalGradient(listOf(lerp(p.accent, Color.White, 0.22f), p.accent2)))
        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.1f))), shape)
    else base.glass(shape, strong = true)
    Box(
        styled.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        GText(label, 15, FontWeight.SemiBold, if (primary) Color.White else p.text, maxLines = 1,
            modifier = Modifier.padding(horizontal = 18.dp))
    }
}

@Composable
fun ListGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val p = LocalPal.current
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(p.glass)) { content() }
}

@Composable
fun ListRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val p = LocalPal.current
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() } else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GIcon(icon, 20.dp, p.text2)
        GText(title, 15, modifier = Modifier.weight(1f), maxLines = 1)
        if (value != null) GText(value, 13, color = p.text2, maxLines = 1)
        if (trailing != null) trailing()
    }
}

/** Barra de progresso fina (frequência por cluster etc.). */
@Composable
fun Bar(frac: Float, modifier: Modifier = Modifier) {
    val p = LocalPal.current
    Box(modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(p.track)) {
        Box(
            Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).fillMaxHeight()
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(lerp(p.accent, Color.White, 0.3f), p.accent))),
        )
    }
}

/** Anel de estabilidade (0..100). */
@Composable
fun Ring(value: Int, size: Dp = 84.dp, modifier: Modifier = Modifier) {
    val p = LocalPal.current
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 9.dp.toPx()
            val inset = sw / 2
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            drawArc(p.track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(sw))
            val col = if (value >= 80) GT.good else if (value >= 55) GT.warn else GT.bad
            drawArc(col, -90f, 360f * value / 100f, false, Offset(inset, inset), arcSize,
                style = Stroke(sw, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GText("$value", 22, FontWeight.Bold)
            GText("estável", 9, color = p.text2)
        }
    }
}
