package com.colegiojuanxxiii.speedbot.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.colegiojuanxxiii.speedbot.model.BluetoothConnectionStatus
import com.colegiojuanxxiii.speedbot.model.BluetoothDeviceUiModel
import com.colegiojuanxxiii.speedbot.model.CarCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Gestor nativo de Bluetooth Clásico SPP (RFCOMM) para el módulo HC-05 de Arduino Nano.
 */
class BluetoothSppManager(private val context: Context) {

    companion object {
        // UUID estándar del perfil SPP (Serial Port Profile) para HC-05 / HC-06
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val PREFS_NAME = "speedbot_prefs"
        private const val KEY_LAST_DEVICE_ADDRESS = "last_device_address"
        private const val KEY_LAST_DEVICE_NAME = "last_device_name"
        private const val KEY_APP_PERMISSION_GRANTED = "app_bt_permission_granted"
    }

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = appContext.getSystemService(BluetoothManager::class.java)
        manager?.adapter
    }

    private var activeSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var writerJob: Job? = null
    private var monitorJob: Job? = null
    private var commandChannel = Channel<String>(Channel.UNLIMITED)

    private var isSimulated = false
    private var connectedMacAddress: String? = null
    private var isReceiverRegistered = false

    private val _connectionStatus = MutableStateFlow(BluetoothConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<BluetoothConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BluetoothDeviceUiModel?>(null)
    val connectedDevice: StateFlow<BluetoothDeviceUiModel?> = _connectedDevice.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _bondedDevices = MutableStateFlow<List<BluetoothDeviceUiModel>>(emptyList())
    val bondedDevices: StateFlow<List<BluetoothDeviceUiModel>> = _bondedDevices.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceUiModel>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDeviceUiModel>> = _discoveredDevices.asStateFlow()

    private val _isBluetoothSupported = MutableStateFlow(bluetoothAdapter != null)
    val isBluetoothSupported: StateFlow<Boolean> = _isBluetoothSupported.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _hasPermissions = MutableStateFlow(checkAllPermissions())
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    val enabled = state == BluetoothAdapter.STATE_ON
                    _isBluetoothEnabled.value = enabled
                    if (!enabled && _connectionStatus.value == BluetoothConnectionStatus.CONNECTED && !isSimulated) {
                        scope.launch {
                            handleRemoteDisconnect("El Bluetooth del celular se apagó.")
                        }
                    } else if (enabled) {
                        refreshBondedDevices()
                    }
                }

                BluetoothDevice.ACTION_FOUND -> {
                    if (!hasConnectPermission()) return
                    val device = extractBluetoothDevice(intent) ?: return
                    val address = device.address ?: return
                    val name = try {
                        device.name ?: "Dispositivo ($address)"
                    } catch (_: SecurityException) {
                        "Dispositivo ($address)"
                    }
                    val isBonded = try {
                        device.bondState == BluetoothDevice.BOND_BONDED
                    } catch (_: SecurityException) {
                        false
                    }

                    val model = BluetoothDeviceUiModel(
                        name = name,
                        address = address,
                        isBonded = isBonded
                    )

                    val current = _discoveredDevices.value.toMutableList()
                    if (current.none { it.address == address }) {
                        current.add(model)
                        _discoveredDevices.value = sortHcFirst(current)
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    if (_connectionStatus.value == BluetoothConnectionStatus.SCANNING) {
                        _connectionStatus.value = BluetoothConnectionStatus.DISCONNECTED
                    }
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    refreshBondedDevices()
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = extractBluetoothDevice(intent)
                    if (!isSimulated && device != null && device.address == connectedMacAddress) {
                        scope.launch {
                            handleRemoteDisconnect("Se perdió la conexión con ${device.name ?: "el módulo HC-05"}.")
                        }
                    }
                }
            }
        }
    }

    init {
        registerReceiver()
        updateEnvironmentState()
    }

    private fun extractBluetoothDevice(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }

    private fun registerReceiver() {
        if (isReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            appContext.registerReceiver(bluetoothReceiver, filter)
        }
        isReceiverRegistered = true
    }

    fun requiredRuntimePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    fun checkAllPermissions(): Boolean {
        return requiredRuntimePermissions().all { perm ->
            ContextCompat.checkSelfPermission(appContext, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isAppStartupPermissionGranted(): Boolean {
        return prefs.getBoolean(KEY_APP_PERMISSION_GRANTED, false)
    }

    fun setAppStartupPermissionGranted(granted: Boolean) {
        prefs.edit().putBoolean(KEY_APP_PERMISSION_GRANTED, granted).apply()
    }

    fun updateEnvironmentState() {
        val adapter = bluetoothAdapter
        _isBluetoothSupported.value = adapter != null
        _isBluetoothEnabled.value = adapter?.isEnabled == true
        _hasPermissions.value = checkAllPermissions()
        refreshBondedDevices()
    }

    @SuppressLint("MissingPermission")
    fun refreshBondedDevices() {
        val adapter = bluetoothAdapter
        if (adapter == null || !hasConnectPermission()) {
            _bondedDevices.value = emptyList()
            return
        }

        val paired = try {
            adapter.bondedDevices?.map { dev ->
                BluetoothDeviceUiModel(
                    name = dev.name ?: "Dispositivo (${dev.address})",
                    address = dev.address,
                    isBonded = true
                )
            } ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }

        _bondedDevices.value = sortHcFirst(paired)
    }

    private fun sortHcFirst(list: List<BluetoothDeviceUiModel>): List<BluetoothDeviceUiModel> {
        val lastAddress = getLastSavedAddress()
        return list.sortedWith(
            compareByDescending<BluetoothDeviceUiModel> { it.address == lastAddress }
                .thenByDescending { it.isHcModule }
                .thenByDescending { it.isBonded }
                .thenBy { it.name.lowercase() }
        )
    }

    fun getLastSavedAddress(): String? = prefs.getString(KEY_LAST_DEVICE_ADDRESS, null)

    fun getLastSavedName(): String? = prefs.getString(KEY_LAST_DEVICE_NAME, null)

    private fun saveLastDevice(device: BluetoothDeviceUiModel) {
        prefs.edit()
            .putString(KEY_LAST_DEVICE_ADDRESS, device.address)
            .putString(KEY_LAST_DEVICE_NAME, device.name)
            .apply()
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        updateEnvironmentState()
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _errorMessage.value = "Este dispositivo no cuenta con adaptador Bluetooth."
            _connectionStatus.value = BluetoothConnectionStatus.ERROR
            return
        }
        if (!hasScanPermission() || !hasConnectPermission()) {
            _errorMessage.value = "Debes aceptar los permisos de Bluetooth para buscar módulos cercanos."
            _connectionStatus.value = BluetoothConnectionStatus.ERROR
            return
        }
        if (!adapter.isEnabled) {
            _errorMessage.value = "El Bluetooth del celular está apagado. Enciéndelo para buscar tu auto."
            _connectionStatus.value = BluetoothConnectionStatus.ERROR
            return
        }

        _errorMessage.value = null
        _discoveredDevices.value = emptyList()
        _connectionStatus.value = BluetoothConnectionStatus.SCANNING

        try {
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            val started = adapter.startDiscovery()
            if (!started) {
                _connectionStatus.value = BluetoothConnectionStatus.DISCONNECTED
                _errorMessage.value = "No se pudo iniciar el escaneo Bluetooth. Verifica que el GPS/Ubicación esté activo si usas Android 11 o menor."
                _connectionStatus.value = BluetoothConnectionStatus.ERROR
            }
        } catch (e: SecurityException) {
            _connectionStatus.value = BluetoothConnectionStatus.ERROR
            _errorMessage.value = "Sin permiso de escaneo Bluetooth: ${e.localizedMessage ?: "revisa los ajustes"}"
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        val adapter = bluetoothAdapter ?: return
        if (!hasScanPermission()) return
        try {
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
        } catch (_: SecurityException) {
        }
        if (_connectionStatus.value == BluetoothConnectionStatus.SCANNING) {
            _connectionStatus.value = BluetoothConnectionStatus.DISCONNECTED
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(deviceUi: BluetoothDeviceUiModel): Result<BluetoothDeviceUiModel> =
        withContext(Dispatchers.IO) {
            val adapter = bluetoothAdapter
                ?: return@withContext failWithError("Este dispositivo no tiene soporte para Bluetooth.")

            if (!hasConnectPermission()) {
                return@withContext failWithError("Faltan permisos de Bluetooth. Toca 'Otorgar permisos' para continuar.")
            }
            if (!adapter.isEnabled) {
                return@withContext failWithError("El Bluetooth está apagado. Actívalo primero en tu celular.")
            }

            _errorMessage.value = null
            _connectionStatus.value = BluetoothConnectionStatus.CONNECTING

            // Cancelar cualquier escaneo activo antes de abrir el socket RFCOMM
            if (hasScanPermission()) {
                try {
                    if (adapter.isDiscovering) {
                        adapter.cancelDiscovery()
                    }
                } catch (_: SecurityException) {
                }
            }

            // Cerrar conexiones previas si hubiera
            closeSocketSilently()

            val remoteDevice = try {
                adapter.getRemoteDevice(deviceUi.address)
            } catch (_: IllegalArgumentException) {
                return@withContext failWithError("Dirección MAC Bluetooth inválida: ${deviceUi.address}")
            }

            var socket: BluetoothSocket? = null
            try {
                socket = remoteDevice.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
            } catch (primaryEx: IOException) {
                try {
                    socket?.close()
                } catch (_: IOException) {
                }

                // Fallback RFCOMM canal 1 para compatibilidad amplia con módulos HC-05 en distintos fabricantes
                try {
                    @Suppress("DiscouragedPrivateApi")
                    val method = remoteDevice.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    socket = method.invoke(remoteDevice, 1) as BluetoothSocket
                    socket.connect()
                } catch (fallbackEx: Exception) {
                    try {
                        socket?.close()
                    } catch (_: IOException) {
                    }
                    val detail = fallbackEx.message ?: primaryEx.message ?: "tiempo de espera agotado"
                    return@withContext failWithError(
                        "No se pudo conectar con ${deviceUi.name}. Verifica que el auto esté encendido, cerca del celular y vinculado con PIN 1234 o 0000. ($detail)"
                    )
                }
            } catch (secEx: SecurityException) {
                return@withContext failWithError("Permiso Bluetooth denegado al intentar conectar: ${secEx.localizedMessage ?: ""}")
            }

            return@withContext try {
                activeSocket = socket
                outputStream = socket!!.outputStream
                inputStream = socket.inputStream
                isSimulated = false
                connectedMacAddress = deviceUi.address

                saveLastDevice(deviceUi)
                startWriterLoop()
                startConnectionMonitor(deviceUi.name)

                _connectedDevice.value = deviceUi
                _connectionStatus.value = BluetoothConnectionStatus.CONNECTED
                refreshBondedDevices()
                Result.success(deviceUi)
            } catch (ioEx: IOException) {
                closeSocketSilently()
                failWithError("Se abrió el enlace pero falló el canal serial SPP: ${ioEx.localizedMessage ?: "error de E/S"}")
            }
        }

    /**
     * Intenta conectar automáticamente al último HC-05 utilizado (o al primer HC-05 ya vinculado).
     */
    suspend fun tryAutoConnectIfPaired(): Result<BluetoothDeviceUiModel>? {
        updateEnvironmentState()
        if (_connectionStatus.value == BluetoothConnectionStatus.CONNECTED) {
            return _connectedDevice.value?.let { Result.success(it) }
        }
        if (!_isBluetoothEnabled.value || !_hasPermissions.value) return null

        val bonded = _bondedDevices.value
        if (bonded.isEmpty()) return null

        val lastAddr = getLastSavedAddress()
        val candidate = bonded.firstOrNull { it.address == lastAddr }
            ?: bonded.firstOrNull { it.isHcModule }
            ?: return null

        return connectToDevice(candidate)
    }

    suspend fun connectSimulated(deviceName: String = "HC-05 SpeedBot (Modo Virtual)"): Result<BluetoothDeviceUiModel> =
        withContext(Dispatchers.IO) {
            _errorMessage.value = null
            _connectionStatus.value = BluetoothConnectionStatus.CONNECTING
            closeSocketSilently()
            delay(450)
            isSimulated = true
            connectedMacAddress = "SIM-HC05"
            val model = BluetoothDeviceUiModel(
                name = deviceName,
                address = "SIMULADO",
                isBonded = false,
                isSimulated = true
            )
            _connectedDevice.value = model
            _connectionStatus.value = BluetoothConnectionStatus.CONNECTED
            Result.success(model)
        }

    private fun startWriterLoop() {
        writerJob?.cancel()
        commandChannel.close()
        commandChannel = Channel(Channel.UNLIMITED)

        writerJob = scope.launch {
            for (cmd in commandChannel) {
                if (isSimulated) continue
                val stream = outputStream ?: continue
                try {
                    stream.write(cmd.toByteArray(Charsets.US_ASCII))
                    stream.flush()
                } catch (_: IOException) {
                    handleRemoteDisconnect("Se interrumpió el envío de datos al módulo HC-05.")
                    break
                }
            }
        }
    }

    private fun startConnectionMonitor(deviceName: String) {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            val buffer = ByteArray(64)
            while (isActive && !isSimulated) {
                val stream = inputStream ?: break
                try {
                    val bytesRead = stream.read(buffer)
                    if (bytesRead == -1) {
                        handleRemoteDisconnect("El módulo $deviceName cerró la conexión.")
                        break
                    }
                } catch (_: IOException) {
                    if (activeSocket != null) {
                        handleRemoteDisconnect("Se perdió la señal con $deviceName.")
                    }
                    break
                }
            }
        }
    }

    fun sendCommand(command: CarCommand) {
        sendRaw(command.code)
    }

    fun sendRaw(command: String) {
        if (_connectionStatus.value != BluetoothConnectionStatus.CONNECTED) return
        if (isSimulated) return
        commandChannel.trySend(command)
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        // Enviar freno de emergencia 'S' antes de desconectar
        if (!isSimulated && outputStream != null) {
            try {
                outputStream?.write("S".toByteArray(Charsets.US_ASCII))
                outputStream?.flush()
            } catch (_: IOException) {
            }
        }

        closeSocketSilently()
        isSimulated = false
        connectedMacAddress = null
        _connectedDevice.value = null
        _errorMessage.value = null
        _connectionStatus.value = BluetoothConnectionStatus.DISCONNECTED
    }

    private suspend fun handleRemoteDisconnect(reason: String) {
        withContext(Dispatchers.IO) {
            closeSocketSilently()
            isSimulated = false
            connectedMacAddress = null
            _connectedDevice.value = null
            _errorMessage.value = reason
            _connectionStatus.value = BluetoothConnectionStatus.ERROR
        }
    }

    private fun closeSocketSilently() {
        writerJob?.cancel()
        writerJob = null
        monitorJob?.cancel()
        monitorJob = null

        try {
            outputStream?.close()
        } catch (_: IOException) {
        }
        outputStream = null

        try {
            inputStream?.close()
        } catch (_: IOException) {
        }
        inputStream = null

        try {
            activeSocket?.close()
        } catch (_: IOException) {
        }
        activeSocket = null
    }

    private fun failWithError(message: String): Result<BluetoothDeviceUiModel> {
        _errorMessage.value = message
        _connectionStatus.value = BluetoothConnectionStatus.ERROR
        return Result.failure(IOException(message))
    }

    fun clearError() {
        _errorMessage.value = null
        if (_connectionStatus.value == BluetoothConnectionStatus.ERROR) {
            _connectionStatus.value = BluetoothConnectionStatus.DISCONNECTED
        }
    }

    fun release() {
        if (isReceiverRegistered) {
            try {
                appContext.unregisterReceiver(bluetoothReceiver)
            } catch (_: IllegalArgumentException) {
            }
            isReceiverRegistered = false
        }
        closeSocketSilently()
        commandChannel.close()
        scope.cancel()
    }
}
