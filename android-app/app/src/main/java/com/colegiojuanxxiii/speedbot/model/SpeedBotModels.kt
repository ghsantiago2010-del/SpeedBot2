package com.colegiojuanxxiii.speedbot.model

enum class AppScreen {
    HOME,
    BLUETOOTH,
    CONTROL
}

enum class BluetoothConnectionStatus {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR
}

enum class CarCommand(val code: String, val description: String) {
    F("F", "Adelante (90°)"),
    B("B", "Atrás (270°)"),
    L("L", "Izquierda (180°)"),
    R("R", "Derecha (0°)"),
    G("G", "Diagonal 135° (Adelante-Izq)"),
    I("I", "Diagonal 45° (Adelante-Der)"),
    H("H", "Diagonal 225° (Atrás-Izq)"),
    J("J", "Diagonal 315° (Atrás-Der)"),
    S("S", "Detenido (Freno Activo)"),
    Z("Z", "Bocina Activa"),
    E("E", "Luces LED Encendidas"),
    D("D", "Luces LED Apagadas")
}

data class JoystickData(
    val angle: Int = 0,
    val throttle: Int = 0,
    val normalizedX: Float = 0f,
    val normalizedY: Float = 0f,
    val command: CarCommand = CarCommand.S,
    val speedPwm: Int = 0,
    val isBraking: Boolean = true
)

data class BluetoothDeviceUiModel(
    val name: String,
    val address: String,
    val isBonded: Boolean = false,
    val isSimulated: Boolean = false
) {
    val isHcModule: Boolean
        get() = name.contains("HC-", ignoreCase = true) ||
            name.contains("SpeedBot", ignoreCase = true) ||
            name.contains("Arduino", ignoreCase = true) ||
            name.contains("BT", ignoreCase = true)
}
