package com.muoffline.mobile.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.muoffline.mobile.R
import com.muoffline.mobile.model.MobileHudSettings
import com.muoffline.mobile.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun SettingsDialog(
    settings: MobileHudSettings,
    canEditHudLayout: Boolean = false,
    onStartHudLayoutEdit: () -> Unit = {},
    onResetHudLayout: () -> Unit = {},
    onUpdateSettings: ((MobileHudSettings) -> MobileHudSettings) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MuAbyssSlate,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MuGold.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 580.dp)
                .heightIn(max = 640.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Configuración y Controles",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MuAmberBright
                        )
                        Text(
                            text = "Ajustes guardados localmente en settings.json",
                            style = MaterialTheme.typography.bodySmall,
                            color = MuTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_settings_dialog")) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(id = R.string.cd_close))
                    }
                }

                // Audio sliders
                Surface(
                    color = MuStoneCard,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MuBorderSlate)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Audio del Juego", style = MaterialTheme.typography.titleMedium, color = MuAmberBright)
                        Text(
                            text = "Efectos (SFX): ${(settings.sfxVolume * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MuTextPrimary
                        )
                        Slider(
                            value = settings.sfxVolume,
                            onValueChange = { v -> onUpdateSettings { it.copy(sfxVolume = v) } },
                            valueRange = 0f..1f,
                            modifier = Modifier.testTag("slider_sfx_volume")
                        )

                        Text(
                            text = "Música Ambiental: ${(settings.musicVolume * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MuTextPrimary
                        )
                        Slider(
                            value = settings.musicVolume,
                            onValueChange = { v -> onUpdateSettings { it.copy(musicVolume = v) } },
                            valueRange = 0f..1f,
                            modifier = Modifier.testTag("slider_music_volume")
                        )
                    }
                }

                // Graphics & Screen
                Surface(
                    color = MuStoneCard,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MuBorderSlate)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Gráficos y Pantalla", style = MaterialTheme.typography.titleMedium, color = MuSky)
                        Text(
                            text = "Brillo de Pantalla: ${(settings.brightness * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MuTextPrimary
                        )
                        Slider(
                            value = settings.brightness,
                            onValueChange = { v -> onUpdateSettings { it.copy(brightness = v) } },
                            valueRange = 0.4f..1f,
                            modifier = Modifier.testTag("slider_brightness")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Calidad Gráfica HD", style = MaterialTheme.typography.bodyMedium, color = MuTextPrimary)
                                Text(
                                    text = if (settings.hdGraphics) "HD Full (rejilla y antorchas)" else "Modo Rendimiento (gama baja)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MuTextSecondary
                                )
                            }
                            Switch(
                                checked = settings.hdGraphics,
                                onCheckedChange = { checked -> onUpdateSettings { it.copy(hdGraphics = checked) } },
                                modifier = Modifier.testTag("switch_hd_graphics")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Animaciones y VFX", style = MaterialTheme.typography.bodyMedium, color = MuTextPrimary)
                                Text(
                                    text = if (settings.animationsEnabled) "Partículas y aleteo activos" else "Efectos reducidos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MuTextSecondary
                                )
                            }
                            Switch(
                                checked = settings.animationsEnabled,
                                onCheckedChange = { checked -> onUpdateSettings { it.copy(animationsEnabled = checked) } },
                                modifier = Modifier.testTag("switch_animations")
                            )
                        }
                    }
                }

                // Touch HUD layout + Custom Button Positioner
                Surface(
                    color = MuStoneCard,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MuBorderSlate)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Botones Táctiles, Inventario y Trade (HUD)", style = MaterialTheme.typography.titleMedium, color = MuEmerald)
                        Text(
                            text = "Tamaño Joystick y Skills: ${(settings.hudScale * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MuTextPrimary
                        )
                        Slider(
                            value = settings.hudScale,
                            onValueChange = { v -> onUpdateSettings { it.copy(hudScale = v) } },
                            valueRange = 0.8f..1.3f,
                            modifier = Modifier.testTag("slider_hud_scale")
                        )

                        Text(
                            text = "Altura inferior HUD: ${settings.hudBottomPaddingDp.roundToInt()} dp",
                            style = MaterialTheme.typography.bodySmall,
                            color = MuTextPrimary
                        )
                        Slider(
                            value = settings.hudBottomPaddingDp,
                            onValueChange = { v -> onUpdateSettings { it.copy(hudBottomPaddingDp = v) } },
                            valueRange = 0f..48f,
                            modifier = Modifier.testTag("slider_hud_bottom")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onDismiss()
                                    onStartHudLayoutEdit()
                                },
                                enabled = canEditHudLayout,
                                colors = ButtonDefaults.buttonColors(containerColor = MuGold, contentColor = Color(0xFF1B1202)),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("edit_hud_layout_button")
                            ) {
                                Icon(Icons.Rounded.OpenWith, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (canEditHudLayout) "Acomodar Botones" else "Entra al mapa para acomodar")
                            }
                            OutlinedButton(
                                onClick = onResetHudLayout,
                                border = BorderStroke(1.dp, MuBorderSlate),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("reset_hud_positions_button")
                            ) {
                                Text("Centrar", color = MuSky)
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        onUpdateSettings {
                            MobileHudSettings(manifestUrl = it.manifestUrl)
                        }
                    },
                    border = BorderStroke(1.dp, MuBorderSlate),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("reset_settings_button")
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = MuAmberBright)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restaurar Valores por Defecto", color = Color(0xFFFDE68A))
                }
            }
        }
    }
}
