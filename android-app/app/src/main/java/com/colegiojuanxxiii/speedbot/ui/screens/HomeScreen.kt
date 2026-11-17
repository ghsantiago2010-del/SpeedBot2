package com.colegiojuanxxiii.speedbot.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colegiojuanxxiii.speedbot.R
import com.colegiojuanxxiii.speedbot.ui.theme.CyberGridOverlay
import com.colegiojuanxxiii.speedbot.ui.theme.CyberImageBackground
import com.colegiojuanxxiii.speedbot.ui.theme.SpeedBotColors

@Composable
fun HomeScreen(
    onStart: () -> Unit,
    onExit: () -> Unit,
    onOpenGuide: () -> Unit
) {
    val creators = listOf(
        "Luca Fazio",
        "Gonzalo Fernandez",
        "Santiago Garcia Higa",
        "Joaquin Gimenez"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "homePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    CyberImageBackground(
        imageRes = R.drawable.bg_home_arena,
        brightness = 0.45f,
        gridAlpha = 0.04f
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Barra superior: SPEEDBOT + Guía Arduino
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SpeedBotColors.CardSlate.copy(alpha = 0.85f))
                        .border(1.dp, SpeedBotColors.CyanNeon.copy(alpha = 0.35f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .alpha(pulseAlpha)
                            .clip(CircleShape)
                            .background(SpeedBotColors.EmeraldLight)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SPEEDBOT",
                        color = SpeedBotColors.CyanText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                }

                OutlinedButton(
                    onClick = onOpenGuide,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, SpeedBotColors.BorderMuted),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SpeedBotColors.CardSlate.copy(alpha = 0.85f),
                        contentColor = SpeedBotColors.TextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.HelpOutline,
                        contentDescription = null,
                        tint = SpeedBotColors.CyanLight,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Guía Arduino",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Identidad central con Logo + Título + Desarrollado Por
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo circular con halo cian
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(164.dp)
                            .alpha(pulseAlpha * 0.45f)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        SpeedBotColors.CyanNeon.copy(alpha = 0.65f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(144.dp)
                            .shadow(20.dp, CircleShape, ambientColor = SpeedBotColors.CyanNeon)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        SpeedBotColors.CyanLight,
                                        SpeedBotColors.BlueAccent,
                                        SpeedBotColors.CyanNeon
                                    )
                                )
                            )
                            .padding(5.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_autorc),
                            contentDescription = "Logo SpeedBot Auto RC Arduino",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(2.dp, SpeedBotColors.CardSlate, CircleShape)
                        )
                    }
                }

                // Título SpeedBot con gradiente
                Text(
                    text = "SPEEDBOT",
                    style = TextStyle(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                SpeedBotColors.CyanText,
                                Color.White,
                                SpeedBotColors.CyanLight
                            )
                        ),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    )
                )

                Text(
                    text = "CONTROL BLUETOOTH HC-05 • ARDUINO NANO",
                    color = SpeedBotColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tarjeta Desarrollado Por: Colegio Juan XXIII + Creadores
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(SpeedBotColors.CardSlate.copy(alpha = 0.85f))
                        .border(1.dp, SpeedBotColors.CyanNeon.copy(alpha = 0.30f), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Groups,
                            contentDescription = null,
                            tint = SpeedBotColors.CyanLight,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DESARROLLADO POR:",
                            color = SpeedBotColors.CyanLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SpeedBotColors.CyanDark.copy(alpha = 0.85f))
                            .border(1.dp, SpeedBotColors.CyanLight.copy(alpha = 0.45f), CircleShape)
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "COLEGIO JUAN XXIII",
                            color = SpeedBotColors.CyanText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Grilla 2x2 de creadores
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (rowIndex in 0..1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (colIndex in 0..1) {
                                    val name = creators[rowIndex * 2 + colIndex]
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SpeedBotColors.SurfaceSlate.copy(alpha = 0.92f))
                                            .border(1.dp, SpeedBotColors.BorderSlate, RoundedCornerShape(10.dp))
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(SpeedBotColors.CyanLight)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = name,
                                            color = SpeedBotColors.TextPrimary,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Specs
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Memory,
                        contentDescription = null,
                        tint = SpeedBotColors.CyanLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "HC-05 9600 BAUD",
                        color = SpeedBotColors.TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "  •  ",
                        color = SpeedBotColors.TextDim,
                        fontSize = 11.sp
                    )
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = SpeedBotColors.EmeraldLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PEDAL 360°",
                        color = SpeedBotColors.TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Botones principales: INICIAR & SALIR
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .shadow(16.dp, RoundedCornerShape(16.dp), ambientColor = SpeedBotColors.CyanNeon)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    SpeedBotColors.CyanNeon,
                                    SpeedBotColors.BlueAccent,
                                    SpeedBotColors.CyanNeon
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(1.dp, SpeedBotColors.CyanText.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = SpeedBotColors.CardSlate,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "INICIAR",
                        color = SpeedBotColors.CardSlate,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                }

                OutlinedButton(
                    onClick = onExit,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SpeedBotColors.BorderSlate),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SpeedBotColors.CardSlate.copy(alpha = 0.90f),
                        contentColor = SpeedBotColors.TextSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = null,
                        tint = SpeedBotColors.RedLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SALIR",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ExitedScreen(
    onRestart: () -> Unit,
    onCloseApp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpeedBotColors.BgDeep)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        CyberGridOverlay(lineAlpha = 0.04f)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(SpeedBotColors.SurfaceSlate.copy(alpha = 0.96f))
                .border(2.dp, SpeedBotColors.CyanNeon.copy(alpha = 0.45f), RoundedCornerShape(26.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SpeedBotColors.CyanDark.copy(alpha = 0.85f))
                    .border(1.dp, SpeedBotColors.CyanNeon.copy(alpha = 0.45f), CircleShape)
                    .padding(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PowerSettingsNew,
                    contentDescription = null,
                    tint = SpeedBotColors.CyanLight,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "¡ADIÓS, TE BANCAMOS!",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sesión finalizada con éxito. Gracias por usar SpeedBot • Colegio Juan XXIII",
                color = SpeedBotColors.CyanText.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                SpeedBotColors.CyanNeon,
                                SpeedBotColors.BlueAccent,
                                SpeedBotColors.CyanNeon
                            )
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = SpeedBotColors.CardSlate,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REINICIAR SPEEDBOT",
                    color = SpeedBotColors.CardSlate,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onCloseApp,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SpeedBotColors.BorderMuted),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SpeedBotColors.CardSlate,
                    contentColor = SpeedBotColors.TextSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(
                    text = "CERRAR APLICACIÓN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
