import React, { useRef, useState, useCallback, useEffect } from 'react';
import { JoystickData, CarCommand } from '../types';
import { ShieldAlert, Compass } from 'lucide-react';

interface AnalogJoystickProps {
  disabled?: boolean;
  onJoystickChange: (data: JoystickData) => void;
}

export function AnalogJoystick({ disabled = false, onJoystickChange }: AnalogJoystickProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [knobPos, setKnobPos] = useState({ x: 0, y: 0 }); // relative offset from center in px
  const [currentAngle, setCurrentAngle] = useState<number>(0); // 0 to 359 degrees
  const [throttle, setThrottle] = useState<number>(0); // 0 to 100%
  const [isBraking, setIsBraking] = useState<boolean>(false);
  const [activeCommand, setActiveCommand] = useState<CarCommand>('S');

  // Maximum radius knob can move from center
  const MAX_RADIUS = 75;
  const DEADZONE = 10; // px for deadzone

  const calculateJoystickData = useCallback(
    (clientX: number, clientY: number): JoystickData => {
      if (!containerRef.current) {
        return {
          angle: 0,
          throttle: 0,
          normalizedX: 0,
          normalizedY: 0,
          command: 'S',
          speedPwm: 0,
          isBraking: true,
        };
      }

      const rect = containerRef.current.getBoundingClientRect();
      const centerX = rect.left + rect.width / 2;
      const centerY = rect.top + rect.height / 2;

      let dx = clientX - centerX;
      let dy = clientY - centerY;

      const rawDistance = Math.sqrt(dx * dx + dy * dy);

      // Clamp distance to MAX_RADIUS
      const distance = Math.min(rawDistance, MAX_RADIUS);
      if (rawDistance > MAX_RADIUS) {
        dx = (dx / rawDistance) * MAX_RADIUS;
        dy = (dy / rawDistance) * MAX_RADIUS;
      }

      setKnobPos({ x: dx, y: dy });

      // If within deadzone, it's stopped/braking
      if (distance <= DEADZONE) {
        setThrottle(0);
        setIsBraking(true);
        setActiveCommand('S');
        return {
          angle: 0,
          throttle: 0,
          normalizedX: 0,
          normalizedY: 0,
          command: 'S',
          speedPwm: 0,
          isBraking: true,
        };
      }

      // Calculate throttle percentage (0% to 100%) like pressing a real accelerator pedal
      const effectiveDist = distance - DEADZONE;
      const maxEffectiveDist = MAX_RADIUS - DEADZONE;
      const throttlePercent = Math.min(100, Math.round((effectiveDist / maxEffectiveDist) * 100));
      const speedPwm = Math.min(255, Math.round((throttlePercent / 100) * 255));

      // Calculate continuous angle in degrees (0° to 359°)
      // Screen coordinates: dx > 0 is right, dy < 0 is UP (North)
      // Mathematical angle: -dy because Y is inverted in browser screen
      let mathAngle = Math.atan2(-dy, dx) * (180 / Math.PI);
      if (mathAngle < 0) {
        mathAngle += 360;
      }
      const roundedAngle = Math.round(mathAngle);

      setThrottle(throttlePercent);
      setCurrentAngle(roundedAngle);
      setIsBraking(false);

      // Map continuous angle into car commands (supports 45°, 135°, 225°, 315° diagonals too!)
      // 90° = Up (F)
      // 45° = Up-Right (I)
      // 0°/360° = Right (R)
      // 315° = Down-Right (J)
      // 270° = Down (B)
      // 225° = Down-Left (H)
      // 180° = Left (L)
      // 135° = Up-Left (G)
      let cmd: CarCommand = 'S';

      if (roundedAngle >= 67.5 && roundedAngle < 112.5) {
        cmd = 'F'; // 90° Adelante
      } else if (roundedAngle >= 22.5 && roundedAngle < 67.5) {
        cmd = 'I'; // 45° Adelante-Derecha
      } else if (roundedAngle >= 112.5 && roundedAngle < 157.5) {
        cmd = 'G'; // 135° Adelante-Izquierda
      } else if (roundedAngle >= 157.5 && roundedAngle < 202.5) {
        cmd = 'L'; // 180° Izquierda
      } else if (roundedAngle >= 202.5 && roundedAngle < 247.5) {
        cmd = 'H'; // 225° Atras-Izquierda
      } else if (roundedAngle >= 247.5 && roundedAngle < 292.5) {
        cmd = 'B'; // 270° Atras
      } else if (roundedAngle >= 292.5 && roundedAngle < 337.5) {
        cmd = 'J'; // 315° Atras-Derecha
      } else {
        cmd = 'R'; // 0° / 360° Derecha
      }

      setActiveCommand(cmd);

      return {
        angle: roundedAngle,
        throttle: throttlePercent,
        normalizedX: +(dx / MAX_RADIUS).toFixed(2),
        normalizedY: +(-dy / MAX_RADIUS).toFixed(2),
        command: cmd,
        speedPwm,
        isBraking: false,
      };
    },
    [DEADZONE, MAX_RADIUS]
  );

  const handlePointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    if (disabled) return;
    e.currentTarget.setPointerCapture(e.pointerId);
    setIsDragging(true);
    setIsBraking(false);
    const data = calculateJoystickData(e.clientX, e.clientY);
    onJoystickChange(data);
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!isDragging || disabled) return;
    const data = calculateJoystickData(e.clientX, e.clientY);
    onJoystickChange(data);
  };

  const handlePointerUp = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!isDragging) return;
    try {
      e.currentTarget.releasePointerCapture(e.pointerId);
    } catch {
      // ignore
    }
    setIsDragging(false);
    setKnobPos({ x: 0, y: 0 });
    setThrottle(0);
    setIsBraking(true);
    setActiveCommand('S');

    // Send Stop / Brake data
    onJoystickChange({
      angle: currentAngle,
      throttle: 0,
      normalizedX: 0,
      normalizedY: 0,
      command: 'S',
      speedPwm: 0,
      isBraking: true,
    });
  };

  // When disabled externally
  useEffect(() => {
    if (disabled) {
      setIsDragging(false);
      setKnobPos({ x: 0, y: 0 });
      setThrottle(0);
      setIsBraking(true);
      setActiveCommand('S');
    }
  }, [disabled]);

  // Direction label display
  const getDirectionText = () => {
    if (isBraking || throttle === 0) return 'FRENO ACTIVO / DETENIDO';
    if (currentAngle >= 35 && currentAngle <= 55) return 'DIAGONAL 45° (ADELANTE-DER)';
    if (currentAngle >= 125 && currentAngle <= 145) return 'DIAGONAL 135° (ADELANTE-IZQ)';
    if (currentAngle >= 215 && currentAngle <= 235) return 'DIAGONAL 225° (ATRÁS-IZQ)';
    if (currentAngle >= 305 && currentAngle <= 325) return 'DIAGONAL 315° (ATRÁS-DER)';
    
    switch (activeCommand) {
      case 'F': return `ADELANTE (${currentAngle}°)`;
      case 'B': return `ATRÁS (${currentAngle}°)`;
      case 'L': return `GIRO IZQUIERDA (${currentAngle}°)`;
      case 'R': return `GIRO DERECHA (${currentAngle}°)`;
      case 'I': return `DIAGONAL DER (${currentAngle}°)`;
      case 'G': return `DIAGONAL IZQ (${currentAngle}°)`;
      case 'J': return `REVERSA DER (${currentAngle}°)`;
      case 'H': return `REVERSA IZQ (${currentAngle}°)`;
      default: return `${currentAngle}°`;
    }
  };

  return (
    <div className="relative flex flex-col items-center justify-center select-none touch-none w-full max-w-sm">
      {/* Top Real-time Angle & Pedal HUD */}
      <div className="w-full flex items-center justify-between px-3 py-1.5 mb-2 rounded-xl bg-slate-950/80 border border-slate-800 text-xs font-mono">
        <div className="flex items-center gap-1.5">
          <Compass className={`w-4 h-4 ${throttle > 0 ? 'text-cyan-400 animate-spin' : 'text-slate-500'}`} />
          <span className="text-slate-400">RUMBO:</span>
          <span className={`font-black text-sm ${throttle > 0 ? 'text-cyan-300' : 'text-slate-500'}`}>
            {throttle > 0 ? `${currentAngle}°` : '0°'}
          </span>
        </div>

        {/* Dynamic Pedal Indicator */}
        <div className="flex items-center gap-1.5">
          <span className="text-slate-400">PEDAL:</span>
          <span className={`font-black text-sm ${
            throttle > 70 ? 'text-red-400' : throttle > 30 ? 'text-amber-300' : throttle > 0 ? 'text-emerald-400' : 'text-slate-500'
          }`}>
            {throttle}% {throttle >= 85 ? '🔥 MAX' : throttle > 0 ? '⚡' : ''}
          </span>
        </div>
      </div>

      {/* Main Joystick Base Ring */}
      <div className="relative p-2 flex items-center justify-center">
        {/* Outer Degree Ticks (Every 45° with 45° highlight) */}
        <div className="absolute inset-0 rounded-full border border-slate-800/80 pointer-events-none flex items-center justify-center">
          {/* 45° diagonal indicators */}
          <span className="absolute top-2 right-4 text-[10px] font-mono text-cyan-400/80 font-bold">45°</span>
          <span className="absolute top-2 left-4 text-[10px] font-mono text-cyan-400/80 font-bold">135°</span>
          <span className="absolute bottom-2 left-4 text-[10px] font-mono text-cyan-400/80 font-bold">225°</span>
          <span className="absolute bottom-2 right-4 text-[10px] font-mono text-cyan-400/80 font-bold">315°</span>

          {/* Cardinal labels */}
          <span className="absolute top-0 text-[11px] font-mono font-black text-cyan-300">90° F</span>
          <span className="absolute bottom-0 text-[11px] font-mono font-black text-cyan-300">270° B</span>
          <span className="absolute left-1 text-[11px] font-mono font-black text-cyan-300">180° L</span>
          <span className="absolute right-1 text-[11px] font-mono font-black text-cyan-300">0° R</span>
        </div>

        {/* Dynamic Vector Line from center to knob */}
        {isDragging && throttle > 0 && (
          <svg className="absolute w-52 h-52 pointer-events-none z-10" viewBox="-104 -104 208 208">
            <line
              x1="0"
              y1="0"
              x2={knobPos.x}
              y2={knobPos.y}
              stroke="rgba(6, 182, 212, 0.8)"
              strokeWidth="3"
              strokeDasharray="4 2"
            />
            {/* Arrowhead / Dot at destination */}
            <circle cx={knobPos.x} cy={knobPos.y} r="4" fill="#38bdf8" />
          </svg>
        )}

        {/* Touchable Joystick Basin */}
        <div
          ref={containerRef}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerCancel={handlePointerUp}
          className={`relative w-52 h-52 rounded-full flex items-center justify-center transition-all ${
            disabled
              ? 'opacity-40 pointer-events-none bg-slate-900 border border-slate-800'
              : 'bg-gradient-to-b from-slate-900/90 via-slate-950 to-[#070b12] border-2 border-slate-750 shadow-[0_0_40px_rgba(0,0,0,0.9),inset_0_0_30px_rgba(6,182,212,0.12)] cursor-grab active:cursor-grabbing'
          }`}
        >
          {/* Subtle concentric rings for pedal intensity visualization */}
          <div className="absolute w-36 h-36 rounded-full border border-dashed border-slate-800 pointer-events-none flex items-center justify-center">
            <span className="text-[9px] text-slate-700 font-mono absolute -top-2">50%</span>
          </div>
          <div className="absolute w-20 h-20 rounded-full border border-slate-800/80 pointer-events-none"></div>

          {/* Center deadzone ring / brake zone */}
          <div className="absolute w-12 h-12 rounded-full border-2 border-slate-700/60 bg-slate-950/80 flex items-center justify-center pointer-events-none">
            {isBraking || throttle === 0 ? (
              <span className="text-[9px] font-mono font-bold text-red-400">FRENO</span>
            ) : null}
          </div>

          {/* Joystick Handle / Analog Thumbstick Knob */}
          <div
            className={`absolute w-20 h-20 rounded-full flex flex-col items-center justify-center transition-transform ${
              isDragging ? 'duration-0 shadow-[0_0_30px_rgba(6,182,212,0.8)]' : 'duration-200 ease-out shadow-[0_0_15px_rgba(0,0,0,0.8)]'
            } ${
              throttle > 0
                ? 'bg-gradient-to-br from-cyan-400 via-blue-500 to-cyan-600 border-2 border-white'
                : isBraking
                ? 'bg-gradient-to-br from-red-950 to-slate-900 border-2 border-red-500/80 shadow-[0_0_20px_rgba(239,68,68,0.4)]'
                : 'bg-gradient-to-br from-slate-800 via-slate-750 to-slate-900 border-2 border-slate-600'
            }`}
            style={{
              transform: `translate(${knobPos.x}px, ${knobPos.y}px)`,
            }}
          >
            {/* Knob Inner Ring Details */}
            <div className="w-14 h-14 rounded-full bg-slate-950/85 border border-white/20 flex flex-col items-center justify-center">
              {throttle === 0 ? (
                <span className="text-[10px] font-mono font-black text-red-400">ALTO</span>
              ) : (
                <>
                  <span className="text-xs font-black font-mono text-cyan-200">{currentAngle}°</span>
                  <span className="text-[9px] font-mono text-amber-300 font-bold">{throttle}%</span>
                </>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Dynamic Status / Deceleration Feedback Bar */}
      <div className="mt-2 w-full flex items-center justify-center">
        <div className={`px-4 py-1.5 rounded-xl text-xs font-mono font-bold tracking-wider flex items-center gap-2 border transition-all ${
          isBraking || throttle === 0
            ? 'bg-red-950/70 text-red-300 border-red-500/50 shadow-[0_0_12px_rgba(239,68,68,0.3)]'
            : 'bg-cyan-950/70 text-cyan-300 border-cyan-500/40 shadow-[0_0_15px_rgba(6,182,212,0.25)]'
        }`}>
          {isBraking || throttle === 0 ? (
            <>
              <ShieldAlert className="w-4 h-4 text-red-400" />
              <span>FRENO ACTIVO (AUTO DETENIDO)</span>
            </>
          ) : (
            <>
              <span className="w-2 h-2 rounded-full bg-cyan-400 animate-ping"></span>
              <span>{getDirectionText()}</span>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
