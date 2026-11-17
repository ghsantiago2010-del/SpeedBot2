import { useState, useEffect } from 'react';
import { HomeScreen } from './components/HomeScreen';
import { BluetoothScreen } from './components/BluetoothScreen';
import { CarControlScreen } from './components/CarControlScreen';
import { ArduinoGuideModal } from './components/ArduinoGuideModal';
import { PermissionModal } from './components/PermissionModal';
import { AppScreen, BluetoothDeviceInfo } from './types';
import { Power, RotateCcw, Wifi, BatteryCharging, Signal, Maximize2, Minimize2 } from 'lucide-react';

export default function App() {
  const [currentScreen, setCurrentScreen] = useState<AppScreen>('home');
  const [hasPermission, setHasPermission] = useState<boolean>(false);
  const [showPermissionPrompt, setShowPermissionPrompt] = useState<boolean>(true);
  const [connectedDevice, setConnectedDevice] = useState<BluetoothDeviceInfo | null>(null);
  const [isGuideOpen, setIsGuideOpen] = useState<boolean>(false);
  const [isAppExited, setIsAppExited] = useState<boolean>(false);
  const [isFullscreen, setIsFullscreen] = useState<boolean>(false);

  // Load permission state from storage
  useEffect(() => {
    try {
      const savedPerm = localStorage.getItem('speedbot_bt_permission');
      if (savedPerm === 'granted') {
        setHasPermission(true);
        setShowPermissionPrompt(false);
      }
    } catch {
      // ignore
    }
  }, []);

  const handleAllowPermission = () => {
    try {
      localStorage.setItem('speedbot_bt_permission', 'granted');
    } catch {
      // ignore
    }
    setHasPermission(true);
    setShowPermissionPrompt(false);
  };

  const handleDenyPermission = () => {
    setShowPermissionPrompt(false);
    setIsAppExited(true);
  };

  // Monitor fullscreen change events
  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(!!document.fullscreenElement);
    };
    document.addEventListener('fullscreenchange', handleFullscreenChange);
    return () => {
      document.removeEventListener('fullscreenchange', handleFullscreenChange);
    };
  }, []);

  // Fullscreen toggle handler
  const toggleFullscreen = () => {
    try {
      if (!document.fullscreenElement) {
        document.documentElement.requestFullscreen?.().catch(() => {});
      } else {
        document.exitFullscreen?.().catch(() => {});
      }
    } catch {
      // browser permissions
    }
  };

  // Navigation handlers
  const handleStartFromHome = () => {
    setCurrentScreen('bluetooth');
  };

  const handleExitApp = () => {
    try {
      window.close();
    } catch {
      // browser may block window.close
    }
    setIsAppExited(true);
  };

  const handleDeviceConnected = (device: BluetoothDeviceInfo) => {
    setConnectedDevice(device);
    // After successfully connecting to the HC-05, go to the car control screen
    setCurrentScreen('control');
  };

  const handleBackToHome = () => {
    setConnectedDevice(null);
    setCurrentScreen('home');
  };

  // When user presses SALIR in CarControlScreen: takes them to "conectar el auto" (BluetoothScreen)
  const handleExitFromControl = () => {
    setConnectedDevice(null);
    setCurrentScreen('bluetooth');
  };

  // If user pressed SALIR on Home Screen
  if (isAppExited) {
    return (
      <div className="min-h-screen w-full flex flex-col items-center justify-center bg-[#05080f] text-slate-100 p-4 sm:p-6 text-center cyber-grid">
        <div className="w-full max-w-md p-6 sm:p-8 rounded-3xl bg-slate-900/95 border-2 border-cyan-500/40 shadow-[0_0_50px_rgba(6,182,212,0.35)] flex flex-col items-center backdrop-blur-xl animate-in fade-in zoom-in-95 duration-300">
          <div className="p-4 rounded-full bg-cyan-950/80 border border-cyan-500/40 text-cyan-400 mb-4 shadow-inner">
            <Power className="w-10 h-10" />
          </div>
          <h2 className="text-3xl sm:text-4xl font-black font-heading tracking-wide uppercase text-white mb-2 drop-shadow-[0_0_20px_rgba(6,182,212,0.6)]">
            ¡adiós, te bancamos!
          </h2>
          <p className="text-sm text-cyan-300/80 font-mono mb-6">
            Sesión finalizada con éxito. Gracias por usar SpeedBot • Colegio Juan XXIII
          </p>
          <div className="w-full flex flex-col gap-2.5">
            <button
              onClick={() => setIsAppExited(false)}
              className="w-full py-3.5 px-4 rounded-xl bg-gradient-to-r from-cyan-500 via-blue-600 to-cyan-500 hover:from-cyan-400 hover:to-blue-500 text-slate-950 font-black font-heading tracking-wider uppercase transition flex items-center justify-center gap-2 cursor-pointer shadow-[0_0_25px_rgba(6,182,212,0.4)]"
            >
              <RotateCcw className="w-5 h-5" />
              <span>REINICIAR SPEEDBOT</span>
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen w-full bg-[#05080f] text-slate-100 flex items-center justify-center p-0 sm:p-2 md:p-4">
      {/* Responsive App Container: mobile full width, tablet / desktop responsive frame */}
      <main className={`w-full ${isFullscreen ? 'max-w-none min-h-screen rounded-none border-0' : 'max-w-md md:max-w-2xl lg:max-w-4xl min-h-[100dvh] md:h-[92vh] md:max-h-[960px] md:rounded-[36px] md:border-2 md:border-slate-800'} bg-[#080c14] relative flex flex-col shadow-[0_0_60px_rgba(0,0,0,0.9)] select-none overflow-y-auto overflow-x-hidden touch-pan-y`}>
        {/* Android Top Notch & System Status Bar with Fullscreen Toggle */}
        <div className="w-full px-4 py-1.5 flex items-center justify-between text-[11px] text-slate-400 font-mono z-30 bg-slate-950/80 backdrop-blur-md border-b border-slate-850/60 sticky top-0">
          <span className="font-bold text-slate-300">12:30</span>
          
          {/* Pantalla completa button in status bar */}
          <button
            onClick={toggleFullscreen}
            className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-slate-900 hover:bg-slate-800 border border-slate-750 text-slate-300 hover:text-cyan-300 text-[11px] cursor-pointer transition"
            title={isFullscreen ? 'Salir de pantalla completa' : 'Pantalla completa de la aplicación'}
          >
            {isFullscreen ? (
              <>
                <Minimize2 className="w-3.5 h-3.5 text-cyan-400" />
                <span>Salir Pantalla Completa</span>
              </>
            ) : (
              <>
                <Maximize2 className="w-3.5 h-3.5 text-cyan-400" />
                <span>Pantalla Completa</span>
              </>
            )}
          </button>

          <div className="flex items-center gap-2 text-slate-300">
            <Signal className="w-3 h-3" />
            <Wifi className="w-3 h-3" />
            <span className="text-[10px] font-bold">100%</span>
            <BatteryCharging className="w-3.5 h-3.5 text-emerald-400" />
          </div>
        </div>

        {/* Dynamic Screen View */}
        <div className="flex-1 w-full relative flex flex-col overflow-y-auto overflow-x-hidden touch-pan-y">
          {/* SCREEN 1: HOME */}
          {currentScreen === 'home' && (
            <HomeScreen
              onStart={handleStartFromHome}
              onExit={handleExitApp}
              onOpenGuide={() => setIsGuideOpen(true)}
            />
          )}

          {/* SCREEN 2: BLUETOOTH CONNECTION */}
          {currentScreen === 'bluetooth' && (
            <BluetoothScreen
              onConnected={handleDeviceConnected}
              onBack={handleBackToHome}
              onOpenGuide={() => setIsGuideOpen(true)}
            />
          )}

          {/* SCREEN 3: CAR CONTROL */}
          {currentScreen === 'control' && (
            <CarControlScreen
              device={connectedDevice}
              onExitToBluetooth={handleExitFromControl}
              onGoToHome={handleBackToHome}
              onOpenGuide={() => setIsGuideOpen(true)}
            />
          )}
        </div>

        {/* Android Bottom Navigation Pill Gesture Bar */}
        <div className="w-full py-1.5 flex items-center justify-center bg-slate-950/90 border-t border-slate-900 z-30 sticky bottom-0">
          <div className="w-32 h-1 bg-slate-600/80 rounded-full"></div>
        </div>

        {/* Arduino Nano & HC-05 Connection/Code Modal Guide */}
        <ArduinoGuideModal
          isOpen={isGuideOpen}
          onClose={() => setIsGuideOpen(false)}
        />

        {/* Startup Bluetooth Permission Prompt: 'Permitir que el sitio web acceda a Bluetooth' */}
        {showPermissionPrompt && (
          <PermissionModal
            onAllow={handleAllowPermission}
            onDeny={handleDenyPermission}
          />
        )}
      </main>
    </div>
  );
}
