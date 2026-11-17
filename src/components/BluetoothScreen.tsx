import { useState, useEffect } from 'react';
import { Bluetooth, RefreshCw, AlertTriangle, CheckCircle2, ArrowLeft, Radio, Cable, ShieldAlert, Cpu } from 'lucide-react';
import { bluetoothManager } from '../services/bluetoothService';
import { BluetoothConnectionStatus, BluetoothDeviceInfo } from '../types';
import { soundEffects } from '../utils/audioFeedback';
import { AppImages } from '../assets/images';

interface BluetoothScreenProps {
  onConnected: (device: BluetoothDeviceInfo) => void;
  onBack: () => void;
  onOpenGuide: () => void;
}

export function BluetoothScreen({ onConnected, onBack, onOpenGuide }: BluetoothScreenProps) {
  const [status, setStatus] = useState<BluetoothConnectionStatus>('disconnected');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [selectedDevice, setSelectedDevice] = useState<string>('HC-05');
  const [isWebBtSupported, setIsWebBtSupported] = useState(true);
  const [isSerialSupported, setIsSerialSupported] = useState(false);

  // Common target devices found or configured
  const knownDevices: BluetoothDeviceInfo[] = [
    { id: 'hc-05-real', name: 'HC-05 (Auto Robótico)', rssi: -62, type: 'ble' },
    { id: 'hc-06-real', name: 'HC-06 SpeedBot', rssi: -71, type: 'ble' },
    { id: 'arduino-bt', name: 'Arduino Nano BT', rssi: -79, type: 'ble' },
  ];

  const [pairedDeviceList, setPairedDeviceList] = useState<any[]>([]);
  const [isAutoConnecting, setIsAutoConnecting] = useState(false);

  // Check for devices already paired in phone/browser on mount & auto-connect
  useEffect(() => {
    setIsWebBtSupported(bluetoothManager.isWebBluetoothAvailable());
    setIsSerialSupported(bluetoothManager.isWebSerialAvailable());

    const checkAndAutoConnect = async () => {
      try {
        const paired = await bluetoothManager.getPairedDevices();
        if (paired && paired.length > 0) {
          setPairedDeviceList(paired);

          // Find candidate (HC-05 or saved device)
          const lastSaved = bluetoothManager.getLastDevice();
          const target = paired.find((d: any) => 
            (lastSaved && d.id === lastSaved.id) ||
            (d.name && (d.name.includes('HC-05') || d.name.includes('HC-') || d.name.includes('SpeedBot')))
          ) || paired[0];

          if (target) {
            setIsAutoConnecting(true);
            setStatus('connecting');
            try {
              const connected = await bluetoothManager.connectExistingDevice(target);
              setStatus('connected');
              soundEffects.playBeep(1200, 0.2);
              setTimeout(() => {
                onConnected(connected);
              }, 400);
              return;
            } catch (autoErr) {
              console.warn('Auto-conexión no inmediata (requiere interacción táctil del usuario):', autoErr);
              setIsAutoConnecting(false);
              setStatus('disconnected');
            }
          }
        }
      } catch {
        // ignore
      }
    };

    checkAndAutoConnect();
  }, [onConnected]);

  // Quick connect to an already paired device without having to scan again
  const handleConnectPairedDevice = async (device: any) => {
    soundEffects.playBeep(920, 0.08);
    setStatus('connecting');
    setErrorMessage(null);

    try {
      const connectedDevice = await bluetoothManager.connectExistingDevice(device);
      setStatus('connected');
      soundEffects.playBeep(1200, 0.2);
      setTimeout(() => {
        onConnected(connectedDevice);
      }, 500);
    } catch (err: any) {
      console.error(err);
      setStatus('error');
      setErrorMessage(err.message || 'No se pudo conectar al dispositivo ya emparejado.');
      soundEffects.playWarning();
    }
  };

  const handleConnectHC05 = async (mode: 'web-bluetooth' | 'serial' | 'simulated') => {
    soundEffects.playBeep(880, 0.08);
    setStatus('connecting');
    setErrorMessage(null);

    try {
      let connectedDevice: BluetoothDeviceInfo;

      if (mode === 'web-bluetooth') {
        connectedDevice = await bluetoothManager.connectWebBluetooth(selectedDevice);
      } else if (mode === 'serial') {
        connectedDevice = await bluetoothManager.connectWebSerial();
      } else {
        connectedDevice = await bluetoothManager.connectSimulated('HC-05 SpeedBot (Test Mode)');
      }

      setStatus('connected');
      soundEffects.playBeep(1200, 0.2);

      // Short delay for user to see the success confirmation state before navigating to control screen
      setTimeout(() => {
        onConnected(connectedDevice);
      }, 600);
    } catch (err: any) {
      console.error(err);
      setStatus('error');
      setErrorMessage(err.message || 'No se pudo conectar al módulo Bluetooth HC-05. Verifica que esté encendido y emparejado en tu celular.');
      soundEffects.playWarning();
    }
  };

  return (
    <div className="relative min-h-[100dvh] w-full flex flex-col justify-between items-center px-4 py-5 overflow-y-auto overflow-x-hidden touch-pan-y select-none">
      {/* 1st Image: Background for Bluetooth Screen (Neon Red/Orange Streaks) */}
      <div className="absolute inset-0 z-0">
        <img
          src={AppImages.bgBluetooth}
          alt="Bluetooth Screen Background"
          referrerPolicy="no-referrer"
          className="w-full h-full object-cover object-center brightness-[0.55] contrast-125"
        />
        <div className="absolute inset-0 bg-slate-950/70 backdrop-blur-[2px]"></div>
        <div className="absolute inset-0 cyber-grid opacity-25"></div>
      </div>

      {/* Top Bar */}
      <div className="w-full max-w-md flex items-center justify-between z-10 pt-1">
        <button
          onClick={() => {
            soundEffects.playBeep(600, 0.08);
            onBack();
          }}
          className="p-2.5 rounded-xl bg-slate-950/80 hover:bg-slate-850 backdrop-blur-md border border-slate-700 text-slate-300 hover:text-white transition flex items-center gap-1.5 text-xs font-semibold cursor-pointer"
          title="Regresar al Menú Principal"
        >
          <ArrowLeft className="w-4 h-4 text-cyan-400" />
          <span>Menú Principal</span>
        </button>
        
        <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-slate-950/80 backdrop-blur-md border border-red-500/30 text-xs text-red-300 font-mono">
          <Radio className="w-3.5 h-3.5 text-red-400 animate-pulse" />
          <span>HC-05 SPP</span>
        </div>
      </div>

      {/* Main Container */}
      <div className="w-full max-w-md my-auto py-2 z-10">
        {/* Title */}
        <div className="text-center mb-5">
          <div className="inline-flex items-center justify-center p-3 rounded-2xl bg-slate-950/80 backdrop-blur-md border border-red-500/40 text-red-400 mb-2 shadow-[0_0_25px_rgba(239,68,68,0.4)]">
            <Bluetooth className="w-8 h-8" />
          </div>
          <h2 className="text-3xl font-black font-heading tracking-wide uppercase text-white drop-shadow-[0_0_15px_rgba(255,255,255,0.3)]">
            Conectar al auto
          </h2>
          <p className="text-xs text-slate-300 mt-1 font-semibold">
            Selecciona tu módulo Bluetooth HC-05 del Arduino
          </p>
        </div>

        {/* Status Indicator */}
        <div className="mb-4 p-4 rounded-2xl bg-slate-950/85 backdrop-blur-md border border-slate-800 shadow-xl">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs uppercase tracking-wider text-slate-400 font-semibold">Estado de Conexión:</span>
            {status === 'disconnected' && (
              <span className="text-xs font-bold text-slate-400 px-2.5 py-0.5 rounded-full bg-slate-900 border border-slate-700">
                DESCONECTADO
              </span>
            )}
            {status === 'connecting' && (
              <span className="text-xs font-bold text-amber-400 px-2.5 py-0.5 rounded-full bg-amber-950/70 border border-amber-500/50 flex items-center gap-1.5 animate-pulse">
                <RefreshCw className="w-3 h-3 animate-spin" /> CONECTANDO...
              </span>
            )}
            {status === 'connected' && (
              <span className="text-xs font-bold text-emerald-400 px-2.5 py-0.5 rounded-full bg-emerald-950/70 border border-emerald-500/50 flex items-center gap-1.5">
                <CheckCircle2 className="w-3 h-3" /> CONECTADO
              </span>
            )}
            {status === 'error' && (
              <span className="text-xs font-bold text-red-400 px-2.5 py-0.5 rounded-full bg-red-950/70 border border-red-500/50 flex items-center gap-1.5">
                <AlertTriangle className="w-3 h-3" /> ERROR
              </span>
            )}
          </div>

          {/* Connection Error Message & Retry */}
          {status === 'error' && errorMessage && (
            <div className="mt-3 p-3 rounded-xl bg-red-950/70 border border-red-500/50 text-xs text-red-200">
              <div className="flex items-start gap-2">
                <AlertTriangle className="w-4 h-4 text-red-400 flex-shrink-0 mt-0.5" />
                <div className="flex-1">
                  <p className="font-bold text-red-300">Fallo de conexión</p>
                  <p className="mt-0.5 text-slate-300">{errorMessage}</p>
                </div>
              </div>
              <div className="mt-3 flex flex-col gap-2">
                {errorMessage.includes('Permissions Policy') || errorMessage.includes('marco integrado') ? (
                  <button
                    onClick={() => {
                      try {
                        const targetUrl = window.location.href;
                        window.open(targetUrl, '_blank');
                      } catch {
                        // ignore
                      }
                    }}
                    className="w-full py-2.5 px-3 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold text-xs tracking-wider uppercase transition flex items-center justify-center gap-1.5 cursor-pointer shadow-md"
                  >
                    <Bluetooth className="w-4 h-4 fill-slate-950" />
                    <span>ABRIR EN PESTAÑA DIRECTA (HABILITA BLUETOOTH)</span>
                  </button>
                ) : null}

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleConnectHC05('web-bluetooth')}
                    className="flex-1 py-2 px-3 rounded-lg bg-red-600 hover:bg-red-500 text-white font-bold text-xs tracking-wider uppercase transition flex items-center justify-center gap-1.5 cursor-pointer"
                  >
                    <RefreshCw className="w-3.5 h-3.5" />
                    REINTENTAR
                  </button>
                  <button
                    onClick={() => handleConnectHC05('simulated')}
                    className="py-2 px-3 rounded-lg bg-slate-900 hover:bg-slate-800 border border-slate-750 text-amber-300 font-bold text-xs tracking-wider uppercase transition flex items-center justify-center gap-1.5 cursor-pointer"
                  >
                    MODO PRUEBA
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Device List & Paired Devices detection */}
        <div className="space-y-2.5 mb-4">
          {/* If device is already paired on the phone */}
          {pairedDeviceList.length > 0 && (
            <div className="p-3.5 rounded-2xl bg-emerald-950/70 border-2 border-emerald-400/80 shadow-[0_0_25px_rgba(16,185,129,0.3)] mb-3">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-bold text-emerald-300 uppercase tracking-wider flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                  <span>Ya vinculado en tu celular:</span>
                </span>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 font-mono font-bold">
                  AUTOCONEXIÓN
                </span>
              </div>
              {pairedDeviceList.map((dev, i) => (
                <button
                  key={dev.id || i}
                  onClick={() => handleConnectPairedDevice(dev)}
                  disabled={status === 'connecting'}
                  className="w-full mt-1.5 py-3 px-3.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 active:scale-95 text-slate-950 font-black font-heading text-sm tracking-wider uppercase transition flex items-center justify-between shadow-md cursor-pointer"
                >
                  <span className="flex items-center gap-2">
                    <Bluetooth className="w-4 h-4 fill-slate-950" />
                    <span>CONECTAR {dev.name || 'HC-05'} AHORA</span>
                  </span>
                  <span className="text-[11px] font-mono font-bold">1 TOQUE</span>
                </button>
              ))}
            </div>
          )}

          <div className="flex items-center justify-between text-xs text-slate-300 px-1">
            <span className="font-semibold tracking-wider uppercase text-cyan-300 flex items-center gap-1.5">
              <Bluetooth className="w-3.5 h-3.5 text-cyan-400" />
              <span>Módulos Bluetooth</span>
            </span>
            <span className="text-[11px] text-cyan-400 font-mono">Baud 9600</span>
          </div>

          {knownDevices.map((dev) => {
            const isSelected = selectedDevice === dev.name.split(' ')[0];
            return (
              <div
                key={dev.id}
                onClick={() => setSelectedDevice(dev.name.split(' ')[0])}
                className={`p-3.5 rounded-2xl border transition-all cursor-pointer flex items-center justify-between ${
                  isSelected
                    ? 'bg-cyan-950/70 border-cyan-400 shadow-[0_0_20px_rgba(6,182,212,0.35)]'
                    : 'bg-slate-950/80 backdrop-blur-md border-slate-800 hover:border-slate-700'
                }`}
              >
                <div className="flex items-center gap-3">
                  <div className={`p-2 rounded-xl ${isSelected ? 'bg-cyan-500 text-slate-950 shadow-md' : 'bg-slate-900 text-slate-400'}`}>
                    <Bluetooth className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="font-bold text-sm text-white tracking-wide flex items-center gap-2">
                      {dev.name}
                      {isSelected && (
                        <span className="text-[10px] px-1.5 py-0.2 rounded bg-cyan-400/20 text-cyan-300 font-mono">
                          SELECCIONADO
                        </span>
                      )}
                    </div>
                    <div className="text-xs text-slate-400 font-mono mt-0.5">
                      PIN: 1234 / 0000
                    </div>
                  </div>
                </div>

                <div className="text-right">
                  <span className="text-xs text-emerald-400 font-mono font-semibold">Listo</span>
                </div>
              </div>
            );
          })}
        </div>

        {/* Primary Action Buttons */}
        <div className="space-y-2.5">
          {/* Real HC-05 Bluetooth scan & connect button via phone native dialog */}
          <button
            id="btn-conectar-hc05"
            onClick={() => handleConnectHC05('web-bluetooth')}
            disabled={status === 'connecting'}
            className="w-full py-4 px-6 rounded-2xl bg-gradient-to-r from-red-500 via-orange-500 to-red-600 hover:from-red-400 hover:to-orange-400 text-slate-950 font-black font-heading text-lg tracking-wider uppercase transition transform active:scale-95 disabled:opacity-50 disabled:pointer-events-none shadow-[0_0_25px_rgba(239,68,68,0.4)] flex flex-col items-center justify-center gap-0.5 cursor-pointer border border-red-300/40"
          >
            {status === 'connecting' ? (
              <div className="flex items-center gap-2">
                <RefreshCw className="w-5 h-5 animate-spin" />
                <span>CONECTANDO A {selectedDevice}...</span>
              </div>
            ) : (
              <>
                <div className="flex items-center gap-2">
                  <Bluetooth className="w-5 h-5 fill-slate-950" />
                  <span>BUSCAR EN EL MENÚ DEL CELULAR</span>
                </div>
                <span className="text-[11px] font-mono font-bold text-slate-900 tracking-normal">
                  Abre la ventana de selección de Android
                </span>
              </>
            )}
          </button>

          {/* Web Serial Port option */}
          {isSerialSupported && (
            <button
              onClick={() => handleConnectHC05('serial')}
              disabled={status === 'connecting'}
              className="w-full py-3 px-4 rounded-xl bg-slate-950/80 hover:bg-slate-900 border border-slate-800 hover:border-cyan-500/40 text-slate-300 text-xs font-bold tracking-wider uppercase transition flex items-center justify-center gap-2 cursor-pointer backdrop-blur-md"
            >
              <Cable className="w-4 h-4 text-cyan-400" />
              <span>Conectar vía Puerto Serial Bluetooth (RFCOMM Android/PC)</span>
            </button>
          )}

          {/* Simulator / Test Mode button */}
          <button
            onClick={() => handleConnectHC05('simulated')}
            disabled={status === 'connecting'}
            className="w-full py-2.5 px-4 rounded-xl bg-slate-950/80 hover:bg-slate-900 border border-slate-800 hover:border-amber-500/40 text-slate-400 hover:text-amber-300 text-xs font-semibold tracking-wider transition flex items-center justify-center gap-2 cursor-pointer backdrop-blur-md"
          >
            <Cpu className="w-3.5 h-3.5 text-amber-400" />
            <span>Modo Pruebas / Auto Virtual (Probar controles sin auto)</span>
          </button>
        </div>

        {/* Pairing notice for HC-05 */}
        <div className="mt-4 p-3 rounded-xl bg-slate-950/80 backdrop-blur-md border border-slate-800/80 text-[11px] text-slate-400 space-y-1">
          <p className="flex items-center gap-1.5 font-semibold text-slate-300">
            <ShieldAlert className="w-3.5 h-3.5 text-cyan-400" />
            Pasos para conectar desde el celular:
          </p>
          <p>
            1. En los <strong>Ajustes de Bluetooth de Android</strong>, vincula "HC-05" con PIN <strong>1234</strong> o <strong>0000</strong>.
          </p>
          <p>
            2. Presiona el botón rojo arriba para abrir el <strong>menú de selección del celular</strong> y toca tu auto.
          </p>
        </div>
      </div>

      {/* Bottom info link */}
      <div className="w-full max-w-md flex justify-center z-10 pt-1 pb-1">
        <button
          onClick={onOpenGuide}
          className="text-xs text-slate-400 hover:text-cyan-400 transition underline underline-offset-4 cursor-pointer"
        >
          ¿Necesitas el código Arduino Nano para el auto? Ver aquí
        </button>
      </div>
    </div>
  );
}
