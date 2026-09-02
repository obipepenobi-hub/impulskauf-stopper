package com.liam.kaptalismusaufhalter.update

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.liam.kaptalismusaufhalter.R
import com.liam.kaptalismusaufhalter.ui.theme.OnPrimary
import com.liam.kaptalismusaufhalter.ui.theme.Primary
import com.liam.kaptalismusaufhalter.ui.theme.Surface as SurfaceColor

/**
 * The whole card is one tap target: tapping anywhere (except the small "Später" link)
 * starts the update immediately — there is no separate confirm step by design.
 *
 * [downloadProgress] is null while idle (tap to start) and 0f..1f while downloading, during
 * which the card shows a progress bar instead and can no longer be dismissed - the download
 * runs entirely in-app (no system DownloadManager notification), so there's nothing to leave
 * running in the background if the user backed out.
 */
@Composable
fun UpdateAvailableDialog(
    info: UpdateInfo,
    downloadProgress: Float? = null,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val downloading = downloadProgress != null
    Dialog(onDismissRequest = { if (!downloading) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = SurfaceColor,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (downloading) Modifier else Modifier.clickable(onClick = onDownload))
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = null,
                        tint = Primary
                    )
                }

                Text(
                    text = stringResource(R.string.update_available_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = stringResource(R.string.update_available_body, info.versionName),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                if (info.releaseNotes.isNotBlank()) {
                    Text(
                        text = info.releaseNotes,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (downloading) {
                    val progress = downloadProgress ?: 0f
                    Column(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = Primary
                        )
                        Text(
                            text = "${(progress * 100).toInt()} %",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(Primary)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.update_download),
                            color = OnPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = stringResource(R.string.update_later),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .clickable(onClick = onDismiss)
                    )
                }
            }
        }
    }
}
