export type AppScreen = 'permission' | 'home' | 'bluetooth' | 'control';

export type BluetoothConnectionStatus = 
  | 'disconnected' 
  | 'scanning' 
  | 'connecting' 
  | 'connected' 
  | 'error';

// Standard + diagonal + speed + auxiliary commands
export type CarCommand = 
  | 'F' // Adelante
  | 'B' // Atras
  | 'L' // Izquierda
  | 'R' // Derecha
  | 'G' // Adelante-Izquierda (135°)
  | 'I' // Adelante-Derecha (45°)
  | 'H' // Atras-Izquierda (225°)
  | 'J' // Atras-Derecha (315°)
  | 'S' // Detener / Freno
  | 'Z' // Bocina (Horn)
  | 'E' // LED ON
  | 'D'; // LED OFF

export interface JoystickData {
  angle: number; // 0 to 359 degrees (0 = East/Right, 90 = North/Up, 180 = West/Left, 270 = South/Down)
  throttle: number; // 0 to 100% (pedal intensity)
  normalizedX: number; // -1.0 to 1.0
  normalizedY: number; // -1.0 to 1.0
  command: CarCommand;
  speedPwm: number; // 0 to 255 PWM for Arduino analogWrite
  isBraking: boolean;
}

export interface BluetoothDeviceInfo {
  id: string;
  name: string;
  rssi?: number;
  type: 'ble' | 'serial' | 'simulated';
}

export interface CommandLog {
  id: string;
  command: CarCommand | string;
  description: string;
  timestamp: string;
}
