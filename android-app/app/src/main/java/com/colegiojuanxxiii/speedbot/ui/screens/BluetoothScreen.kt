package com.colegiojuanxxiii.speedbot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colegiojuanxxiii.speedbot.R
import com.colegiojuanxxiii.speedbot.model.BluetoothConnectionStatus
import com.colegiojuanxxiii.speedbot.model.BluetoothDeviceUiModel
import com.colegiojuanxxiii.speedbot.ui.theme.CyberImageBackground
import com.colegiojuanxxiii.speedbot.ui.theme.SpeedBotColors

@Composable
fun BluetoothScreen(
    status: BluetoothConnectionStatus,
    errorMessage: String?,
    isBluetoothSupported: Boolean,
    isBluetoothEnabled: Boolean,
    hasPermissions: Boolean,
    bondedDevices: List<BluetoothDeviceUiModel>,
    discoveredDevices: List<BluetoothDeviceUiModel>,
    allowAutoConnect: Boolean,
    onAutoConnectCheck: () -> Unit,
    onRequestPermissions: () -> Unit,
    onRequestEnableBluetooth: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onConnectDevice: (BluetoothDeviceUiModel) -> Unit,
    onConnectSimulated: () -> Unit,
    onRefreshBonded: () -> Unit,
    onBack: () -> Unit,
    onOpenGuide: () -> Unit
) {
    var autoAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(allowAutoConnect, hasPermissions, isBluetoothEnabled) {
        onRefreshBonded()
        if (allowAutoConnect && !autoAttempted && hasPermissions && isBluetoothEnabled) {
            autoAttempted = true
            onAutoConnectCheck()
        }
    }

    CyberImageBackground(
        imageRes = R.drawable.bg_bluetooth,
        brightness = 0.50f,
        gridAlpha = 0.04f,
        overlayBrush = Brush.verticalGradient(
            colors = listOf(
                SpeedBotColors.CardSlate.copy(alpha = 0.78f),
                SpeedBotColors.CardSlate.copy(alpha = 0.72f),
                SpeedBotColors.CardSlate.copy(alpha = 0.88f)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Barra superior: Botón volver directo al Menú Principal + badge HC-05 SPP
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    border = BorderStroke(1.dp, SpeedBotColors.BorderMuted),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SpeedBotColors.CardSlate.copy(alpha = 0.85f),
                        contentColor = SpeedBotColors.TextSecondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = SpeedBotColors.CyanLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Menú Principal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SpeedBotColors.CardSlate.copy(alpha = 0.85f))
                        .border(1.dp, SpeedBotColors.RedNeon.copy(alpha = 0.35f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Radio,
                        contentDescription = null,
                        tint = SpeedBotColors.RedLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "HC-05 SPP",
                        color = SpeedBotColors.RedText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Contenedor Principal
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Encabezado
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SpeedBotColors.CardSlate.copy(alpha = 0.85f))
                        .border(1.dp, SpeedBotColors.RedNeon.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bluetooth,
                        contentDescription = null,
                        tint = SpeedBotColors.RedLight,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "CONECTAR AL AUTO",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Selecciona tu módulo Bluetooth Clásico HC-05",
                    color = SpeedBotColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Tarjeta de Estado de Conexión
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SpeedBotColors.CardSlate.copy(alpha = 0.88f))
                        .border(1.dp, SpeedBotColors.BorderSlate, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ESTADO DE CONEXIÓN:",
                            color = SpeedBotColors.TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        StatusBadge(status = status)
                    }

                    // Mensaje de error en español
                    if (status == BluetoothConnectionStatus.ERROR && !errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SpeedBotColors.RedDark.copy(alpha = 0.75f))
                                .border(1.dp, SpeedBotColors.RedNeon.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = SpeedBotColors.RedLight,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Fallo de conexión",
                                        color = SpeedBotColors.RedText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = errorMessage,
                                        color = SpeedBotColors.TextSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onStartDiscovery,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SpeedBotColors.RedNeon,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "REINTENTAR",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                OutlinedButton(
                                    onClick = onConnectSimulated,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, SpeedBotColors.BorderMuted),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = SpeedBotColors.SurfaceSlate,
                                        contentColor = SpeedBotColors.AmberText
                                    )
                                ) {
                                    Text(
                                        text = "MODO PRUEBA",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Alerta de permisos faltantes en Android
                if (!hasPermissions) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SpeedBotColors.CyanDark.copy(alpha = 0.75f))
                            .border(1.dp, SpeedBotColors.CyanNeon.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Faltan permisos Bluetooth de Android",
                            color = SpeedBotColors.CyanText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Concede el permiso para ver tus dispositivos HC-05 vinculados y conectar el auto.",
                            color = SpeedBotColors.TextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRequestPermissions,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpeedBotColors.CyanNeon,
                                contentColor = SpeedBotColors.CardSlate
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "OTORGAR PERMISOS BLUETOOTH",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Alerta si el Bluetooth del celular está apagado
                if (isBluetoothSupported && !isBluetoothEnabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SpeedBotColors.AmberDark.copy(alpha = 0.75f))
                            .border(1.dp, SpeedBotColors.AmberNeon.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "El Bluetooth del celular está apagado",
                            color = SpeedBotColors.AmberText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Activa el Bluetooth de Android para detectar y conectar tu HC-05.",
                            color = SpeedBotColors.TextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRequestEnableBluetooth,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpeedBotColors.AmberNeon,
                                contentColor = SpeedBotColors.CardSlate
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ACTIVAR BLUETOOTH DEL CELULAR",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Sección: Dispositivos previamente vinculados en el celular (Android bondedDevices)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = SpeedBotColors.EmeraldLight,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "YA VINCULADOS EN TU CELULAR",
                                color = SpeedBotColors.EmeraldText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Text(
                            text = "Baud 9600",
                            color = SpeedBotColors.CyanLight,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (bondedDevices.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SpeedBotColors.CardSlate.copy(alpha = 0.82f))
                                .border(1.dp, SpeedBotColors.BorderSlate, RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "No hay módulos HC-05 vinculados aún.",
                                color = SpeedBotColors.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Vincula tu módulo HC-05 en los Ajustes Bluetooth de Android con PIN 1234 o 0000, o toca Buscar Dispositivos Cercanos.",
                                color = SpeedBotColors.TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        bondedDevices.forEach { dev ->
                            BluetoothDeviceCard(
                                device = dev,
                                enabled = status != BluetoothConnectionStatus.CONNECTING,
                                onClick = { onConnectDevice(dev) }
                            )
                        }
                    }
                }

                // Sección: Dispositivos cercanos encontrados por escaneo clásico
                if (discoveredDevices.isNotEmpty() || status == BluetoothConnectionStatus.SCANNING) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.BluetoothSearching,
                                    contentDescription = null,
                                    tint = SpeedBotColors.CyanLight,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DISPOSITIVOS CERCANOS",
                                    color = SpeedBotColors.CyanText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (status == BluetoothConnectionStatus.SCANNING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = SpeedBotColors.CyanLight
                                )
                            }
                        }

                        discoveredDevices.forEach { dev ->
                            BluetoothDeviceCard(
                                device = dev,
                                enabled = status != BluetoothConnectionStatus.CONNECTING,
                                onClick = { onConnectDevice(dev) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Botones de acción principales
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isScanning = status == BluetoothConnectionStatus.SCANNING
                    val isConnecting = status == BluetoothConnectionStatus.CONNECTING

                    Button(
                        onClick = {
                            if (isScanning) onStopDiscovery() else onStartDiscovery()
                        },
                        enabled = !isConnecting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        SpeedBotColors.RedNeon,
                                        Color(0xFFF97316),
                                        SpeedBotColors.RedNeon
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(1.dp, SpeedBotColors.RedText.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = SpeedBotColors.CardSlate
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "CONECTANDO AL HC-05...",
                                color = SpeedBotColors.CardSlate,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        } else if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = SpeedBotColors.CardSlate
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "BUSCANDO... (TOCA PARA PARAR)",
                                color = SpeedBotColors.CardSlate,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = SpeedBotColors.CardSlate,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BUSCAR DISPOSITIVOS BLUETOOTH",
                                color = SpeedBotColors.CardSlate,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    // Botón para abrir Ajustes Bluetooth nativos de Android
                    OutlinedButton(
                        onClick = onOpenBluetoothSettings,
                        enabled = !isConnecting,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, SpeedBotColors.CyanNeon.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SpeedBotColors.CardSlate.copy(alpha = 0.85f),
                            contentColor = SpeedBotColors.TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                            tint = SpeedBotColors.CyanLight,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AJUSTES BLUETOOTH DE ANDROID",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                    }

                    // Botón Modo Pruebas / Auto Virtual
                    OutlinedButton(
                        onClick = onConnectSimulated,
                        enabled = !isConnecting,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, SpeedBotColors.BorderSlate),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SpeedBotColors.CardSlate.copy(alpha = 0.82f),
                            contentColor = SpeedBotColors.AmberText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Memory,
                            contentDescription = null,
                            tint = SpeedBotColors.AmberNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Modo Pruebas / Auto Virtual (Sin hardware)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Instrucciones HC-05
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SpeedBotColors.CardSlate.copy(alpha = 0.82f))
                        .border(1.dp, SpeedBotColors.BorderSlate, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = SpeedBotColors.CyanLight,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pasos para conectar tu módulo HC-05:",
                            color = SpeedBotColors.TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "1. Si es la primera vez, vincula \"HC-05\" en Ajustes de Android con PIN 1234 o 0000.",
                        color = SpeedBotColors.TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "2. Toca tu HC-05 en la lista de arriba para conectar por Bluetooth Clásico SPP.",
                        color = SpeedBotColors.TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Enlace inferior a la guía Arduino
            TextButton(onClick = onOpenGuide) {
                Text(
                    text = "¿Necesitas el código Arduino Nano para el auto? Ver aquí",
                    color = SpeedBotColors.TextMuted,
                    fontSize = 12.sp,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: BluetoothConnectionStatus) {
    val (label, textColor, bgColor, borderColor) = when (status) {
        BluetoothConnectionStatus.DISCONNECTED -> Quad(
            "DESCONECTADO",
            SpeedBotColors.TextMuted,
            SpeedBotColors.SurfaceSlate,
            SpeedBotColors.BorderMuted
        )
        BluetoothConnectionStatus.SCANNING -> Quad(
            "BUSCANDO...",
            SpeedBotColors.CyanText,
            SpeedBotColors.CyanDark,
            SpeedBotColors.CyanNeon
        )
        BluetoothConnectionStatus.CONNECTING -> Quad(
            "CONECTANDO...",
            SpeedBotColors.AmberText,
            SpeedBotColors.AmberDark,
            SpeedBotColors.AmberNeon
        )
        BluetoothConnectionStatus.CONNECTED -> Quad(
            "CONECTADO",
            SpeedBotColors.EmeraldText,
            SpeedBotColors.EmeraldDark,
            SpeedBotColors.EmeraldNeon
        )
        BluetoothConnectionStatus.ERROR -> Quad(
            "ERROR",
            SpeedBotColors.RedText,
            SpeedBotColors.RedDark,
            SpeedBotColors.RedNeon
        )
    }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bgColor.copy(alpha = 0.85f))
            .border(1.dp, borderColor.copy(alpha = 0.6f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun BluetoothDeviceCard(
    device: BluetoothDeviceUiModel,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val highlight = device.isHcModule
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (highlight) SpeedBotColors.CyanDark.copy(alpha = 0.68f)
                else SpeedBotColors.CardSlate.copy(alpha = 0.85f)
            )
            .border(
                width = if (highlight) 1.5.dp else 1.dp,
                color = if (highlight) SpeedBotColors.CyanLight else SpeedBotColors.BorderSlate,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (highlight) SpeedBotColors.CyanNeon else SpeedBotColors.SurfaceSlate)
                    .padding(9.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Bluetooth,
                    contentDescription = null,
                    tint = if (highlight) SpeedBotColors.CardSlate else SpeedBotColors.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = device.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "MAC: ${device.address} • ${if (device.isBonded) "Vinculado" else "Nuevo"}",
                    color = SpeedBotColors.TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (highlight) SpeedBotColors.EmeraldNeon
                    else SpeedBotColors.SurfaceSlate
                )
                .border(
                    1.dp,
                    if (highlight) SpeedBotColors.EmeraldLight else SpeedBotColors.BorderMuted,
                    RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "CONECTAR",
                color = if (highlight) SpeedBotColors.CardSlate else SpeedBotColors.CyanText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
