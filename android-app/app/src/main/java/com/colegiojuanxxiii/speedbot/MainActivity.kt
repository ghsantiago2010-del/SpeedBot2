package com.colegiojuanxxiii.speedbot

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.colegiojuanxxiii.speedbot.audio.SoundEffectsManager
import com.colegiojuanxxiii.speedbot.bluetooth.BluetoothSppManager
import com.colegiojuanxxiii.speedbot.model.AppScreen
import com.colegiojuanxxiii.speedbot.ui.components.ArduinoGuideDialog
import com.colegiojuanxxiii.speedbot.ui.components.PermissionDialog
import com.colegiojuanxxiii.speedbot.ui.screens.BluetoothScreen
import com.colegiojuanxxiii.speedbot.ui.screens.CarControlScreen
import com.colegiojuanxxiii.speedbot.ui.screens.ExitedScreen
import com.colegiojuanxxiii.speedbot.ui.screens.HomeScreen
import com.colegiojuanxxiii.speedbot.ui.theme.SpeedBotTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var bluetoothManager: BluetoothSppManager
    private lateinit var soundEffects: SoundEffectsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        bluetoothManager = BluetoothSppManager(this)
        soundEffects = SoundEffectsManager()

        setContent {
            SpeedBotTheme {
                val scope = rememberCoroutineScope()

                val connectionStatus by bluetoothManager.connectionStatus.collectAsState()
                val connectedDevice by bluetoothManager.connectedDevice.collectAsState()
                val errorMessage by bluetoothManager.errorMessage.collectAsState()
                val isBluetoothSupported by bluetoothManager.isBluetoothSupported.collectAsState()
                val isBluetoothEnabled by bluetoothManager.isBluetoothEnabled.collectAsState()
                val hasPermissions by bluetoothManager.hasPermissions.collectAsState()
                val bondedDevices by bluetoothManager.bondedDevices.collectAsState()
                val discoveredDevices by bluetoothManager.discoveredDevices.collectAsState()

                var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
                var allowAutoConnect by remember { mutableStateOf(true) }
                var showPermissionPrompt by remember {
                    mutableStateOf(!bluetoothManager.isAppStartupPermissionGranted() || !bluetoothManager.checkAllPermissions())
                }
                var isGuideOpen by remember { mutableStateOf(false) }
                var isAppExited by remember { mutableStateOf(false) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    bluetoothManager.updateEnvironmentState()
                }

                val enableBluetoothLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    bluetoothManager.updateEnvironmentState()
                }

                fun requestRuntimePermissions() {
                    permissionLauncher.launch(bluetoothManager.requiredRuntimePermissions())
                }

                fun requestEnableBluetooth() {
                    try {
                        val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        enableBluetoothLauncher.launch(enableIntent)
                    } catch (_: SecurityException) {
                        requestRuntimePermissions()
                    }
                }

                fun openAndroidBluetoothSettings() {
                    startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                }

                if (isAppExited) {
                    ExitedScreen(
                        onRestart = {
                            isAppExited = false
                            currentScreen = AppScreen.HOME
                        },
                        onCloseApp = {
                            finishAffinity()
                        }
                    )
                } else {
                    when (currentScreen) {
                        AppScreen.HOME -> {
                            HomeScreen(
                                onStart = {
                                    soundEffects.playBeep(980.0, 120)
                                    allowAutoConnect = true
                                    bluetoothManager.clearError()
                                    bluetoothManager.updateEnvironmentState()
                                    currentScreen = AppScreen.BLUETOOTH
                                },
                                onExit = {
                                    soundEffects.playWarning()
                                    isAppExited = true
                                },
                                onOpenGuide = {
                                    isGuideOpen = true
                                }
                            )
                        }

                        AppScreen.BLUETOOTH -> {
                            BluetoothScreen(
                                status = connectionStatus,
                                errorMessage = errorMessage,
                                isBluetoothSupported = isBluetoothSupported,
                                isBluetoothEnabled = isBluetoothEnabled,
                                hasPermissions = hasPermissions,
                                bondedDevices = bondedDevices,
                                discoveredDevices = discoveredDevices,
                                allowAutoConnect = allowAutoConnect,
                                onAutoConnectCheck = {
                                    scope.launch {
                                        val result = bluetoothManager.tryAutoConnectIfPaired()
                                        if (result != null && result.isSuccess) {
                                            soundEffects.playBeep(1200.0, 180)
                                            delay(400L)
                                            currentScreen = AppScreen.CONTROL
                                        }
                                    }
                                },
                                onRequestPermissions = { requestRuntimePermissions() },
                                onRequestEnableBluetooth = { requestEnableBluetooth() },
                                onOpenBluetoothSettings = { openAndroidBluetoothSettings() },
                                onStartDiscovery = {
                                    soundEffects.playBeep(880.0, 80)
                                    bluetoothManager.startDiscovery()
                                },
                                onStopDiscovery = {
                                    bluetoothManager.stopDiscovery()
                                },
                                onConnectDevice = { device ->
                                    soundEffects.playBeep(920.0, 80)
                                    scope.launch {
                                        val res = bluetoothManager.connectToDevice(device)
                                        if (res.isSuccess) {
                                            soundEffects.playBeep(1200.0, 180)
                                            delay(450L)
                                            currentScreen = AppScreen.CONTROL
                                        } else {
                                            soundEffects.playWarning()
                                        }
                                    }
                                },
                                onConnectSimulated = {
                                    soundEffects.playBeep(880.0, 80)
                                    scope.launch {
                                        val res = bluetoothManager.connectSimulated()
                                        if (res.isSuccess) {
                                            soundEffects.playBeep(1200.0, 180)
                                            delay(350L)
                                            currentScreen = AppScreen.CONTROL
                                        }
                                    }
                                },
                                onRefreshBonded = {
                                    bluetoothManager.updateEnvironmentState()
                                },
                                onBack = {
                                    soundEffects.playBeep(600.0, 80)
                                    bluetoothManager.stopDiscovery()
                                    currentScreen = AppScreen.HOME
                                },
                                onOpenGuide = {
                                    isGuideOpen = true
                                }
                            )
                        }

                        AppScreen.CONTROL -> {
                            CarControlScreen(
                                device = connectedDevice,
                                connectionStatus = connectionStatus,
                                connectionError = errorMessage,
                                soundEffects = soundEffects,
                                onSendCommand = { cmd ->
                                    bluetoothManager.sendCommand(cmd)
                                },
                                onDisconnect = {
                                    bluetoothManager.disconnect()
                                },
                                onExitToBluetooth = {
                                    allowAutoConnect = false
                                    bluetoothManager.updateEnvironmentState()
                                    currentScreen = AppScreen.BLUETOOTH
                                },
                                onGoToHome = {
                                    currentScreen = AppScreen.HOME
                                },
                                onOpenGuide = {
                                    isGuideOpen = true
                                }
                            )
                        }
                    }

                    if (isGuideOpen) {
                        ArduinoGuideDialog(onDismiss = { isGuideOpen = false })
                    }

                    if (showPermissionPrompt) {
                        PermissionDialog(
                            onAllow = {
                                soundEffects.playBeep(980.0, 120)
                                bluetoothManager.setAppStartupPermissionGranted(true)
                                showPermissionPrompt = false
                                if (!bluetoothManager.checkAllPermissions()) {
                                    requestRuntimePermissions()
                                } else {
                                    bluetoothManager.updateEnvironmentState()
                                }
                            },
                            onDeny = {
                                soundEffects.playWarning()
                                showPermissionPrompt = false
                                isAppExited = true
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::bluetoothManager.isInitialized) {
            bluetoothManager.updateEnvironmentState()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::soundEffects.isInitialized) {
            soundEffects.release()
        }
        if (::bluetoothManager.isInitialized) {
            bluetoothManager.release()
        }
    }
}
