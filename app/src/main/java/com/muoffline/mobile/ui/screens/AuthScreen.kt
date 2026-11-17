package com.muoffline.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.muoffline.mobile.R
import com.muoffline.mobile.ui.theme.*

@Composable
fun AuthScreen(
    busy: Boolean,
    errorText: String?,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onClearError: () -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var accountId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF090B11), Color(0xFF121826), Color(0xFF0B0D13))
                )
            )
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MuAbyssSlate),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_mu_crest),
                    contentDescription = stringResource(id = R.string.cd_crest),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(id = R.string.portal_title),
                style = MaterialTheme.typography.displaySmall,
                color = MuAmberBright,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(id = R.string.portal_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MuTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, MuEmerald.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WifiOff,
                        contentDescription = null,
                        tint = MuEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.offline_mode_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                color = MuAbyssSlate,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MuBorderSlate),
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MuObsidian)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val loginSelected = !isRegisterMode
                        Button(
                            onClick = {
                                isRegisterMode = false
                                onClearError()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (loginSelected) MuGold else Color.Transparent,
                                contentColor = if (loginSelected) Color(0xFF1B1202) else MuTextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("tab_login")
                        ) {
                            Text(stringResource(id = R.string.action_login))
                        }
                        Button(
                            onClick = {
                                isRegisterMode = true
                                onClearError()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!loginSelected) MuGold else Color.Transparent,
                                contentColor = if (!loginSelected) Color(0xFF1B1202) else MuTextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("tab_register")
                        ) {
                            Text(stringResource(id = R.string.action_register))
                        }
                    }

                    OutlinedTextField(
                        value = accountId,
                        onValueChange = {
                            accountId = it
                            if (errorText != null) onClearError()
                        },
                        label = { Text("ID de Cuenta") },
                        placeholder = { Text("ej. darkknight01") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = MuGold)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("account_id_input")
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (errorText != null) onClearError()
                        },
                        label = { Text("Contraseña") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = MuGold)
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (isRegisterMode) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (!isRegisterMode && !busy) {
                                    onLogin(accountId, password)
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input")
                    )

                    if (isRegisterMode) {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                if (errorText != null) onClearError()
                            },
                            label = { Text("Confirmar Contraseña") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Shield, contentDescription = null, tint = MuGold)
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (!busy) onRegister(accountId, password, confirmPassword)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_password_input")
                        )
                    }

                    if (!errorText.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MuCrimson.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = errorText,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (isRegisterMode) {
                                onRegister(accountId, password, confirmPassword)
                            } else {
                                onLogin(accountId, password)
                            }
                        },
                        enabled = !busy && accountId.isNotBlank() && password.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MuGold,
                            contentColor = Color(0xFF1B1202)
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("auth_submit_button")
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                color = Color(0xFF1B1202),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = stringResource(
                                    id = if (isRegisterMode) R.string.action_create_account else R.string.action_enter_game
                                ),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
