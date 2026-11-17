package com.colegiojuanxxiii.speedbot.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.colegiojuanxxiii.speedbot.R
import com.colegiojuanxxiii.speedbot.audio.SoundEffectsManager
import com.colegiojuanxxiii.speedbot.model.BluetoothConnectionStatus
import com.colegiojuanxxiii.speedbot.model.BluetoothDeviceUiModel
import com.colegiojuanxxiii.speedbot.model.CarCommand
import com.colegiojuanxxiii.speedbot.model.JoystickData
import com.colegiojuanxxiii.speedbot.ui.components.AnalogJoystick
import com.colegiojuanxxiii.speedbot.ui.theme.CyberImageBackground
import com.colegiojuanxxiii.speedbot.ui.theme.SpeedBotColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val MAX_SESSION_SECONDS = 120

@Composable
fun CarControlScreen(
    device: BluetoothDeviceUiModel?,
    connectionStatus: BluetoothConnectionStatus,
    connectionError: String?,
    soundEffects: SoundEffectsManager,
    onSendCommand: (CarCommand) -> Unit,
    onDisconnect: suspend () -> Unit,
    onExitToBluetooth: () -> Unit,
    onGoToHome: () -> Unit,
    onOpenGuide: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var secondsRemaining by remember { mutableIntStateOf(MAX_SESSION_SECONDS) }
    var isTimeExpired by remember { mutableStateOf(false) }
    var controlsDisabled by remember { mutableStateOf(false) }
    var isTimerPaused by remember { mutableStateOf(false) }
    var showNavModal by remember { mutableStateOf(false) }

    var isLedOn by remember { mutableStateOf(false) }
    var lastCommand by remember { mutableStateOf(CarCommand.S) }
    var hornActive by remember { mutableStateOf(false) }
    var currentThrottle by remember { mutableIntStateOf(0) }
    var currentAngle by remember { mutableIntStateOf(0) }
    var isBrakingState by remember { mutableStateOf(true) }

    var lastSentTime by remember { mutableLongStateOf(0L) }
    var prevCommand by remember { mutableStateOf(CarCommand.S) }
    var expiredHandled by remember { mutableStateOf(false) }
    var hornRepeatJob by remember { mutableStateOf<Job?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "controlPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Cuenta regresiva cada 1 segundo
    LaunchedEffect(isTimerPaused, isTimeExpired) {
        while (isActive && !isTimerPaused && !isTimeExpired && secondsRemaining > 0) {
            delay(1000L)
            if (!isTimerPaused && !isTimeExpired && secondsRemaining > 0) {
                secondsRemaining = (secondsRemaining - 1).coerceAtLeast(0)
            }
        }
    }

    // Manejo cuando el tiempo llega a 00:00
    LaunchedEffect(secondsRemaining) {
        if (secondsRemaining == 0 && !expiredHandled) {
            expiredHandled = true
            onSendCommand(CarCommand.S)
            lastCommand = CarCommand.S
            currentThrottle = 0
            isBrakingState = true
            controlsDisabled = true
            isTimeExpired = true
            hornRepeatJob?.cancel()
            hornActive = false
            soundEffects.stopHorn()
            soundEffects.playWarning()

            onDisconnect()
            delay(2800L)
            onExitToBluetooth()
        }
    }

    // Limpieza al salir de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            hornRepeatJob?.cancel()
            soundEffects.stopHorn()
        }
    }

    fun handleJoystickChange(data: JoystickData) {
        if (controlsDisabled || isTimeExpired) return

        currentThrottle = data.throttle
        currentAngle = data.angle
        isBrakingState = data.isBraking || data.throttle == 0

        val now = System.currentTimeMillis()

        if (data.isBraking || data.throttle == 0) {
            if (prevCommand != CarCommand.S) {
                prevCommand = CarCommand.S
                lastCommand = CarCommand.S
                onSendCommand(CarCommand.S)
                soundEffects.playBeep(260.0, 80)
            }
            return
        }

        val commandChanged = data.command != prevCommand
        if (commandChanged || (now - lastSentTime) > 85L) {
            lastSentTime = now
            prevCommand = data.command
            lastCommand = data.command
            onSendCommand(data.command)
            soundEffects.playBeep(280.0 + data.throttle * 4.5, 40)
        }
    }

    fun startHornHold() {
        if (controlsDisabled || isTimeExpired) return
        hornActive = true
        soundEffects.startHorn()
        lastCommand = CarCommand.Z
        onSendCommand(CarCommand.Z)

        hornRepeatJob?.cancel()
        hornRepeatJob = scope.launch {
            while (isActive) {
                delay(160L)
                onSendCommand(CarCommand.Z)
            }
        }
    }

    fun stopHornHold() {
        hornRepeatJob?.cancel()
        hornRepeatJob = null
        hornActive = false
        soundEffects.stopHorn()
    }

    fun handleLedToggle() {
        if (controlsDisabled || isTimeExpired) return
        if (!isLedOn) {
            isLedOn = true
            lastCommand = CarCommand.E
            soundEffects.playBeep(1100.0, 80)
            onSendCommand(CarCommand.E)
        } else {
            isLedOn = false
            lastCommand = CarCommand.D
            soundEffects.playBeep(700.0, 80)
            onSendCommand(CarCommand.D)
        }
    }

    fun handleNavigateTo(destination: String) {
        soundEffects.playBeep(700.0, 80)
        if (destination == "guide") {
            showNavModal = false
            onOpenGuide()
            return
        }

        scope.launch {
            hornRepeatJob?.cancel()
            soundEffects.stopHorn()
            onSendCommand(CarCommand.S)
            lastCommand = CarCommand.S
            onDisconnect()
            showNavModal = false
            if (destination == "home") {
                onGoToHome()
            } else {
                onExitToBluetooth()
            }
        }
    }

    val isTimerCritical = secondsRemaining <= 30
    val formattedTime = "%02d:%02d".format(secondsRemaining / 60, secondsRemaining % 60)

    CyberImageBackground(
        imageRes = R.drawable.bg_control_car,
        brightness = 0.38f,
        gridAlpha = 0.03f
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ====================================================
            // BARRA SUPERIOR: TÍTULO, ESTADO HC-05, PESTAÑAS, SALIR
            // ====================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "SpeedBot",
                        style = TextStyle(
                            brush = Brush.horizontalGradient(
                                colors = listOf(SpeedBotColors.CyanLight, SpeedBotColors.BlueBright)
                            ),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    val isConnected = connectionStatus == BluetoothConnectionStatus.CONNECTED
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SpeedBotColors.CardSlate.copy(alpha = 0.85f))
                            .border(
                                1.dp,
                                if (isConnected) SpeedBotColors.BorderSlate else SpeedBotColors.RedNeon.copy(alpha = 0.7f),
                                CircleShape
                            )
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Radio,
                            contentDescription = null,
                            tint = if (isConnected) SpeedBotColors.EmeraldLight else SpeedBotColors.RedLight,
                            modifier = Modifier
                                .size(12.dp)
                                .alpha(pulseAlpha)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = device?.name ?: "HC-05",
                            color = SpeedBotColors.TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 95.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            soundEffects.playBeep(600.0, 80)
                            showNavModal = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, SpeedBotColors.CyanNeon.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SpeedBotColors.CardSlate.copy(alpha = 0.85f),
                            contentColor = SpeedBotColors.CyanText
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Apps,
                            contentDescription = null,
                            tint = SpeedBotColors.CyanLight,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pestañas",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            soundEffects.playBeep(600.0, 80)
                            showNavModal = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpeedBotColors.RedDark.copy(alpha = 0.9f),
                            contentColor = SpeedBotColors.RedText
                        ),
                        border = BorderStroke(1.dp, SpeedBotColors.RedNeon.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PowerSettingsNew,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SALIR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            // Alerta en caso de desconexión física repentina del HC-05
            if (connectionStatus == BluetoothConnectionStatus.ERROR && !isTimeExpired) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SpeedBotColors.RedDark.copy(alpha = 0.88f))
                        .border(1.dp, SpeedBotColors.RedNeon, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = SpeedBotColors.RedLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = connectionError ?: "Se perdió el enlace con el HC-05.",
                            color = SpeedBotColors.RedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { handleNavigateTo("bluetooth") },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpeedBotColors.RedNeon,
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = "RECONECTAR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ====================================================
            // TEMPORIZADOR + PAUSA + LÍMITE 2:00 MIN + TELEMETRÍA
            // ====================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isTimerCritical) SpeedBotColors.RedDark.copy(alpha = 0.75f)
                            else SpeedBotColors.CardSlate.copy(alpha = 0.88f)
                        )
                        .border(
                            1.dp,
                            if (isTimerCritical) SpeedBotColors.RedNeon else SpeedBotColors.BorderSlate,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = when {
                                    isTimerPaused -> SpeedBotColors.AmberNeon
                                    isTimerCritical -> SpeedBotColors.RedLight
                                    else -> SpeedBotColors.CyanLight
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "TIEMPO DE CONTROL",
                                    color = SpeedBotColors.TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                if (isTimerPaused) {
                                    Text(
                                        text = "⏸ PAUSADO",
                                        color = SpeedBotColors.AmberNeon,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formattedTime,
                                color = when {
                                    isTimerPaused -> SpeedBotColors.AmberText
                                    isTimerCritical -> SpeedBotColors.RedLight
                                    else -> SpeedBotColors.CyanText
                                },
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))

                            OutlinedButton(
                                onClick = {
                                    soundEffects.playBeep(if (isTimerPaused) 880.0 else 440.0, 80)
                                    isTimerPaused = !isTimerPaused
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isTimerPaused) SpeedBotColors.EmeraldNeon.copy(alpha = 0.6f)
                                    else SpeedBotColors.AmberNeon.copy(alpha = 0.6f)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isTimerPaused) SpeedBotColors.EmeraldDark else SpeedBotColors.AmberDark,
                                    contentColor = if (isTimerPaused) SpeedBotColors.EmeraldText else SpeedBotColors.AmberText
                                )
                            ) {
                                Icon(
                                    imageVector = if (isTimerPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isTimerPaused) "REANUDAR" else "PAUSAR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = SpeedBotColors.BorderSlate)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Ajustes de tiempo (+30s límite estricto 2:00 + Reinicio 2:00)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LÍMITE MÁX: 2:00 MIN",
                            color = SpeedBotColors.TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val atMaxLimit = secondsRemaining >= MAX_SESSION_SECONDS
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (atMaxLimit) SpeedBotColors.SurfaceSlate.copy(alpha = 0.5f)
                                        else SpeedBotColors.SurfaceSlate
                                    )
                                    .border(
                                        1.dp,
                                        if (atMaxLimit) SpeedBotColors.BorderSlate else SpeedBotColors.BorderMuted,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        if (secondsRemaining >= MAX_SESSION_SECONDS) {
                                            soundEffects.playWarning()
                                        } else {
                                            soundEffects.playBeep(1020.0, 80)
                                            secondsRemaining = (secondsRemaining + 30).coerceAtMost(MAX_SESSION_SECONDS)
                                            if (isTimeExpired) {
                                                isTimeExpired = false
                                                controlsDisabled = false
                                                expiredHandled = false
                                            }
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = if (atMaxLimit) SpeedBotColors.TextDim else SpeedBotColors.CyanText,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (atMaxLimit) "+30s (Máx)" else "+30s",
                                    color = if (atMaxLimit) SpeedBotColors.TextDim else SpeedBotColors.CyanText,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SpeedBotColors.SurfaceSlate)
                                    .border(1.dp, SpeedBotColors.BorderMuted, RoundedCornerShape(8.dp))
                                    .clickable {
                                        soundEffects.playBeep(980.0, 100)
                                        secondsRemaining = MAX_SESSION_SECONDS
                                        isTimeExpired = false
                                        controlsDisabled = false
                                        isTimerPaused = false
                                        expiredHandled = false
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    tint = SpeedBotColors.CyanLight,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "2:00",
                                    color = SpeedBotColors.TextSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Barra de progreso lineal del tiempo
                    val progress = (secondsRemaining.toFloat() / MAX_SESSION_SECONDS.toFloat()).coerceIn(0f, 1f)
                    val barBrush = when {
                        isTimerPaused -> Brush.horizontalGradient(listOf(SpeedBotColors.AmberNeon, SpeedBotColors.AmberText))
                        isTimerCritical -> Brush.horizontalGradient(listOf(SpeedBotColors.RedNeon, SpeedBotColors.AmberNeon))
                        else -> Brush.horizontalGradient(listOf(SpeedBotColors.CyanNeon, SpeedBotColors.BlueBright))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(SpeedBotColors.CardSlate)
                            .border(0.5.dp, SpeedBotColors.BorderSlate, CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(CircleShape)
                                .background(barBrush)
                        )
                    }
                }

                // 3 Indicadores de Telemetría (Rumbo, Pedal, Estado)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelemetryMiniCard(
                        icon = Icons.Filled.Explore,
                        iconTint = SpeedBotColors.CyanLight,
                        label = "RUMBO",
                        value = if (currentThrottle > 0) "$currentAngle°" else "0°",
                        valueColor = SpeedBotColors.CyanText,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryMiniCard(
                        icon = Icons.Filled.Speed,
                        iconTint = SpeedBotColors.AmberNeon,
                        label = "PEDAL",
                        value = "$currentThrottle%",
                        valueColor = when {
                            currentThrottle > 75 -> SpeedBotColors.RedLight
                            currentThrottle > 35 -> SpeedBotColors.AmberText
                            currentThrottle > 0 -> SpeedBotColors.EmeraldLight
                            else -> SpeedBotColors.TextDim
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryMiniCard(
                        icon = Icons.Filled.Shield,
                        iconTint = SpeedBotColors.RedLight,
                        label = "ESTADO",
                        value = if (isBrakingState) "FRENANDO" else "EN MARCHA",
                        valueColor = if (isBrakingState) SpeedBotColors.RedLight else SpeedBotColors.EmeraldLight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ====================================================
            // JOYSTICK ANALÓGICO 360°
            // ====================================================
            AnalogJoystick(
                disabled = controlsDisabled || isTimeExpired,
                onJoystickChange = ::handleJoystickChange
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ====================================================
            // BOTONES DE ACCIÓN: BOCINA (MANTENER) & LED (ÚNICO)
            // ====================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // BOCINA (Mantener presionado)
                val hornEnabled = !(controlsDisabled || isTimeExpired)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .scale(if (hornActive) 0.96f else 1f)
                        .alpha(if (hornEnabled) 1f else 0.4f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (hornActive) SpeedBotColors.AmberNeon
                            else SpeedBotColors.CardSlate.copy(alpha = 0.88f)
                        )
                        .border(
                            1.5.dp,
                            if (hornActive) SpeedBotColors.AmberText else SpeedBotColors.BorderMuted,
                            RoundedCornerShape(16.dp)
                        )
                        .pointerInput(hornEnabled) {
                            if (!hornEnabled) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()
                                startHornHold()
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val anyPressed = event.changes.any { it.pressed }
                                    if (!anyPressed) {
                                        stopHornHold()
                                        break
                                    }
                                }
                            }
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = "Bocina",
                        tint = if (hornActive) SpeedBotColors.CardSlate else SpeedBotColors.AmberNeon,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (hornActive) "¡SONANDO!" else "BOCINA",
                        color = if (hornActive) SpeedBotColors.CardSlate else SpeedBotColors.AmberText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (hornActive) "SUELTA PARA PARAR" else "MANTENER PRESIONADO",
                        color = if (hornActive) SpeedBotColors.CardSlate.copy(alpha = 0.85f) else SpeedBotColors.TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                // LED TOGGLE (Botón único E/D)
                val ledEnabled = !(controlsDisabled || isTimeExpired)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(if (ledEnabled) 1f else 0.4f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isLedOn) SpeedBotColors.EmeraldNeon
                            else SpeedBotColors.CardSlate.copy(alpha = 0.88f)
                        )
                        .border(
                            1.5.dp,
                            if (isLedOn) SpeedBotColors.EmeraldText else SpeedBotColors.BorderMuted,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable(enabled = ledEnabled, onClick = ::handleLedToggle)
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = "LED",
                        tint = if (isLedOn) SpeedBotColors.CardSlate else SpeedBotColors.TextDim,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isLedOn) "LED ENCENDIDO" else "LED APAGADO",
                        color = if (isLedOn) SpeedBotColors.CardSlate else SpeedBotColors.TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = if (isLedOn) "(Cmd: E activa)" else "(Cmd: D apagada)",
                        color = if (isLedOn) SpeedBotColors.CardSlate.copy(alpha = 0.85f) else SpeedBotColors.TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Barra inferior de Telemetría en Tiempo Real
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SpeedBotColors.CardSlate.copy(alpha = 0.88f))
                    .border(1.dp, SpeedBotColors.BorderSlate, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isBrakingState) SpeedBotColors.RedNeon else SpeedBotColors.EmeraldLight)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBrakingState) "Freno de Motor:" else "Enviando:",
                        color = SpeedBotColors.TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "[${lastCommand.code}] ${lastCommand.description}",
                    color = SpeedBotColors.CyanText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ====================================================
        // MODAL: TIEMPO AGOTADO
        // ====================================================
        if (isTimeExpired) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(SpeedBotColors.SurfaceSlate)
                        .border(2.dp, SpeedBotColors.RedNeon, RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SpeedBotColors.RedDark)
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = SpeedBotColors.RedLight,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "TIEMPO AGOTADO",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Se completaron los 2 minutos de sesión. El auto se detuvo ('S') y se desconectó el HC-05 por seguridad.",
                        color = SpeedBotColors.TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Volviendo a conectar al auto...",
                        color = SpeedBotColors.CyanLight,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // ====================================================
        // MODAL DE NAVEGACIÓN: ¿A QUÉ PESTAÑA QUERÉS IR?
        // ====================================================
        if (showNavModal) {
            Dialog(onDismissRequest = { showNavModal = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(SpeedBotColors.SurfaceSlate)
                        .border(2.dp, SpeedBotColors.CyanNeon.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(SpeedBotColors.CyanDark)
                            .border(1.dp, SpeedBotColors.CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Apps,
                            contentDescription = null,
                            tint = SpeedBotColors.CyanLight,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "¿A QUÉ PESTAÑA QUERÉS IR?",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Selecciona la sección a la que deseas navegar:",
                        color = SpeedBotColors.TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Opción 1: Menú Principal
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        SpeedBotColors.CyanNeon,
                                        SpeedBotColors.BlueAccent,
                                        SpeedBotColors.CyanNeon
                                    )
                                )
                            )
                            .clickable { handleNavigateTo("home") }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(SpeedBotColors.CardSlate.copy(alpha = 0.2f))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = null,
                                tint = SpeedBotColors.CardSlate,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "MENÚ PRINCIPAL",
                                color = SpeedBotColors.CardSlate,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Inicio • Colegio Juan XXIII",
                                color = SpeedBotColors.SurfaceSlate,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Opción 2: Conectar el Auto (Bluetooth)
                    NavOptionCard(
                        icon = Icons.Filled.Bluetooth,
                        iconTint = SpeedBotColors.RedLight,
                        iconBg = SpeedBotColors.RedDark,
                        borderColor = SpeedBotColors.RedNeon.copy(alpha = 0.4f),
                        title = "CONECTAR EL AUTO",
                        subtitle = "Vincular Bluetooth HC-05",
                        onClick = { handleNavigateTo("bluetooth") }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Opción 3: Guía Arduino Nano
                    NavOptionCard(
                        icon = Icons.Filled.MenuBook,
                        iconTint = SpeedBotColors.CyanLight,
                        iconBg = SpeedBotColors.CyanDark,
                        borderColor = SpeedBotColors.CyanNeon.copy(alpha = 0.4f),
                        title = "GUÍA ARDUINO NANO",
                        subtitle = "Conexiones y código C++",
                        onClick = { handleNavigateTo("guide") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Continuar controlando
                    OutlinedButton(
                        onClick = {
                            soundEffects.playBeep(800.0, 80)
                            showNavModal = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SpeedBotColors.BorderMuted),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SpeedBotColors.CardSlate,
                            contentColor = SpeedBotColors.TextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CONTINUAR CONTROLANDO",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryMiniCard(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SpeedBotColors.CardSlate.copy(alpha = 0.85f))
            .border(1.dp, SpeedBotColors.BorderSlate, RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = SpeedBotColors.TextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}

@Composable
private fun NavOptionCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    borderColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SpeedBotColors.CardSlate.copy(alpha = 0.92f))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg)
                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                .padding(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = subtitle,
                color = SpeedBotColors.TextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
