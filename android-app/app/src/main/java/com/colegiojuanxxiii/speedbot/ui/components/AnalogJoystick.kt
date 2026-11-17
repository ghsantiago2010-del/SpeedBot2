package com.colegiojuanxxiii.speedbot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colegiojuanxxiii.speedbot.model.CarCommand
import com.colegiojuanxxiii.speedbot.model.JoystickData
import com.colegiojuanxxiii.speedbot.ui.theme.SpeedBotColors
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun AnalogJoystick(
    disabled: Boolean = false,
    onJoystickChange: (JoystickData) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestCallback by rememberUpdatedState(onJoystickChange)
    val density = LocalDensity.current

    val basinSizeDp = 212.dp
    val maxRadiusPx = with(density) { 75.dp.toPx() }
    val deadzonePx = with(density) { 10.dp.toPx() }
    val centerPx = with(density) { (basinSizeDp / 2).toPx() }

    var isDragging by remember { mutableStateOf(false) }
    var knobX by remember { mutableFloatStateOf(0f) }
    var knobY by remember { mutableFloatStateOf(0f) }
    var currentAngle by remember { mutableIntStateOf(0) }
    var throttle by remember { mutableIntStateOf(0) }
    var isBraking by remember { mutableStateOf(true) }
    var activeCommand by remember { mutableStateOf(CarCommand.S) }

    fun processPosition(localX: Float, localY: Float) {
        var dx = localX - centerPx
        var dy = localY - centerPx
        val rawDistance = sqrt(dx * dx + dy * dy)
        val distance = rawDistance.coerceAtMost(maxRadiusPx)

        if (rawDistance > maxRadiusPx && rawDistance > 0f) {
            dx = (dx / rawDistance) * maxRadiusPx
            dy = (dy / rawDistance) * maxRadiusPx
        }

        knobX = dx
        knobY = dy

        if (distance <= deadzonePx) {
            throttle = 0
            isBraking = true
            activeCommand = CarCommand.S
            latestCallback(
                JoystickData(
                    angle = 0,
                    throttle = 0,
                    normalizedX = 0f,
                    normalizedY = 0f,
                    command = CarCommand.S,
                    speedPwm = 0,
                    isBraking = true
                )
            )
            return
        }

        val effectiveDist = distance - deadzonePx
        val maxEffectiveDist = (maxRadiusPx - deadzonePx).coerceAtLeast(1f)
        val throttlePercent = ((effectiveDist / maxEffectiveDist) * 100f).roundToInt().coerceIn(0, 100)
        val speedPwm = ((throttlePercent / 100f) * 255f).roundToInt().coerceIn(0, 255)

        var mathAngle = atan2(-dy.toDouble(), dx.toDouble()) * (180.0 / PI)
        if (mathAngle < 0.0) {
            mathAngle += 360.0
        }
        val roundedAngle = mathAngle.roundToInt() % 360

        val cmd = when {
            roundedAngle >= 67.5 && roundedAngle < 112.5 -> CarCommand.F
            roundedAngle >= 22.5 && roundedAngle < 67.5 -> CarCommand.I
            roundedAngle >= 112.5 && roundedAngle < 157.5 -> CarCommand.G
            roundedAngle >= 157.5 && roundedAngle < 202.5 -> CarCommand.L
            roundedAngle >= 202.5 && roundedAngle < 247.5 -> CarCommand.H
            roundedAngle >= 247.5 && roundedAngle < 292.5 -> CarCommand.B
            roundedAngle >= 292.5 && roundedAngle < 337.5 -> CarCommand.J
            else -> CarCommand.R
        }

        throttle = throttlePercent
        currentAngle = roundedAngle
        isBraking = false
        activeCommand = cmd

        latestCallback(
            JoystickData(
                angle = roundedAngle,
                throttle = throttlePercent,
                normalizedX = dx / maxRadiusPx,
                normalizedY = -dy / maxRadiusPx,
                command = cmd,
                speedPwm = speedPwm,
                isBraking = false
            )
        )
    }

    fun releaseKnob() {
        isDragging = false
        knobX = 0f
        knobY = 0f
        throttle = 0
        isBraking = true
        activeCommand = CarCommand.S
        latestCallback(
            JoystickData(
                angle = currentAngle,
                throttle = 0,
                normalizedX = 0f,
                normalizedY = 0f,
                command = CarCommand.S,
                speedPwm = 0,
                isBraking = true
            )
        )
    }

    LaunchedEffect(disabled) {
        if (disabled) {
            isDragging = false
            knobX = 0f
            knobY = 0f
            throttle = 0
            isBraking = true
            activeCommand = CarCommand.S
        }
    }

    val directionText = remember(isBraking, throttle, currentAngle, activeCommand) {
        when {
            isBraking || throttle == 0 -> "FRENO ACTIVO / DETENIDO"
            currentAngle in 35..55 -> "DIAGONAL 45° (ADELANTE-DER)"
            currentAngle in 125..145 -> "DIAGONAL 135° (ADELANTE-IZQ)"
            currentAngle in 215..235 -> "DIAGONAL 225° (ATRÁS-IZQ)"
            currentAngle in 305..325 -> "DIAGONAL 315° (ATRÁS-DER)"
            activeCommand == CarCommand.F -> "ADELANTE ($currentAngle°)"
            activeCommand == CarCommand.B -> "ATRÁS ($currentAngle°)"
            activeCommand == CarCommand.L -> "GIRO IZQUIERDA ($currentAngle°)"
            activeCommand == CarCommand.R -> "GIRO DERECHA ($currentAngle°)"
            activeCommand == CarCommand.I -> "DIAGONAL DER ($currentAngle°)"
            activeCommand == CarCommand.G -> "DIAGONAL IZQ ($currentAngle°)"
            activeCommand == CarCommand.J -> "REVERSA DER ($currentAngle°)"
            activeCommand == CarCommand.H -> "REVERSA IZQ ($currentAngle°)"
            else -> "$currentAngle°"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 380.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // HUD superior: RUMBO & PEDAL
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SpeedBotColors.CardSlate.copy(alpha = 0.85f))
                .border(1.dp, SpeedBotColors.BorderSlate, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Explore,
                    contentDescription = "Rumbo",
                    tint = if (throttle > 0) SpeedBotColors.CyanLight else SpeedBotColors.TextDim,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RUMBO:",
                    color = SpeedBotColors.TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (throttle > 0) "$currentAngle°" else "0°",
                    color = if (throttle > 0) SpeedBotColors.CyanText else SpeedBotColors.TextDim,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "PEDAL:",
                    color = SpeedBotColors.TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(4.dp))
                val pedalColor = when {
                    throttle > 70 -> SpeedBotColors.RedLight
                    throttle > 30 -> SpeedBotColors.AmberText
                    throttle > 0 -> SpeedBotColors.EmeraldLight
                    else -> SpeedBotColors.TextDim
                }
                val suffix = when {
                    throttle >= 85 -> " MAX"
                    throttle > 0 -> " ⚡"
                    else -> ""
                }
                Text(
                    text = "$throttle%$suffix",
                    color = pedalColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Base circular con etiquetas cardinales y diagonales
        Box(
            modifier = Modifier.size(236.dp),
            contentAlignment = Alignment.Center
        ) {
            // Anillo exterior con grados
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        1.dp,
                        SpeedBotColors.BorderSlate.copy(alpha = 0.85f),
                        CircleShape
                    )
            ) {
                Text(
                    text = "90° F",
                    color = SpeedBotColors.CyanText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                )
                Text(
                    text = "270° B",
                    color = SpeedBotColors.CyanText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                )
                Text(
                    text = "180° L",
                    color = SpeedBotColors.CyanText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 2.dp)
                )
                Text(
                    text = "0° R",
                    color = SpeedBotColors.CyanText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 2.dp)
                )

                Text(
                    text = "45°",
                    color = SpeedBotColors.CyanLight.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 18.dp, end = 20.dp)
                )
                Text(
                    text = "135°",
                    color = SpeedBotColors.CyanLight.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 18.dp, start = 18.dp)
                )
                Text(
                    text = "225°",
                    color = SpeedBotColors.CyanLight.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 18.dp, start = 18.dp)
                )
                Text(
                    text = "315°",
                    color = SpeedBotColors.CyanLight.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 18.dp, end = 18.dp)
                )
            }

            // Cuenca táctil del Joystick
            Box(
                modifier = Modifier
                    .size(basinSizeDp)
                    .alpha(if (disabled) 0.4f else 1f)
                    .shadow(16.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                SpeedBotColors.SurfaceSlate.copy(alpha = 0.94f),
                                SpeedBotColors.CardSlate,
                                Color(0xFF070B12)
                            )
                        )
                    )
                    .border(2.dp, SpeedBotColors.BorderMuted, CircleShape)
                    .pointerInput(disabled) {
                        if (disabled) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            isDragging = true
                            processPosition(down.position.x, down.position.y)

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                    ?: event.changes.firstOrNull()
                                if (change == null || !change.pressed) {
                                    change?.consume()
                                    releaseKnob()
                                    break
                                }
                                change.consume()
                                processPosition(change.position.x, change.position.y)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Anillos concéntricos y línea vectorial
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height / 2f)

                    // Anillo de 50% punteado
                    drawCircle(
                        color = SpeedBotColors.BorderSlate,
                        radius = 72.dp.toPx(),
                        center = c,
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    )

                    // Anillo interno
                    drawCircle(
                        color = SpeedBotColors.BorderSlate.copy(alpha = 0.8f),
                        radius = 40.dp.toPx(),
                        center = c,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Línea vectorial punteada mientras se arrastra
                    if (isDragging && throttle > 0) {
                        val target = Offset(c.x + knobX, c.y + knobY)
                        drawLine(
                            color = SpeedBotColors.CyanNeon.copy(alpha = 0.85f),
                            start = c,
                            end = target,
                            strokeWidth = 3.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                        )
                        drawCircle(
                            color = Color(0xFF38BDF8),
                            radius = 4.dp.toPx(),
                            center = target
                        )
                    }
                }

                // Etiqueta 50%
                Text(
                    text = "50%",
                    color = SpeedBotColors.TextDim.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 26.dp)
                )

                // Zona muerta central / Freno
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SpeedBotColors.CardSlate.copy(alpha = 0.85f))
                        .border(2.dp, SpeedBotColors.BorderMuted.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBraking || throttle == 0) {
                        Text(
                            text = "FRENO",
                            color = SpeedBotColors.RedLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Perilla móvil del Joystick
                val knobBrush = when {
                    throttle > 0 -> Brush.linearGradient(
                        colors = listOf(
                            SpeedBotColors.CyanLight,
                            SpeedBotColors.BlueBright,
                            SpeedBotColors.CyanNeon
                        )
                    )
                    isBraking -> Brush.linearGradient(
                        colors = listOf(SpeedBotColors.RedDark, SpeedBotColors.SurfaceSlate)
                    )
                    else -> Brush.linearGradient(
                        colors = listOf(SpeedBotColors.SurfaceSlateAlt, SpeedBotColors.SurfaceSlate)
                    )
                }
                val knobBorder = when {
                    throttle > 0 -> Color.White
                    isBraking -> SpeedBotColors.RedNeon.copy(alpha = 0.85f)
                    else -> SpeedBotColors.BorderMuted
                }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(knobX.roundToInt(), knobY.roundToInt()) }
                        .size(80.dp)
                        .shadow(
                            elevation = if (throttle > 0) 16.dp else 8.dp,
                            shape = CircleShape,
                            ambientColor = if (throttle > 0) SpeedBotColors.CyanNeon else Color.Black,
                            spotColor = if (throttle > 0) SpeedBotColors.CyanNeon else Color.Black
                        )
                        .clip(CircleShape)
                        .background(knobBrush)
                        .border(2.dp, knobBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(SpeedBotColors.CardSlate.copy(alpha = 0.88f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (throttle == 0) {
                            Text(
                                text = "ALTO",
                                color = SpeedBotColors.RedLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$currentAngle°",
                                    color = SpeedBotColors.CyanText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "$throttle%",
                                    color = SpeedBotColors.AmberText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Barra inferior de estado dinámico del joystick
        val statusBg = if (isBraking || throttle == 0) {
            SpeedBotColors.RedDark.copy(alpha = 0.75f)
        } else {
            SpeedBotColors.CyanDark.copy(alpha = 0.75f)
        }
        val statusBorder = if (isBraking || throttle == 0) {
            SpeedBotColors.RedNeon.copy(alpha = 0.55f)
        } else {
            SpeedBotColors.CyanNeon.copy(alpha = 0.45f)
        }
        val statusTextColor = if (isBraking || throttle == 0) {
            SpeedBotColors.RedText
        } else {
            SpeedBotColors.CyanText
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(statusBg)
                .border(1.dp, statusBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isBraking || throttle == 0) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = SpeedBotColors.RedLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FRENO ACTIVO (AUTO DETENIDO)",
                    color = statusTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SpeedBotColors.CyanLight)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = directionText,
                    color = statusTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
