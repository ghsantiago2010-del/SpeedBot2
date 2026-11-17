import { useState } from 'react';
import { X, Copy, Check, Cpu, Zap, Radio } from 'lucide-react';

interface ArduinoGuideModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function ArduinoGuideModal({ isOpen, onClose }: ArduinoGuideModalProps) {
  const [copied, setCopied] = useState(false);

  if (!isOpen) return null;

  const arduinoCode = `/*
  SpeedBot - Arduino Nano + HC-05 Robot Car Firmware
  Baud rate: 9600
  Comandos Bluetooth:
  F = Adelante (Forward)
  B = Atras (Backward)
  L = Izquierda (Left)
  R = Derecha (Right)
  S = Detener (Stop)
  Z = Bocina (Buzzer)
  E = Luces LED Encendidas
  D = Luces LED Apagadas
*/

#include <SoftwareSerial.h>

// HC-05 conectado a pines digitales (RX, TX)
// Arduino Pin 2 (RX) -> HC-05 TX
// Arduino Pin 3 (TX) -> HC-05 RX (usar divisor de voltaje 1k/2k si es posible)
SoftwareSerial BTSerial(2, 3);

// Pines del Puente H (L298N / L293D)
const int IN1 = 5;
const int IN2 = 6;
const int IN3 = 9;
const int IN4 = 10;

// Pin de Bocina / Buzzer
const int PIN_BUZZER = 11;

// Pin de Luces LED
const int PIN_LED = 12;

char command = 'S';

void setup() {
  Serial.begin(9600);
  BTSerial.begin(9600);

  pinMode(IN1, OUTPUT);
  pinMode(IN2, OUTPUT);
  pinMode(IN3, OUTPUT);
  pinMode(IN4, OUTPUT);
  pinMode(PIN_BUZZER, OUTPUT);
  pinMode(PIN_LED, OUTPUT);

  detenerAuto();
  digitalWrite(PIN_LED, LOW);
  digitalWrite(PIN_BUZZER, LOW);
  Serial.println("SpeedBot listo para recibir comandos...");
}

void loop() {
  // Leer comando desde HC-05 (o Serial USB)
  if (BTSerial.available()) {
    command = BTSerial.read();
    procesarComando(command);
  } else if (Serial.available()) {
    command = Serial.read();
    procesarComando(command);
  }
}

void procesarComando(char cmd) {
  switch (cmd) {
    case 'F': // Adelante
      adelante();
      break;
    case 'B': // Atras
      atras();
      break;
    case 'L': // Izquierda (180°)
      izquierda();
      break;
    case 'R': // Derecha (0°)
      derecha();
      break;
    case 'I': // Diagonal 45° (Adelante-Derecha)
      adelanteDerecha();
      break;
    case 'G': // Diagonal 135° (Adelante-Izquierda)
      adelanteIzquierda();
      break;
    case 'J': // Reversa 315° (Atras-Derecha)
      atrasDerecha();
      break;
    case 'H': // Reversa 225° (Atras-Izquierda)
      atrasIzquierda();
      break;
    case 'S': // Detener / Freno Activo
      detenerAuto();
      break;
    case 'Z': // Bocina / Horn
      sonarBocina();
      break;
    case 'E': // LED ON
      digitalWrite(PIN_LED, HIGH);
      break;
    case 'D': // LED OFF
      digitalWrite(PIN_LED, LOW);
      break;
  }
}

void adelante() {
  digitalWrite(IN1, HIGH);
  digitalWrite(IN2, LOW);
  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);
}

void atras() {
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, HIGH);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, HIGH);
}

void izquierda() {
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, HIGH);
  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);
}

void derecha() {
  digitalWrite(IN1, HIGH);
  digitalWrite(IN2, LOW);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, HIGH);
}

// Giros en diagonal a 45° y 135°
void adelanteDerecha() {
  // Motor izquierdo a toda potencia, motor derecho suave
  digitalWrite(IN1, HIGH);
  digitalWrite(IN2, LOW);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, LOW);
}

void adelanteIzquierda() {
  // Motor derecho a toda potencia, motor izquierdo suave
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, LOW);
  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);
}

void atrasDerecha() {
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, HIGH);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, LOW);
}

void atrasIzquierda() {
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, LOW);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, HIGH);
}

void detenerAuto() {
  // Freno activo: desconecta bobinas de motores
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, LOW);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, LOW);
}

void sonarBocina() {
  tone(PIN_BUZZER, 800, 300); // 800Hz por 300ms
}
`;

  const copyCode = () => {
    navigator.clipboard.writeText(arduinoCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md">
      <div className="bg-slate-900 border border-cyan-500/40 rounded-2xl w-full max-w-2xl max-h-[90vh] flex flex-col shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-950/80">
          <div className="flex items-center gap-3">
            <Cpu className="w-6 h-6 text-cyan-400" />
            <div>
              <h2 className="text-xl font-bold font-heading text-white tracking-wide">Código Arduino Nano & HC-05</h2>
              <p className="text-xs text-slate-400">Sketch compatible con SpeedBot a 9600 Baudios</p>
            </div>
          </div>
          <button 
            onClick={onClose} 
            className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6 space-y-4 text-sm text-slate-300">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div className="bg-slate-950/60 p-3 rounded-xl border border-slate-800">
              <div className="flex items-center gap-2 text-cyan-400 font-bold mb-1">
                <Radio className="w-4 h-4" /> HC-05 Módulo
              </div>
              <p className="text-xs text-slate-400">Baud: 9600</p>
              <p className="text-xs text-slate-400">Pin default: 1234 / 0000</p>
            </div>
            <div className="bg-slate-950/60 p-3 rounded-xl border border-slate-800">
              <div className="flex items-center gap-2 text-amber-400 font-bold mb-1">
                <Zap className="w-4 h-4" /> Pines Motores
              </div>
              <p className="text-xs text-slate-400">IN1=D5, IN2=D6</p>
              <p className="text-xs text-slate-400">IN3=D9, IN4=D10</p>
            </div>
            <div className="bg-slate-950/60 p-3 rounded-xl border border-slate-800">
              <div className="flex items-center gap-2 text-emerald-400 font-bold mb-1">
                <Cpu className="w-4 h-4" /> Bocina y LED
              </div>
              <p className="text-xs text-slate-400">Bocina: Pin D11</p>
              <p className="text-xs text-slate-400">LED: Pin D12</p>
            </div>
          </div>

          <div className="relative">
            <div className="flex items-center justify-between text-xs text-slate-400 mb-2">
              <span className="font-mono">SpeedBot_Nano.ino</span>
              <button 
                onClick={copyCode}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-cyan-600/30 hover:bg-cyan-600/50 text-cyan-300 border border-cyan-500/40 text-xs font-semibold transition"
              >
                {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                {copied ? '¡Copiado!' : 'Copiar Código'}
              </button>
            </div>
            <pre className="bg-slate-950 p-4 rounded-xl border border-slate-800 text-xs font-mono text-emerald-400/90 overflow-x-auto max-h-64 scrollbar-thin">
              {arduinoCode}
            </pre>
          </div>
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-slate-800 bg-slate-950/80 flex justify-end">
          <button
            onClick={onClose}
            className="px-6 py-2.5 rounded-xl bg-cyan-600 hover:bg-cyan-500 text-slate-950 font-bold tracking-wider transition"
          >
            ENTENDIDO
          </button>
        </div>
      </div>
    </div>
  );
}
