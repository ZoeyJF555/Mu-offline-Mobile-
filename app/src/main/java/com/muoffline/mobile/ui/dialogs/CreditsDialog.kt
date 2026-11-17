package com.muoffline.mobile.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.muoffline.mobile.R
import com.muoffline.mobile.ui.theme.*

@Composable
fun CreditsDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MuAbyssSlate,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MuGold.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Créditos y Arquitectura", style = MaterialTheme.typography.headlineSmall, color = MuAmberBright)
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_credits_dialog")) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(id = R.string.cd_close))
                    }
                }

                CreditRow(
                    title = "Mu Offline Mobile (Season 6 Ep 3)",
                    detail = "Motor Kotlin + Jetpack Compose Canvas + archivos JSON planos (sin SQL) + Multijugador Wi-Fi LAN (12 jugadores) y Bluetooth."
                )
                CreditRow(
                    title = "Inspiración Open Source MU",
                    detail = "Inspirado en el diseño clásico de Lorencia y mecánicas documentadas por las comunidades OpenMU, muonlinejs y OpenMu."
                )
                CreditRow(
                    title = "Audio Sintetizado & Controles",
                    detail = "Efectos de sonido sintetizados en tiempo real con AudioTrack, Joystick táctil virtual, MU Helper y soporte completo de Gamepad físico."
                )
            }
        }
    }
}

@Composable
private fun CreditRow(title: String, detail: String) {
    Surface(
        color = MuStoneCard,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MuBorderSlate)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MuTextPrimary)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MuTextSecondary)
        }
    }
}
