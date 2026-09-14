package com.overscroll.app.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.overscroll.app.ui.theme.OverscrollPrimary
import com.overscroll.app.ui.theme.OverscrollSecondary
import com.overscroll.app.ui.theme.OverscrollSuccess
import com.overscroll.app.ui.theme.OverscrollSurfaceElevated
import com.overscroll.app.ui.theme.OverscrollTertiary
import com.overscroll.app.ui.theme.OverscrollWarning
import com.overscroll.app.ui.theme.OverscrollBackgroundTop
import com.overscroll.app.ui.theme.OverscrollBackgroundBottom

/**
 * Onboarding screen (PRD §5.1).
 *
 * Shows a scrollable list of permission cards. Each card:
 * - Explains why the permission is needed
 * - Shows live granted/not-granted status
 * - Has a button that deep-links to the relevant system settings screen
 *
 * The screen re-checks permission state every time the activity resumes
 * (i.e., when the user returns from the system settings).
 */
@Composable
fun OnboardingScreen(
    onAllPermissionsGranted: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissions by remember { mutableStateOf(checkPermissions(context)) }

    // Re-check permissions every time the activity resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissions = checkPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Notification permission launcher (API 33+)
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        permissions = checkPermissions(context)
    }

    val progress by animateFloatAsState(
        targetValue = permissions.grantedCount / 3f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "progress",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        OverscrollBackgroundTop,
                        OverscrollBackgroundBottom,
                    )
                )
            )
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Header ──
        Icon(
            imageVector = Icons.Rounded.Article,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Overscroll",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "See how many Reels you actually\nscroll through today.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))

        // ── Progress bar ──
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "${permissions.grantedCount} of 3 permissions granted",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = OverscrollPrimary,
                trackColor = OverscrollSurfaceElevated,
            )
        }

        Spacer(Modifier.height(28.dp))

        // ── Permission cards ──
        PermissionCard(
            icon = Icons.Rounded.Accessibility,
            title = "Accessibility Service",
            description = "Detects when you scroll through Reels in Instagram. Only monitors Instagram — no other apps.",
            isGranted = permissions.accessibilityEnabled,
            onGrantClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            },
        )

        Spacer(Modifier.height(16.dp))

        PermissionCard(
            icon = Icons.Rounded.Layers,
            title = "Display Over Other Apps",
            description = "Shows a floating counter bubble on top of Instagram so you can see your count in real time.",
            isGranted = permissions.overlayEnabled,
            onGrantClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            },
        )

        Spacer(Modifier.height(16.dp))

        PermissionCard(
            icon = Icons.Rounded.Notifications,
            title = "Notifications",
            description = "Shows your live Reels count in the status bar notification.",
            isGranted = permissions.notificationEnabled,
            onGrantClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                // Pre-13: notifications are enabled by default, card won't show "Grant"
            },
        )

        Spacer(Modifier.height(32.dp))

        // ── Continue button (appears when all granted) ──
        AnimatedVisibility(
            visible = permissions.allGranted,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = onAllPermissionsGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF1E1E24), // Dark rounded button
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                ) {
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                
                Spacer(Modifier.height(12.dp))
                
                Text(
                    text = "Skip for now",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onAllPermissionsGranted() }.padding(8.dp)
                )
            }
        }

        // ── Known limitations note ──
        if (permissions.allGranted) {
            Spacer(Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = OverscrollSurfaceElevated.copy(alpha = 0.7f),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = OverscrollWarning
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Good to know",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Scroll counting is a best-effort heuristic. Fast swipes, replays, and pre-loaded content may cause slight over- or under-counting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit,
) {
    val cardColor by animateColorAsState(
        targetValue = if (isGranted) {
            OverscrollSuccess.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        label = "cardColor",
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isGranted) 1.1f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "iconScale",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Icon badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .scale(iconScale)
                    .clip(CircleShape)
                    .background(
                        if (isGranted) OverscrollSuccess.copy(alpha = 0.15f)
                        else OverscrollPrimary.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Rounded.Check else icon,
                    contentDescription = null,
                    tint = if (isGranted) OverscrollSuccess else OverscrollPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (isGranted) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = OverscrollSuccess,
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (!isGranted) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onGrantClick,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = OverscrollPrimary,
                        ),
                    ) {
                        Text(
                            text = "Grant Permission",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}
