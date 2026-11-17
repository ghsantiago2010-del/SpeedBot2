import { CarCommand, BluetoothDeviceInfo } from '../types';

export class BluetoothService {
  private device: any = null;
  private gattServer: any = null;
  private txCharacteristic: any = null;
  private serialPort: any = null;
  private serialWriter: any = null;
  private isSimulated = false;
  private onDisconnectCallback: (() => void) | null = null;

  // Well known Bluetooth UART and Serial Service UUIDs
  private readonly SERVICE_UUIDS = [
    '0000ffe0-0000-1000-8000-00805f9b34fb', // HC-05 / HM-10 / CC2541 BLE service
    '6e400001-b5a3-f393-e0a9-e50e24dcca9e', // Nordic UART Service (NUS)
    '49535343-fe7d-4ae5-8fa9-9fafd205e455', // ISSC transparent UART
    '00001101-0000-1000-8000-00805f9b34fb', // Standard Serial Port Profile (SPP)
  ];

  private readonly CHAR_UUIDS = [
    '0000ffe1-0000-1000-8000-00805f9b34fb', // HC-05 / HM-10 TX/RX characteristic
    '6e400002-b5a3-f393-e0a9-e50e24dcca9e', // Nordic TX
    '49535343-1e4d-4bd9-ba61-23c647249616', // ISSC TX
  ];

  public isWebBluetoothAvailable(): boolean {
    return typeof navigator !== 'undefined' && 'bluetooth' in navigator;
  }

  public isWebSerialAvailable(): boolean {
    return typeof navigator !== 'undefined' && 'serial' in (navigator as any);
  }

  public setOnDisconnect(callback: () => void) {
    this.onDisconnectCallback = callback;
  }

  /**
   * Check for previously paired/authorized Bluetooth devices without showing a new picker
   */
  public async getPairedDevices(): Promise<any[]> {
    if (!this.isWebBluetoothAvailable()) return [];
    try {
      const bluetooth = (navigator as any).bluetooth;
      if (typeof bluetooth.getDevices === 'function') {
        const devices = await bluetooth.getDevices();
        return devices || [];
      }
    } catch {
      // not supported in some browsers
    }
    return [];
  }

  /**
   * Connect to an already paired device object directly without opening picker
   */
  public async connectExistingDevice(device: any): Promise<BluetoothDeviceInfo> {
    try {
      this.device = device;
      this.isSimulated = false;

      device.addEventListener('gattserverdisconnected', () => {
        this.handleDisconnected();
      });

      const server = await device.gatt.connect();
      this.gattServer = server;

      await this.discoverCharacteristics(server);

      // Save last connected device info
      this.saveLastDevice(device.id, device.name || 'HC-05 Robot Car');

      return {
        id: device.id,
        name: device.name || 'HC-05 Robot Car',
        type: 'ble',
      };
    } catch (err: any) {
      throw new Error(err.message || 'Error al reconectar al dispositivo ya vinculado.');
    }
  }

  private saveLastDevice(id: string, name: string) {
    try {
      localStorage.setItem('speedbot_last_device', JSON.stringify({ id, name, timestamp: Date.now() }));
    } catch {
      // ignore
    }
  }

  public getLastDevice(): { id: string; name: string } | null {
    try {
      const data = localStorage.getItem('speedbot_last_device');
      if (data) return JSON.parse(data);
    } catch {
      // ignore
    }
    return null;
  }

  /**
   * Discovers characteristics on the GATT server
   */
  private async discoverCharacteristics(server: any) {
    let targetCharacteristic: any = null;

    for (const serviceUuid of this.SERVICE_UUIDS) {
      try {
        const service = await server.getPrimaryService(serviceUuid);
        const characteristics = await service.getCharacteristics();
        for (const char of characteristics) {
          if (char.properties.write || char.properties.writeWithoutResponse) {
            targetCharacteristic = char;
            break;
          }
        }
        if (targetCharacteristic) break;
      } catch {
        // continue
      }
    }

    if (!targetCharacteristic) {
      try {
        const services = await server.getPrimaryServices();
        for (const service of services) {
          const characteristics = await service.getCharacteristics();
          for (const char of characteristics) {
            if (char.properties.write || char.properties.writeWithoutResponse) {
              targetCharacteristic = char;
              break;
            }
          }
          if (targetCharacteristic) break;
        }
      } catch {
        // fallback
      }
    }

    this.txCharacteristic = targetCharacteristic;
  }

  /**
   * Connect to real HC-05 via Web Bluetooth API (GATT / BLE)
   * This calls navigator.bluetooth.requestDevice which triggers the native system/cellphone Bluetooth picker menu
   */
  public async connectWebBluetooth(preferredDeviceName?: string): Promise<BluetoothDeviceInfo> {
    if (!this.isWebBluetoothAvailable()) {
      throw new Error('Tu navegador no tiene activado Web Bluetooth. Usa Google Chrome en tu celular Android.');
    }

    try {
      const bluetooth = (navigator as any).bluetooth;

      // 1. If device is already paired in phone, try to connect without prompt
      if (typeof bluetooth.getDevices === 'function') {
        try {
          const paired = await bluetooth.getDevices();
          if (paired && paired.length > 0) {
            const match = paired.find((d: any) => 
              (preferredDeviceName && d.name && d.name.toLowerCase().includes(preferredDeviceName.toLowerCase())) ||
              (d.name && (d.name.includes('HC-') || d.name.includes('SpeedBot') || d.name.includes('Arduino') || d.name.includes('BT')))
            ) || paired[0];

            if (match) {
              return await this.connectExistingDevice(match);
            }
          }
        } catch {
          // fallback to picker
        }
      }

      // Triggers the phone's native Bluetooth picker dialog (menú de Bluetooth del celular)
      const device = await bluetooth.requestDevice({
        acceptAllDevices: true,
        optionalServices: this.SERVICE_UUIDS,
      });

      return await this.connectExistingDevice(device);
    } catch (err: any) {
      if (err.name === 'NotFoundError') {
        throw new Error('Cancelaste la búsqueda o no seleccionaste ningún dispositivo del menú Bluetooth de tu celular.');
      }
      if (err.name === 'SecurityError' || (err.message && err.message.toLowerCase().includes('permissions policy'))) {
        throw new Error('El navegador tiene bloqueado el Bluetooth en la vista previa del marco. Toca el botón para abrir en pestaña completa del celular.');
      }
      if (err.name === 'NotSupportedError') {
        throw new Error('El Bluetooth no es compatible o está apagado en tu celular. Enciende el Bluetooth de Android.');
      }
      if (err.message && err.message.includes('User cancelled')) {
        throw new Error('Selección cancelada en el menú del celular.');
      }
      throw new Error(err.message || 'Error al conectar con el módulo Bluetooth HC-05. Comprueba que el auto esté encendido.');
    }
  }

  /**
   * Connect to real HC-05 paired via Bluetooth Serial (Web Serial API)
   */
  public async connectWebSerial(): Promise<BluetoothDeviceInfo> {
    if (!this.isWebSerialAvailable()) {
      throw new Error('El puerto serial Bluetooth no está disponible en este navegador.');
    }

    try {
      const serial = (navigator as any).serial;
      const port = await serial.requestPort();
      await port.open({ baudRate: 9600 });

      this.serialPort = port;
      this.serialWriter = port.writable.getWriter();
      this.isSimulated = false;

      return {
        id: 'serial-hc05',
        name: 'HC-05 (Puerto Serial Bluetooth)',
        type: 'serial',
      };
    } catch (err: any) {
      if (err.name === 'NotFoundError') {
        throw new Error('No se seleccionó ningún puerto serial desde el menú del dispositivo.');
      }
      throw new Error(err.message || 'Error al conectar mediante el puerto serial Bluetooth.');
    }
  }

  /**
   * Connect in virtual / simulation mode (for testing without physical Arduino hardware)
   */
  public async connectSimulated(deviceName = 'HC-05 (Simulado)'): Promise<BluetoothDeviceInfo> {
    await new Promise((resolve) => setTimeout(resolve, 800));
    this.isSimulated = true;
    return {
      id: 'sim-hc05-nano',
      name: deviceName,
      type: 'simulated',
    };
  }

  /**
   * Send a single-character command or telemetry packet to HC-05
   */
  public async sendCommand(command: CarCommand | string): Promise<boolean> {
    if (this.isSimulated) {
      // In simulation mode, command is handled virtually
      return true;
    }

    const encoder = new TextEncoder();
    const data = encoder.encode(command);

    try {
      // 1. If connected via Web Serial
      if (this.serialWriter) {
        await this.serialWriter.write(data);
        return true;
      }

      // 2. If connected via Web Bluetooth GATT
      if (this.txCharacteristic) {
        if (this.txCharacteristic.properties.writeWithoutResponse) {
          await this.txCharacteristic.writeValueWithoutResponse(data);
        } else {
          await this.txCharacteristic.writeValueWithResponse(data);
        }
        return true;
      }

      return false;
    } catch (error) {
      console.error('Error al enviar comando Bluetooth:', error);
      return false;
    }
  }

  /**
   * Disconnect from Bluetooth
   */
  public async disconnect(): Promise<void> {
    try {
      // Always attempt to send STOP command before cutting connection
      await this.sendCommand('S');
    } catch {
      // ignore
    }

    try {
      if (this.serialWriter) {
        await this.serialWriter.close();
        this.serialWriter = null;
      }
      if (this.serialPort) {
        await this.serialPort.close();
        this.serialPort = null;
      }
    } catch {
      // ignore
    }

    try {
      if (this.gattServer && this.gattServer.connected) {
        this.gattServer.disconnect();
      }
      this.gattServer = null;
      this.txCharacteristic = null;
      this.device = null;
    } catch {
      // ignore
    }

    this.isSimulated = false;
    this.handleDisconnected();
  }

  public isConnected(): boolean {
    if (this.isSimulated) return true;
    if (this.serialPort && this.serialWriter) return true;
    if (this.gattServer && this.gattServer.connected) return true;
    return false;
  }

  private handleDisconnected() {
    if (this.onDisconnectCallback) {
      this.onDisconnectCallback();
    }
  }
}

export const bluetoothManager = new BluetoothService();
