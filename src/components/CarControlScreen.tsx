import { useState, useEffect, useRef, useCallback } from 'react';
import { Power, Volume2, Lightbulb, Clock, Radio, ShieldAlert, Gauge, Compass, Play, Pause, RotateCcw, Plus, LayoutGrid, Home, Bluetooth, BookOpen, X } from 'lucide-react';
import { AnalogJoystick } from './AnalogJoystick';
import { bluetoothManager } from '../services/bluetoothService';
import { CarCommand, BluetoothDeviceInfo, JoystickData } from '../types';
import { soundEffects } from '../utils/audioFeedback';
import { AppImages } from '../assets/images';

interface CarControlScreenProps {
  device: BluetoothDeviceInfo | null;
  onExitToBluetooth: () => void;
  onGoToHome?: () => void;
  onOpenGuide?: () => void;
}

export function CarControlScreen({ device, onExitToBluetooth, onGoToHome, onOpenGuide }: CarControlScreenProps) {
  // 2-minute countdown (120 seconds default, strict maximum 120s)
  const MAX_SESSION_SECONDS = 120;
  const [secondsRemaining, setSecondsRemaining] = useState<number>(MAX_SESSION_SECONDS);
  const [isTimeExpired, setIsTimeExpired] = useState<boolean>(false);
  const [controlsDisabled, setControlsDisabled] = useState<boolean>(false);
  const [isTimerPaused, setIsTimerPaused] = useState<boolean>(false);
  const [showNavModal, setShowNavModal] = useState<boolean>(false);

  // LED toggle state: false = OFF, true = ON
  const [isLedOn, setIsLedOn] = useState<boolean>(false);

  // Active command & joystick telemetry tracking
  const [lastCommand, setLastCommand] = useState<CarCommand>('S');
  const [hornActive, setHornActive] = useState<boolean>(false);
  const [currentThrottle, setCurrentThrottle] = useState<number>(0);
  const [currentAngle, setCurrentAngle] = useState<number>(0);
  const [isBrakingState, setIsBrakingState] = useState<boolean>(true);

  const timerRef = useRef<NodeJS.Timeout | null>(null);
  const hornIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const hasExpiredHandled = useRef<boolean>(false);
  const lastSentTime = useRef<number>(0);
  const prevCommandRef = useRef<string>('S');

  // Run the countdown timer when not paused
  useEffect(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }

    if (!isTimerPaused && !isTimeExpired && secondsRemaining > 0) {
      timerRef.current = setInterval(() => {
        setSecondsRemaining((prev) => {
          if (prev <= 1) {
            if (timerRef.current) clearInterval(timerRef.current);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    }

    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
      }
    };
  }, [isTimerPaused, isTimeExpired, secondsRemaining]);

  // Handle when timer reaches 00:00
  const handleTimeExpired = useCallback(async () => {
    if (hasExpiredHandled.current) return;
    hasExpiredHandled.current = true;

    // 1. Immediately send STOP command "S" to Arduino
    // 2. Stop the car
    await bluetoothManager.sendCommand('S');
    setLastCommand('S');
    setCurrentThrottle(0);
    setIsBrakingState(true);

    // 3. Disable movement controls
    setControlsDisabled(true);

    // 4. Show clear message "TIEMPO AGOTADO"
    setIsTimeExpired(true);
    soundEffects.playWarning();

    // 5. Automatically disconnect from HC-05
    await bluetoothManager.disconnect();

    // 6. After showing the message, return to Bluetooth screen ("conectar el auto")
    setTimeout(() => {
      onExitToBluetooth();
    }, 2800);
  }, [onExitToBluetooth]);

  // Monitor timer for reaching 00:00
  useEffect(() => {
    if (secondsRemaining === 0 && !hasExpiredHandled.current) {
      handleTimeExpired();
    }
  }, [secondsRemaining, handleTimeExpired]);

  // Open Navigation modal to choose which tab/screen to go to
  const handleOpenNavModal = () => {
    soundEffects.playBeep(600, 0.08);
    setShowNavModal(true);
  };

  // Perform navigation to selected screen safely stopping car & disconnecting if exiting
  const handleNavigateTo = async (destination: 'home' | 'bluetooth' | 'guide') => {
    soundEffects.playBeep(700, 0.08);

    if (destination === 'guide') {
      setShowNavModal(false);
      onOpenGuide?.();
      return;
    }

    // 1. Cancel timer
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }

    // 2. Stop the car (send 'S')
    await bluetoothManager.sendCommand('S');
    setLastCommand('S');

    // 3. Disconnect from Bluetooth
    await bluetoothManager.disconnect();
    setShowNavModal(false);

    // 4. Navigate to selected screen
    if (destination === 'home') {
      onGoToHome?.();
    } else {
      onExitToBluetooth();
    }
  };

  // Toggle Pause/Resume timer
  const handleToggleTimerPause = () => {
    soundEffects.playBeep(isTimerPaused ? 880 : 440, 0.08);
    setIsTimerPaused((prev) => !prev);
  };

  // Reset timer to exactly 2 minutes
  const handleResetTimer = () => {
    soundEffects.playBeep(980, 0.1);
    setSecondsRemaining(MAX_SESSION_SECONDS);
    setIsTimeExpired(false);
    setControlsDisabled(false);
    setIsTimerPaused(false);
    hasExpiredHandled.current = false;
  };

  // Add 30 seconds with STRICT limit of 2 minutes (120 seconds maximum, cannot exceed 2 minutes)
  const handleAdd30Seconds = () => {
    if (secondsRemaining >= MAX_SESSION_SECONDS) {
      soundEffects.playWarning();
      return;
    }
    soundEffects.playBeep(1020, 0.08);
    setSecondsRemaining((prev) => Math.min(MAX_SESSION_SECONDS, prev + 30));
    if (isTimeExpired) {
      setIsTimeExpired(false);
      setControlsDisabled(false);
      hasExpiredHandled.current = false;
    }
  };

  // Joystick telemetry change handler
  const handleJoystickChange = async (data: JoystickData) => {
    if (controlsDisabled || isTimeExpired) return;

    setCurrentThrottle(data.throttle);
    setCurrentAngle(data.angle);
    setIsBrakingState(data.isBraking || data.throttle === 0);

    const now = Date.now();

    // When releasing joystick ("cuando lo dejas de apretar tome como q frena")
    if (data.isBraking || data.throttle === 0) {
      if (prevCommandRef.current !== 'S') {
        prevCommandRef.current = 'S';
        setLastCommand('S');
        await bluetoothManager.sendCommand('S');
        soundEffects.playBeep(260, 0.08); // Brake acoustic feedback
      }
      return;
    }

    // Rate-limit continuous transmission over Bluetooth (every 85ms) or on command change
    const commandChanged = data.command !== prevCommandRef.current;
    if (commandChanged || now - lastSentTime.current > 85) {
      lastSentTime.current = now;
      prevCommandRef.current = data.command;
      setLastCommand(data.command);

      // Send primary movement command
      await bluetoothManager.sendCommand(data.command);

      // Motor audio pitch according to throttle %
      soundEffects.playBeep(280 + data.throttle * 4.5, 0.04);
    }
  };

  // BOCINA (HORN) : MANTENER / HOLD HANDLERS
  const startHornHold = (e: React.PointerEvent<HTMLButtonElement>) => {
    if (controlsDisabled || isTimeExpired) return;
    try {
      e.currentTarget.setPointerCapture(e.pointerId);
    } catch {
      // ignore
    }

    setHornActive(true);
    soundEffects.startHorn();
    setLastCommand('Z');
    bluetoothManager.sendCommand('Z');

    // Keep sending 'Z' while held down every 160ms
    if (hornIntervalRef.current) clearInterval(hornIntervalRef.current);
    hornIntervalRef.current = setInterval(() => {
      bluetoothManager.sendCommand('Z');
    }, 160);
  };

  const stopHornHold = (e?: React.PointerEvent<HTMLButtonElement>) => {
    if (e) {
      try {
        e.currentTarget.releasePointerCapture(e.pointerId);
      } catch {
        // ignore
      }
    }

    if (hornIntervalRef.current) {
      clearInterval(hornIntervalRef.current);
      hornIntervalRef.current = null;
    }
    setHornActive(false);
    soundEffects.stopHorn();
  };

  // Clean up horn interval on unmount
  useEffect(() => {
    return () => {
      if (hornIntervalRef.current) {
        clearInterval(hornIntervalRef.current);
      }
      soundEffects.stopHorn();
    };
  }, []);

  // LED Toggle handler: ONLY ONE button
  const handleLedToggle = async () => {
    if (controlsDisabled || isTimeExpired) return;

    if (!isLedOn) {
      // First press: Turn LED ON, send "E"
      setIsLedOn(true);
      setLastCommand('E');
      soundEffects.playBeep(1100, 0.08);
      await bluetoothManager.sendCommand('E');
    } else {
      // Second press: Turn LED OFF, send "D"
      setIsLedOn(false);
      setLastCommand('D');
      soundEffects.playBeep(700, 0.08);
      await bluetoothManager.sendCommand('D');
    }
  };

  // Format MM:SS
  const formatTime = (secs: number) => {
    const minutes = Math.floor(secs / 60);
    const remainingSeconds = secs % 60;
    return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`;
  };

  const isTimerCritical = secondsRemaining <= 30;

  return (
    <div className="relative min-h-[100dvh] w-full flex flex-col justify-between items-center px-3 sm:px-5 py-3 select-none overflow-y-auto overflow-x-hidden touch-pan-y pb-12">
      {/* 4th Image: Background of Car Control Screen (Sleek black buggy RC car with red engine) */}
      <div className="absolute inset-0 z-0">
        <img
          src={AppImages.bgControl}
          alt="SpeedBot Buggy Background"
          referrerPolicy="no-referrer"
          className="w-full h-full object-cover object-center brightness-[0.38] contrast-125"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-[#080c14] via-[#080c14]/80 to-[#080c14]/75"></div>
        <div className="absolute inset-0 cyber-grid opacity-20"></div>
      </div>

      {/* ========================================================
          TOP BAR: APP TITLE, STATUS & SALIR / PESTAÑAS
          ======================================================== */}
      <div className="w-full max-w-md md:max-w-xl lg:max-w-2xl flex items-center justify-between z-10 pt-1">
        <div className="flex items-center gap-2">
          <span className="font-heading font-black text-xl tracking-wider text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-blue-400">
            SpeedBot
          </span>
          <div className="flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-slate-950/80 backdrop-blur-md border border-slate-800 text-[11px] text-slate-300 font-mono">
            <Radio className="w-3 h-3 text-emerald-400 animate-pulse" />
            <span className="truncate max-w-[95px]">{device?.name || 'HC-05'}</span>
          </div>
        </div>

        {/* Top Navigation & Exit Buttons -> Shows "¿A qué pestaña querés ir?" */}
        <div className="flex items-center gap-2">
          <button
            onClick={handleOpenNavModal}
            className="px-3 py-1.5 rounded-xl bg-slate-950/80 hover:bg-slate-850 border border-cyan-500/40 text-cyan-300 hover:text-white font-bold font-heading text-xs tracking-wider uppercase transition flex items-center gap-1.5 cursor-pointer backdrop-blur-md"
            title="Elegir a qué pestaña deseas ir"
          >
            <LayoutGrid className="w-3.5 h-3.5 text-cyan-400" />
            <span>Pestañas</span>
          </button>

          <button
            id="btn-control-salir"
            onClick={handleOpenNavModal}
            className="px-3.5 py-1.5 rounded-xl bg-red-950/85 hover:bg-red-900 border border-red-500/70 text-red-300 hover:text-white font-bold font-heading text-xs tracking-widest uppercase transition transform active:scale-95 shadow-[0_0_20px_rgba(239,68,68,0.4)] flex items-center gap-1.5 cursor-pointer backdrop-blur-md"
            title="Salir o cambiar de pestaña"
          >
            <Power className="w-4 h-4" />
            <span>SALIR</span>
          </button>
        </div>
      </div>

      {/* ========================================================
          PROMINENT COUNTDOWN TIMER WITH PAUSE & TIME CONTROLS
          ======================================================== */}
      <div className="w-full max-w-md md:max-w-xl lg:max-w-2xl z-10 my-1 space-y-1.5">
        {/* Timer Box */}
        <div className={`p-2.5 rounded-2xl border backdrop-blur-md transition-all ${
          isTimerCritical 
            ? 'bg-red-950/70 border-red-500 shadow-[0_0_25px_rgba(239,68,68,0.45)]' 
            : 'bg-slate-950/85 border-slate-800 shadow-xl'
        }`}>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Clock className={`w-4 h-4 ${isTimerPaused ? 'text-amber-400' : isTimerCritical ? 'text-red-400 animate-spin' : 'text-cyan-400'}`} />
              <div className="flex flex-col">
                <span className="text-[10px] uppercase font-bold tracking-widest text-slate-400 font-mono">
                  TIEMPO DE CONTROL
                </span>
                {isTimerPaused && (
                  <span className="text-[10px] font-bold text-amber-400 font-mono animate-pulse">
                    ⏸ PAUSADO
                  </span>
                )}
              </div>
            </div>

            {/* Countdown Display & Pause Button */}
            <div className="flex items-center gap-2">
              <div className={`text-2xl font-black font-mono tracking-widest ${
                isTimerPaused ? 'text-amber-300' : isTimerCritical ? 'text-red-400 animate-pulse' : 'text-cyan-300'
              }`}>
                {formatTime(secondsRemaining)}
              </div>

              {/* Botón de Pausar / Reanudar el tiempo */}
              <button
                onClick={handleToggleTimerPause}
                className={`p-2 rounded-xl border transition flex items-center gap-1 text-xs font-mono font-bold cursor-pointer ${
                  isTimerPaused
                    ? 'bg-emerald-500 text-slate-950 border-emerald-300 shadow-[0_0_15px_rgba(16,185,129,0.5)]'
                    : 'bg-slate-900 hover:bg-slate-850 text-amber-300 border-amber-500/40'
                }`}
                title={isTimerPaused ? 'Reanudar tiempo' : 'Pausar tiempo'}
              >
                {isTimerPaused ? (
                  <>
                    <Play className="w-3.5 h-3.5 fill-current" />
                    <span>REANUDAR</span>
                  </>
                ) : (
                  <>
                    <Pause className="w-3.5 h-3.5 fill-current" />
                    <span>PAUSAR</span>
                  </>
                )}
              </button>
            </div>
          </div>

          {/* Quick Time Buttons (Reset 2:00, +30s max 2 min) */}
          <div className="flex items-center justify-between mt-2 pt-1.5 border-t border-slate-850 text-[10px] font-mono">
            <span className="text-slate-400">LÍMITE MÁX: 2:00 MIN</span>
            <div className="flex items-center gap-1.5">
              <button
                onClick={handleAdd30Seconds}
                disabled={secondsRemaining >= MAX_SESSION_SECONDS}
                className={`px-2 py-0.5 rounded-lg border text-[11px] font-mono flex items-center gap-1 transition ${
                  secondsRemaining >= MAX_SESSION_SECONDS
                    ? 'bg-slate-900/50 border-slate-800 text-slate-500 cursor-not-allowed'
                    : 'bg-slate-900 hover:bg-slate-800 border-slate-700 text-cyan-300 cursor-pointer'
                }`}
                title={secondsRemaining >= MAX_SESSION_SECONDS ? 'Límite de 2 minutos alcanzado' : 'Agregar 30 segundos (máximo 2:00)'}
              >
                <Plus className="w-3 h-3" />
                <span>+30s</span>
                {secondsRemaining >= MAX_SESSION_SECONDS && (
                  <span className="text-[9px] text-amber-400 font-bold">(Máx)</span>
                )}
              </button>
              <button
                onClick={handleResetTimer}
                className="px-2 py-0.5 rounded-lg bg-slate-900 hover:bg-slate-800 border border-slate-700 text-slate-300 flex items-center gap-1 cursor-pointer"
                title="Reiniciar a 2 minutos exactos"
              >
                <RotateCcw className="w-3 h-3 text-cyan-400" />
                <span>2:00</span>
              </button>
            </div>
          </div>

          {/* Linear countdown progress bar */}
          <div className="w-full h-1 bg-slate-950 rounded-full mt-2 overflow-hidden border border-slate-850">
            <div
              className={`h-full transition-all duration-1000 ease-linear ${
                isTimerPaused
                  ? 'bg-amber-400'
                  : isTimerCritical
                  ? 'bg-gradient-to-r from-red-600 to-amber-500'
                  : 'bg-gradient-to-r from-cyan-500 to-blue-500'
              }`}
              style={{ width: `${Math.min(100, (secondsRemaining / 120) * 100)}%` }}
            ></div>
          </div>
        </div>

        {/* Real-time Dynamic Pedal & Angle Telemetry Gauge */}
        <div className="grid grid-cols-3 gap-2 px-1">
          {/* Rumbo / Angle Gauge */}
          <div className="p-2 rounded-xl bg-slate-950/80 backdrop-blur-md border border-slate-800 flex flex-col items-center">
            <div className="flex items-center gap-1 text-[10px] text-slate-400 font-mono uppercase font-semibold">
              <Compass className="w-3 h-3 text-cyan-400" /> Rumbo
            </div>
            <span className="text-sm font-black font-mono text-cyan-300 mt-0.5">
              {currentThrottle > 0 ? `${currentAngle}°` : '0°'}
            </span>
          </div>

          {/* Pedal / Throttle Intensity Bar */}
          <div className="p-2 rounded-xl bg-slate-950/80 backdrop-blur-md border border-slate-800 flex flex-col items-center">
            <div className="flex items-center gap-1 text-[10px] text-slate-400 font-mono uppercase font-semibold">
              <Gauge className="w-3 h-3 text-amber-400" /> Pedal
            </div>
            <span className={`text-sm font-black font-mono mt-0.5 ${
              currentThrottle > 75 ? 'text-red-400' : currentThrottle > 35 ? 'text-amber-300' : currentThrottle > 0 ? 'text-emerald-400' : 'text-slate-500'
            }`}>
              {currentThrottle}%
            </span>
          </div>

          {/* Active Brake Status */}
          <div className="p-2 rounded-xl bg-slate-950/80 backdrop-blur-md border border-slate-800 flex flex-col items-center">
            <div className="flex items-center gap-1 text-[10px] text-slate-400 font-mono uppercase font-semibold">
              <ShieldAlert className="w-3 h-3 text-red-400" /> Estado
            </div>
            <span className={`text-xs font-bold font-mono mt-1 ${
              isBrakingState ? 'text-red-400' : 'text-emerald-400'
            }`}>
              {isBrakingState ? 'FRENANDO' : 'EN MARCHA'}
            </span>
          </div>
        </div>
      </div>

      {/* ========================================================
          MIDDLE: 360° CONTINUOUS ANALOG JOYSTICK (WITH 45° & SPEED)
          ======================================================== */}
      <div className="w-full max-w-md md:max-w-xl lg:max-w-2xl flex flex-col items-center justify-center my-auto py-2 z-10">
        <AnalogJoystick
          disabled={controlsDisabled || isTimeExpired}
          onJoystickChange={handleJoystickChange}
        />
      </div>

      {/* ========================================================
          ACTION BUTTONS: BOCINA (MANTENER) & LED (ONLY ONE BUTTON)
          ======================================================== */}
      <div className="w-full max-w-md md:max-w-xl lg:max-w-2xl grid grid-cols-2 gap-3 z-10 mb-1">
        {/* BOCINA Button : MANTENER */}
        <button
          id="btn-bocina"
          onPointerDown={startHornHold}
          onPointerUp={stopHornHold}
          onPointerLeave={stopHornHold}
          onPointerCancel={stopHornHold}
          onContextMenu={(e) => e.preventDefault()}
          disabled={controlsDisabled || isTimeExpired}
          className={`py-3.5 px-4 rounded-2xl font-black font-heading text-sm tracking-wider uppercase transition-all transform active:scale-95 flex flex-col items-center justify-center gap-1 border cursor-pointer select-none backdrop-blur-md ${
            hornActive
              ? 'bg-amber-500 text-slate-950 border-amber-300 shadow-[0_0_30px_rgba(245,158,11,0.85)] scale-95'
              : 'bg-slate-950/85 hover:bg-slate-900 border-slate-750 text-amber-300 hover:border-amber-500/60 shadow-lg'
          } ${controlsDisabled ? 'opacity-40 pointer-events-none' : ''}`}
        >
          <Volume2 className={`w-6 h-6 ${hornActive ? 'animate-bounce text-slate-950' : 'text-amber-400'}`} />
          <span>{hornActive ? '¡SONANDO!' : 'BOCINA'}</span>
          <span className="text-[10px] font-mono text-slate-400 font-semibold">
            {hornActive ? 'SUELTA PARA PARAR' : 'MANTENER PRESIONADO'}
          </span>
        </button>

        {/* LED TOGGLE Button: ONLY ONE BUTTON */}
        <button
          id="btn-led-toggle"
          onClick={handleLedToggle}
          disabled={controlsDisabled || isTimeExpired}
          className={`py-3.5 px-4 rounded-2xl font-black font-heading text-sm tracking-wider uppercase transition transform active:scale-95 flex flex-col items-center justify-center gap-1 border cursor-pointer backdrop-blur-md ${
            isLedOn
              ? 'bg-emerald-500 text-slate-950 border-emerald-300 shadow-[0_0_25px_rgba(16,185,129,0.7)]'
              : 'bg-slate-950/85 hover:bg-slate-900 border-slate-750 text-slate-300 hover:border-emerald-500/50 shadow-lg'
          } ${controlsDisabled ? 'opacity-40 pointer-events-none' : ''}`}
        >
          <Lightbulb className={`w-6 h-6 ${isLedOn ? 'text-slate-950 fill-slate-950' : 'text-slate-500'}`} />
          <span>{isLedOn ? 'LED ENCENDIDO' : 'LED APAGADO'}</span>
          <span className="text-[10px] font-mono text-slate-400 font-semibold">
            {isLedOn ? '(Cmd: E activa)' : '(Cmd: D apagada)'}
          </span>
        </button>
      </div>

      {/* Real-time Telemetry Bar */}
      <div className="w-full max-w-md md:max-w-xl lg:max-w-2xl z-10 px-3 py-1 flex items-center justify-between text-[11px] text-slate-400 font-mono bg-slate-950/85 backdrop-blur-md rounded-xl border border-slate-800">
        <span className="flex items-center gap-1.5">
          <span className={`w-2 h-2 rounded-full ${isBrakingState ? 'bg-red-500' : 'bg-emerald-400'}`}></span>
          {isBrakingState ? 'Freno de Motor:' : 'Enviando:'}
        </span>
        <span className="font-bold text-cyan-300">
          [{lastCommand}] {
            lastCommand === 'F' ? 'Adelante (90°)' :
            lastCommand === 'B' ? 'Atrás (270°)' :
            lastCommand === 'L' ? 'Izquierda (180°)' :
            lastCommand === 'R' ? 'Derecha (0°)' :
            lastCommand === 'I' ? 'Diagonal Der (45°)' :
            lastCommand === 'G' ? 'Diagonal Izq (135°)' :
            lastCommand === 'J' ? 'Reversa Der (315°)' :
            lastCommand === 'H' ? 'Reversa Izq (225°)' :
            lastCommand === 'S' ? 'Detenido / Freno' :
            lastCommand === 'Z' ? 'Bocina' :
            lastCommand === 'E' ? 'LED ON' :
            lastCommand === 'D' ? 'LED OFF' : ''
          }
        </span>
      </div>

      {/* ========================================================
          TIEMPO AGOTADO OVERLAY (TRIGGERED WHEN TIMER REACHES 00:00)
          ======================================================== */}
      {isTimeExpired && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-6 bg-black/90 backdrop-blur-lg animate-in fade-in duration-300">
          <div className="w-full max-w-sm p-6 rounded-3xl bg-slate-900 border-2 border-red-500 shadow-[0_0_50px_rgba(239,68,68,0.7)] text-center flex flex-col items-center">
            <div className="p-4 rounded-full bg-red-950/80 border border-red-500/50 text-red-400 mb-4 animate-bounce">
              <ShieldAlert className="w-12 h-12" />
            </div>

            <h2 className="text-3xl font-black font-heading tracking-wider uppercase text-white drop-shadow-[0_0_15px_rgba(239,68,68,0.8)]">
              TIEMPO AGOTADO
            </h2>

            <p className="text-sm text-slate-300 mt-2">
              Se ha enviado la orden de parada <strong className="text-red-400 font-mono">"S"</strong> al Arduino Nano.
            </p>

            <div className="mt-4 p-3 rounded-xl bg-slate-950 border border-slate-800 text-xs text-slate-400 space-y-1 w-full text-left font-mono">
              <p className="text-emerald-400">✓ Auto detenido con éxito</p>
              <p className="text-emerald-400">✓ Freno activo activado</p>
              <p className="text-emerald-400">✓ Desconectando módulo Bluetooth HC-05...</p>
            </div>

            <p className="text-xs text-cyan-400 mt-5 font-semibold animate-pulse">
              Regresando a Conectar el auto...
            </p>
          </div>
        </div>
      )}

      {/* ========================================================
          MODAL DE NAVEGACIÓN: ¿A QUÉ PESTAÑA QUERÉS IR?
          ======================================================== */}
      {showNavModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-in fade-in duration-200">
          <div className="w-full max-w-sm p-6 rounded-3xl bg-slate-900 border-2 border-cyan-500/50 shadow-[0_0_50px_rgba(6,182,212,0.35)] flex flex-col items-center">
            {/* Modal Icon */}
            <div className="p-3.5 rounded-2xl bg-cyan-950/90 border border-cyan-500/40 text-cyan-400 mb-3 shadow-inner">
              <LayoutGrid className="w-7 h-7" />
            </div>

            <h3 className="text-2xl font-black font-heading tracking-wide uppercase text-white mb-1 text-center">
              ¿A qué pestaña querés ir?
            </h3>
            <p className="text-xs text-slate-300 mb-5 text-center">
              Selecciona la sección a la que deseas navegar:
            </p>

            <div className="w-full flex flex-col gap-2.5">
              {/* Opción 1: Menú Principal */}
              <button
                onClick={() => handleNavigateTo('home')}
                className="w-full p-3.5 rounded-2xl bg-gradient-to-r from-cyan-500 via-blue-600 to-cyan-500 hover:from-cyan-400 hover:to-blue-500 text-slate-950 font-black font-heading text-sm tracking-wider uppercase transition transform active:scale-95 shadow-md flex items-center gap-3 cursor-pointer"
              >
                <div className="p-2 rounded-xl bg-slate-950/20 text-slate-950">
                  <Home className="w-5 h-5 fill-current" />
                </div>
                <div className="text-left flex-1">
                  <div className="text-sm font-black">Menú Principal</div>
                  <div className="text-[10px] text-slate-900 font-mono font-bold">Inicio • Colegio Juan XXIII</div>
                </div>
              </button>

              {/* Opción 2: Conectar el Auto (Bluetooth) */}
              <button
                onClick={() => handleNavigateTo('bluetooth')}
                className="w-full p-3.5 rounded-2xl bg-slate-950/90 hover:bg-slate-850 border border-slate-750 hover:border-red-500/50 text-slate-200 transition transform active:scale-95 shadow-md flex items-center gap-3 cursor-pointer"
              >
                <div className="p-2 rounded-xl bg-red-950 text-red-400 border border-red-500/30">
                  <Bluetooth className="w-5 h-5" />
                </div>
                <div className="text-left flex-1">
                  <div className="text-sm font-black font-heading tracking-wider uppercase text-white">Conectar el Auto</div>
                  <div className="text-[10px] text-slate-400 font-mono">Vincular Bluetooth HC-05</div>
                </div>
              </button>

              {/* Opción 3: Guía Arduino */}
              <button
                onClick={() => handleNavigateTo('guide')}
                className="w-full p-3.5 rounded-2xl bg-slate-950/90 hover:bg-slate-850 border border-slate-750 hover:border-cyan-500/50 text-slate-200 transition transform active:scale-95 shadow-md flex items-center gap-3 cursor-pointer"
              >
                <div className="p-2 rounded-xl bg-cyan-950 text-cyan-400 border border-cyan-500/30">
                  <BookOpen className="w-5 h-5" />
                </div>
                <div className="text-left flex-1">
                  <div className="text-sm font-black font-heading tracking-wider uppercase text-white">Guía Arduino Nano</div>
                  <div className="text-[10px] text-slate-400 font-mono">Conexiones y código C++</div>
                </div>
              </button>

              {/* Cancelar / Seguir Controlando */}
              <button
                onClick={() => {
                  soundEffects.playBeep(800, 0.08);
                  setShowNavModal(false);
                }}
                className="w-full mt-1 py-3 px-4 rounded-xl bg-slate-950 hover:bg-slate-800 border border-slate-750 text-slate-300 font-bold font-heading text-xs tracking-wider uppercase transition flex items-center justify-center gap-2 cursor-pointer"
              >
                <X className="w-4 h-4 text-slate-400" />
                <span>Continuar Controlando</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
