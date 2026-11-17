import React from 'react';
import { Play, Power, HelpCircle, Users, Cpu, ShieldCheck } from 'lucide-react';
import { soundEffects } from '../utils/audioFeedback';
import { AppImages } from '../assets/images';

interface HomeScreenProps {
  onStart: () => void;
  onExit: () => void;
  onOpenGuide: () => void;
}

export function HomeScreen({ onStart, onExit, onOpenGuide }: HomeScreenProps) {
  const handleStart = () => {
    soundEffects.playBeep(980, 0.12);
    onStart();
  };

  const handleExit = () => {
    soundEffects.playWarning();
    onExit();
  };

  const creators = [
    'Luca Fazio',
    'Gonzalo Fernandez',
    'Santiago Garcia Higa',
    'Joaquin Gimenez',
  ];

  return (
    <div className="relative min-h-[100dvh] w-full flex flex-col justify-between items-center px-4 py-5 overflow-y-auto overflow-x-hidden touch-pan-y select-none">
      {/* 3rd Image: Background for Home Screen (Stadium Arena) */}
      <div className="absolute inset-0 z-0">
        <img
          src={AppImages.bgHome}
          alt="Stadium Arena Background"
          referrerPolicy="no-referrer"
          className="w-full h-full object-cover object-center brightness-[0.45] contrast-125 scale-105"
        />
        {/* Futuristic dark overlay gradients */}
        <div className="absolute inset-0 bg-gradient-to-t from-[#080c14] via-[#080c14]/75 to-[#080c14]/80"></div>
        <div className="absolute inset-0 cyber-grid opacity-30"></div>
      </div>

      {/* Top Header / Android Status Bar style */}
      <div className="w-full max-w-md flex justify-between items-center z-10 pt-1">
        <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-slate-950/85 backdrop-blur-md border border-cyan-500/30 text-xs text-slate-300">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
          <span className="tracking-widest uppercase font-bold text-cyan-300 font-mono">SPEEDBOT</span>
        </div>

        <button
          onClick={onOpenGuide}
          className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-slate-950/85 backdrop-blur-md hover:bg-slate-850 border border-slate-700 text-xs text-slate-200 transition cursor-pointer"
          title="Ver código Arduino Nano y conexiones"
        >
          <HelpCircle className="w-3.5 h-3.5 text-cyan-400" />
          <span className="font-semibold">Guía Arduino</span>
        </button>
      </div>

      {/* Center Brand Identity with 2nd Image: The Logo */}
      <div className="relative flex flex-col items-center justify-center my-auto z-10 text-center w-full max-w-md">
        {/* 2nd Image: Logo badge */}
        <div className="relative mb-3 group">
          <div className="absolute -inset-3 bg-cyan-500/30 rounded-full blur-xl animate-pulse"></div>
          <div className="relative w-36 h-36 rounded-full p-1.5 bg-gradient-to-br from-cyan-400 via-blue-600 to-cyan-500 shadow-[0_0_35px_rgba(6,182,212,0.6)]">
            <img
              src={AppImages.logo}
              alt="Logo SpeedBot Auto RC Arduino"
              referrerPolicy="no-referrer"
              className="w-full h-full rounded-full object-cover shadow-inner border-2 border-slate-950"
            />
          </div>
        </div>

        {/* Project Name: SpeedBot */}
        <h1 className="text-5xl font-black font-heading tracking-wider uppercase text-transparent bg-clip-text bg-gradient-to-r from-cyan-300 via-white to-cyan-400 drop-shadow-[0_0_25px_rgba(6,182,212,0.7)]">
          SpeedBot
        </h1>
        <p className="text-xs text-slate-300 tracking-widest uppercase font-bold mt-0.5">
          Control Bluetooth HC-05 • Arduino Nano
        </p>

        {/* Authors / Creators Section in the Menu as requested */}
        <div className="mt-4 w-full max-w-sm px-2">
          <div className="p-3 rounded-2xl bg-slate-950/80 backdrop-blur-md border border-cyan-500/25 shadow-xl">
            <div className="flex items-center justify-center gap-1.5 text-[11px] font-mono tracking-widest uppercase text-cyan-400 font-bold mb-1">
              <Users className="w-3.5 h-3.5 text-cyan-400" />
              <span>Desarrollado Por:</span>
            </div>
            
            {/* Colegio Juan XXIII */}
            <div className="text-center mb-2.5">
              <span className="inline-block px-3 py-1 rounded-full bg-cyan-950/80 border border-cyan-400/40 text-xs font-bold text-cyan-300 font-heading tracking-wider uppercase shadow-[0_0_15px_rgba(6,182,212,0.25)]">
                Colegio Juan XXIII
              </span>
            </div>

            <div className="grid grid-cols-2 gap-1.5 text-xs font-semibold text-slate-200">
              {creators.map((name) => (
                <div
                  key={name}
                  className="px-2.5 py-1.5 rounded-lg bg-slate-900/90 border border-slate-800/90 text-center font-mono text-[11px] text-slate-200 shadow-sm flex items-center justify-center gap-1"
                >
                  <span className="w-1.5 h-1.5 rounded-full bg-cyan-400"></span>
                  <span className="truncate">{name}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Specs Pill */}
        <div className="mt-3 flex items-center gap-3 text-[11px] text-slate-400 font-mono">
          <span className="flex items-center gap-1"><Cpu className="w-3 h-3 text-cyan-400" /> HC-05 9600 BAUD</span>
          <span>•</span>
          <span className="flex items-center gap-1"><ShieldCheck className="w-3 h-3 text-emerald-400" /> PEDAL ANALÓGICO 360°</span>
        </div>
      </div>

      {/* Bottom Main Action Buttons (INICIAR & SALIR) */}
      <div className="w-full max-w-sm md:max-w-md flex flex-col gap-3 z-10 mb-2">
        {/* INICIAR Button */}
        <button
          id="btn-iniciar"
          onClick={handleStart}
          className="relative group w-full py-4 px-6 rounded-2xl bg-gradient-to-r from-cyan-500 via-blue-600 to-cyan-500 hover:from-cyan-400 hover:to-blue-500 text-slate-950 font-black font-heading text-xl tracking-wider uppercase transition-all duration-200 transform active:scale-95 shadow-[0_0_30px_rgba(6,182,212,0.5)] flex items-center justify-center gap-3 border border-cyan-300/50 cursor-pointer"
        >
          <Play className="w-6 h-6 fill-slate-950 stroke-none" />
          <span>INICIAR</span>
        </button>

        {/* SALIR Button */}
        <button
          id="btn-salir"
          onClick={handleExit}
          className="relative w-full py-3.5 px-6 rounded-2xl bg-slate-950/90 hover:bg-red-950/60 border border-slate-800 hover:border-red-500/60 text-slate-300 hover:text-red-400 font-bold font-heading text-base tracking-wider uppercase transition-all duration-200 transform active:scale-95 flex items-center justify-center gap-2 cursor-pointer backdrop-blur-md"
        >
          <Power className="w-5 h-5" />
          <span>SALIR</span>
        </button>
      </div>
    </div>
  );
}
