package io.github.cxpexporter.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import io.github.cxpexporter.R

@Composable
fun MasterPasswordDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    // Plain remember on purpose: secrets must not be written to saved-state bundles.
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var mismatch by remember { mutableStateOf(false) }
    val transformation = if (visible) VisualTransformation.None else PasswordVisualTransformation()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_master_password_title)) },
        text = {
            Column {
                Text(stringResource(R.string.dialog_master_password_text))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        mismatch = false
                    },
                    label = { Text(stringResource(R.string.label_master_password)) },
                    singleLine = true,
                    visualTransformation = transformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = stringResource(R.string.content_toggle_visibility),
                            )
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = {
                        confirm = it
                        mismatch = false
                    },
                    label = { Text(stringResource(R.string.label_confirm_password)) },
                    singleLine = true,
                    visualTransformation = transformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                if (mismatch) {
                    Text(
                        stringResource(R.string.error_passwords_mismatch),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = password.isNotEmpty() && confirm.isNotEmpty(),
                onClick = {
                    if (password == confirm) onConfirm(password) else mismatch = true
                },
            ) { Text(stringResource(R.string.action_export)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
fun UnencryptedExportDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_unencrypted_title)) },
        text = { Text(stringResource(R.string.dialog_unencrypted_text)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_export)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
fun PasskeyInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.passkey_info_title)) },
        text = { Text(stringResource(R.string.passkey_info_text)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) }
        },
    )
}
