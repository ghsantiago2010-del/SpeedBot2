import React from 'react';
import { Bluetooth, ShieldCheck, XCircle, CheckCircle, Smartphone, AlertCircle } from 'lucide-react';
import { soundEffects } from '../utils/audioFeedback';

interface PermissionModalProps {
  onAllow: () => void;
  onDeny: () => void;
}

export function PermissionModal({ onAllow, onDeny }: PermissionModalProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/90 backdrop-blur-md animate-in fade-in duration-200">
      <div className="w-full max-w-sm p-6 rounded-3xl bg-slate-900/95 border-2 border-cyan-500/50 shadow-[0_0_50px_rgba(6,182,212,0.4)] flex flex-col items-center text-center">
        {/* Glowing Bluetooth Security Icon */}
        <div className="relative mb-3">
          <div className="absolute -inset-3 bg-cyan-500/30 rounded-full blur-xl animate-pulse"></div>
          <div className="relative p-4 rounded-2xl bg-cyan-950/90 border border-cyan-400/60 text-cyan-300 shadow-inner">
            <Bluetooth className="w-10 h-10 animate-pulse" />
          </div>
        </div>

        {/* Required Exact Title */}
        <h2 className="text-2xl font-black font-heading tracking-wide uppercase text-white mb-2 leading-tight">
          Permitir que el sitio web acceda a Bluetooth
        </h2>

        <p className="text-xs text-slate-300 mb-4 leading-relaxed font-sans">
          SpeedBot necesita vincularse con el Bluetooth de tu celular para enviar las señales de dirección y velocidad al auto Arduino Nano.
        </p>

        {/* Android Native Selection Note */}
        <div className="w-full p-3 rounded-xl bg-slate-950/80 border border-slate-800 text-left mb-5 space-y-1.5">
          <div className="flex items-center gap-1.5 text-[11px] font-bold text-cyan-400 font-mono">
            <Smartphone className="w-3.5 h-3.5" />
            <span>MENÚ NATIVO DE TU CELULAR</span>
          </div>
          <p className="text-[11px] text-slate-400">
            Al presionar Permitir, se abrirá el menú Bluetooth del celular (Android / Chrome) para seleccionar tu módulo HC-05.
          </p>
        </div>

        {/* 2 Required Options: Permitir & No Permitir (Salir) */}
        <div className="w-full flex flex-col gap-2.5">
          {/* Option 1: Permitir */}
          <button
            onClick={() => {
              soundEffects.playBeep(980, 0.12);
              onAllow();
            }}
            className="w-full py-3.5 px-4 rounded-xl bg-gradient-to-r from-cyan-500 via-blue-600 to-cyan-500 hover:from-cyan-400 hover:to-blue-500 text-slate-950 font-black font-heading text-sm tracking-wider uppercase transition transform active:scale-95 shadow-[0_0_20px_rgba(6,182,212,0.4)] flex items-center justify-center gap-2 cursor-pointer border border-cyan-300/40"
          >
            <CheckCircle className="w-5 h-5 fill-slate-950 text-cyan-400" />
            <span>PERMITIR</span>
          </button>

          {/* Option 2: No permitir (salir) */}
          <button
            onClick={() => {
              soundEffects.playWarning();
              onDeny();
            }}
            className="w-full py-3 px-4 rounded-xl bg-slate-950 hover:bg-red-950/80 border border-slate-750 hover:border-red-500/60 text-slate-300 hover:text-red-400 font-bold font-heading text-xs tracking-wider uppercase transition flex items-center justify-center gap-2 cursor-pointer"
          >
            <XCircle className="w-4 h-4" />
            <span>NO PERMITIR (SALIR)</span>
          </button>
        </div>
      </div>
    </div>
  );
}
