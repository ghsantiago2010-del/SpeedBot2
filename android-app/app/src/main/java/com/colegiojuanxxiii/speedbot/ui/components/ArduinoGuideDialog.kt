package com.colegiojuanxxiii.speedbot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.colegiojuanxxiii.speedbot.ui.theme.SpeedBotColors
import kotlinx.coroutines.delay

private const val ARDUINO_CODE_SNIPPET = """/*
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
  digitalWrite(IN1, HIGH);
  digitalWrite(IN2, LOW);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, LOW);
}

void adelanteIzquierda() {
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
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, LOW);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, LOW);
}

void sonarBocina() {
  tone(PIN_BUZZER, 800, 300);
}"""

@Composable
fun ArduinoGuideDialog(
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SpeedBotColors.SurfaceSlate)
                .border(1.dp, SpeedBotColors.CyanNeon.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpeedBotColors.CardSlate.copy(alpha = 0.9f))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Filled.Memory,
                        contentDescription = null,
                        tint = SpeedBotColors.CyanLight,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Código Arduino Nano & HC-05",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sketch compatible con SpeedBot a 9600 Baudios",
                            color = SpeedBotColors.TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cerrar",
                        tint = SpeedBotColors.TextMuted
                    )
                }
            }

            // Contenido scrollable
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GuidePinCard(
                    icon = Icons.Filled.Radio,
                    title = "HC-05 Módulo",
                    titleColor = SpeedBotColors.CyanLight,
                    line1 = "Baud: 9600 • RX=Pin 3, TX=Pin 2",
                    line2 = "PIN por defecto: 1234 / 0000"
                )
                GuidePinCard(
                    icon = Icons.Filled.Bolt,
                    title = "Pines Motores (Puente H)",
                    titleColor = SpeedBotColors.AmberText,
                    line1 = "IN1=D5, IN2=D6",
                    line2 = "IN3=D9, IN4=D10"
                )
                GuidePinCard(
                    icon = Icons.Filled.Memory,
                    title = "Bocina y LED",
                    titleColor = SpeedBotColors.EmeraldLight,
                    line1 = "Bocina: Pin D11",
                    line2 = "LED: Pin D12"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SpeedBot_Nano.ino",
                        color = SpeedBotColors.TextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(ARDUINO_CODE_SNIPPET))
                            copied = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpeedBotColors.CyanDark,
                            contentColor = SpeedBotColors.CyanText
                        )
                    ) {
                        Icon(
                            imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                            contentDescription = null,
                            tint = if (copied) SpeedBotColors.EmeraldLight else SpeedBotColors.CyanText,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (copied) "¡Copiado!" else "Copiar Código",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SpeedBotColors.CardSlate)
                        .border(1.dp, SpeedBotColors.BorderSlate, RoundedCornerShape(12.dp))
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(
                        text = ARDUINO_CODE_SNIPPET,
                        color = SpeedBotColors.EmeraldLight.copy(alpha = 0.92f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }

            // Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpeedBotColors.CardSlate.copy(alpha = 0.9f))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpeedBotColors.CyanNeon,
                        contentColor = SpeedBotColors.CardSlate
                    )
                ) {
                    Text(
                        text = "ENTENDIDO",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GuidePinCard(
    icon: ImageVector,
    title: String,
    titleColor: Color,
    line1: String,
    line2: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SpeedBotColors.CardSlate.copy(alpha = 0.7f))
            .border(1.dp, SpeedBotColors.BorderSlate, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = titleColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = titleColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = line1, color = SpeedBotColors.TextMuted, fontSize = 12.sp)
        Text(text = line2, color = SpeedBotColors.TextMuted, fontSize = 12.sp)
    }
}
