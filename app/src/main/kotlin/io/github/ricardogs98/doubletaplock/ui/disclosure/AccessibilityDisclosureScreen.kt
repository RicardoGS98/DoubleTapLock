package io.github.ricardogs98.doubletaplock.ui.disclosure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.ricardogs98.doubletaplock.R

/**
 * Full-screen prominent disclosure shown before redirecting the user to system Accessibility
 * Settings. Renders the five points required by Google Play's prominent-disclosure guidance for
 * apps that use AccessibilityService without declaring `isAccessibilityTool="true"`.
 *
 * Stateless. No persistence, no analytics, no side effects beyond invoking [onContinue] /
 * [onCancel]. The hosting screen decides what those do.
 */
@Composable
fun AccessibilityDisclosureScreen(
    onContinue: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            bottomBar = {
                DisclosureActions(onContinue = onContinue, onCancel = onCancel)
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.disclosure_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.disclosure_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
                HorizontalDivider()

                DisclosurePoint(
                    title = stringResource(R.string.disclosure_point_1_title),
                    body = stringResource(R.string.disclosure_point_1_body)
                )
                DisclosurePoint(
                    title = stringResource(R.string.disclosure_point_2_title),
                    body = stringResource(R.string.disclosure_point_2_body)
                )
                DisclosurePoint(
                    title = stringResource(R.string.disclosure_point_3_title),
                    body = stringResource(R.string.disclosure_point_3_body)
                )
                DisclosurePoint(
                    title = stringResource(R.string.disclosure_point_4_title),
                    body = stringResource(R.string.disclosure_point_4_body)
                )
                DisclosurePoint(
                    title = stringResource(R.string.disclosure_point_5_title),
                    body = stringResource(R.string.disclosure_point_5_body)
                )
                RepoLink()
            }
        }
    }
}

@Composable
private fun RepoLink() {
    val uriHandler = LocalUriHandler.current
    val url = stringResource(R.string.disclosure_repo_url)
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.disclosure_repo_link_label),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .clickable { uriHandler.openUri(url) }
            .padding(vertical = 4.dp)
    )
}

@Composable
private fun DisclosurePoint(title: String, body: String) {
    Spacer(Modifier.height(20.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun DisclosureActions(onContinue: () -> Unit, onCancel: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text(stringResource(R.string.disclosure_cancel))
            }
            Button(
                onClick = onContinue,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text(stringResource(R.string.disclosure_continue))
            }
        }
    }
}

