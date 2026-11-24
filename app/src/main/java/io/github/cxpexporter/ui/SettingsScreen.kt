package io.github.cxpexporter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.cxpexporter.R

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    updateCheckEnabled: Boolean,
    checking: Boolean,
    onUpdateCheckEnabledChange: (Boolean) -> Unit,
    onCheckNow: () -> Unit,
) {
    val switchLabel = stringResource(R.string.settings_update_check)
    val explanation = stringResource(R.string.settings_update_explanation)
    val checkNowLabel = stringResource(R.string.settings_check_now)

    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(
                    value = updateCheckEnabled,
                    role = Role.Switch,
                    onValueChange = onUpdateCheckEnabledChange,
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = switchLabel,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 16.dp),
            )
            Switch(checked = updateCheckEnabled, onCheckedChange = null)
        }
        Text(
            text = explanation,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onCheckNow, enabled = !checking) {
            Text(checkNowLabel)
        }
    }
}
