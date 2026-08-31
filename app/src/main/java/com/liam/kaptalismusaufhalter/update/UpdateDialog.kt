package com.liam.kaptalismusaufhalter.update

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.liam.kaptalismusaufhalter.R

@Composable
fun UpdateAvailableDialog(
    info: UpdateInfo,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_available_title)) },
        text = {
            Text(
                stringResource(R.string.update_available_body, info.versionName) +
                    if (info.releaseNotes.isNotBlank()) "\n\n${info.releaseNotes}" else ""
            )
        },
        confirmButton = {
            TextButton(onClick = onDownload) { Text(stringResource(R.string.update_download)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later)) }
        }
    )
}
