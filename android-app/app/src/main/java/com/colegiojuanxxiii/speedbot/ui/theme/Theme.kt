package com.colegiojuanxxiii.speedbot.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

object SpeedBotColors {
    val BgDark = Color(0xFF080C14)
    val BgDeep = Color(0xFF05080F)
    val CardSlate = Color(0xFF020617)
    val SurfaceSlate = Color(0xFF0F172A)
    val SurfaceSlateAlt = Color(0xFF1E293B)
    val BorderSlate = Color(0xFF1E293B)
    val BorderMuted = Color(0xFF334155)

    val CyanNeon = Color(0xFF06B6D4)
    val CyanLight = Color(0xFF22D3EE)
    val CyanText = Color(0xFF67E8F9)
    val CyanDark = Color(0xFF083344)

    val BlueAccent = Color(0xFF2563EB)
    val BlueBright = Color(0xFF3B82F6)

    val RedNeon = Color(0xFFEF4444)
    val RedLight = Color(0xFFF87171)
    val RedText = Color(0xFFFCA5A5)
    val RedDark = Color(0xFF450A0A)

    val AmberNeon = Color(0xFFF59E0B)
    val AmberText = Color(0xFFFCD34D)
    val AmberDark = Color(0xFF451A03)

    val EmeraldNeon = Color(0xFF10B981)
    val EmeraldLight = Color(0xFF34D399)
    val EmeraldText = Color(0xFF6EE7B7)
    val EmeraldDark = Color(0xFF022C22)

    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFFCBD5E1)
    val TextMuted = Color(0xFF94A3B8)
    val TextDim = Color(0xFF64748B)
}

private val SpeedBotDarkScheme = darkColorScheme(
    primary = SpeedBotColors.CyanNeon,
    onPrimary = SpeedBotColors.CardSlate,
    secondary = SpeedBotColors.BlueAccent,
    onSecondary = Color.White,
    background = SpeedBotColors.BgDark,
    onBackground = SpeedBotColors.TextPrimary,
    surface = SpeedBotColors.SurfaceSlate,
    onSurface = SpeedBotColors.TextPrimary,
    error = SpeedBotColors.RedNeon,
    onError = Color.White
)

@Composable
fun SpeedBotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SpeedBotDarkScheme,
        content = content
    )
}

@Composable
fun CyberGridOverlay(
    modifier: Modifier = Modifier,
    lineAlpha: Float = 0.04f
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val stepPx = 32.dp.toPx()
        val lineColor = Color.White.copy(alpha = lineAlpha)
        var x = 0f
        while (x <= size.width) {
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            x += stepPx
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += stepPx
        }
    }
}

@Composable
fun CyberImageBackground(
    @DrawableRes imageRes: Int,
    brightness: Float = 0.45f,
    gridAlpha: Float = 0.04f,
    overlayBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            SpeedBotColors.BgDark.copy(alpha = 0.80f),
            SpeedBotColors.BgDark.copy(alpha = 0.72f),
            SpeedBotColors.BgDark.copy(alpha = 0.94f)
        )
    ),
    content: @Composable BoxScope.() -> Unit
) {
    val colorMatrix = ColorMatrix().apply {
        setToScale(brightness, brightness, brightness, 1f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpeedBotColors.BgDark)
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.colorMatrix(colorMatrix),
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayBrush)
        )
        CyberGridOverlay(lineAlpha = gridAlpha)
        content()
    }
}
