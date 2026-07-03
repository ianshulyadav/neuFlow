package com.codetrio.spatialflow.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.codetrio.spatialflow.ui.components.SpatialFlowCard
import com.codetrio.spatialflow.ui.theme.CardCorners
import com.codetrio.spatialflow.ui.theme.GoogleSansRounded
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

enum class SetupStep(val icon: ImageVector, val title: String, val description: String, val actionLabel: String) {
    PERMISSIONS(Icons.Rounded.FolderOpen, "Grant Storage Access", "SpatialFlow needs access to your music files to build your library. Everything stays on your device.", "Grant Permission"),
    LIBRARY_SCAN(Icons.Rounded.LibraryMusic, "Scan Your Library", "We'll find all your music files. This runs entirely on-device and only takes a moment.", "Start Scan"),
    THEME(Icons.Rounded.Palette, "Choose Your Look", "Pick a theme that matches your style. You can always change this in Settings.", "Customize"),
    YOUTUBE_CONNECT(Icons.Rounded.Link, "Connect YouTube Music", "Link your account to stream and explore millions of songs.", "Connect"),
    DONE(Icons.Rounded.CheckCircle, "You're All Set!", "Your library is ready. Start exploring, playing, and enjoying your music.", "Let's Go")
}

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(SetupStep.PERMISSIONS) }
    var progress by remember { mutableStateOf(0f) }
    
    // Simulate scan when library scan step is active and action is clicked
    LaunchedEffect(currentStep) {
        if (currentStep == SetupStep.LIBRARY_SCAN) {
            progress = 0.1f
            while (progress < 1.0f) {
                kotlinx.coroutines.delay(150)
                progress += 0.15f
            }
            progress = 1.0f
            kotlinx.coroutines.delay(300)
            currentStep = SetupStep.THEME
        }
    }

    OnboardingScreen(
        currentStep = currentStep,
        onStepAction = { step ->
            when (step) {
                SetupStep.PERMISSIONS -> {
                    // Simple transition to scanning. 
                    // In a production app, request Android storage/audio permission here.
                    currentStep = SetupStep.LIBRARY_SCAN
                }
                SetupStep.LIBRARY_SCAN -> {
                    // Triggers the LaunchedEffect scanning simulation
                }
                SetupStep.THEME -> {
                    currentStep = SetupStep.YOUTUBE_CONNECT
                }
                SetupStep.YOUTUBE_CONNECT -> {
                    currentStep = SetupStep.DONE
                }
                SetupStep.DONE -> {
                    onComplete()
                }
            }
        },
        onSkip = onComplete,
        progress = progress
    )
}

@Composable
fun OnboardingScreen(
    currentStep: SetupStep,
    onStepAction: (SetupStep) -> Unit,
    onSkip: () -> Unit,
    progress: Float = 0f
) {
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        containerColor = scheme.background,
        topBar = {
            if (currentStep != SetupStep.DONE) {
                TopAppBar(title = {}, actions = { TextButton(onClick = onSkip) { Text("Skip", color = scheme.onSurfaceVariant, fontFamily = GoogleSansRounded) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            AnimatedContent(currentStep, transitionSpec = { fadeIn(tween(400)) + scaleIn(tween(400)) togetherWith fadeOut(tween(200)) + scaleOut(tween(200)) }, label = "stepIcon") { step ->
                Box(Modifier.size(120.dp).clip(CircleShape).background(Brush.radialGradient(listOf(scheme.primaryContainer, scheme.primaryContainer.copy(alpha = 0.2f)))), contentAlignment = Alignment.Center) {
                    Icon(imageVector = step.icon, contentDescription = step.title, modifier = Modifier.size(56.dp), tint = scheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.height(32.dp))
            AnimatedContent(currentStep, transitionSpec = { fadeIn(tween(300, delayMillis = 200)) togetherWith fadeOut(tween(150)) }, label = "stepText") { step ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(step.title, style = MaterialTheme.typography.headlineMedium, fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold, color = scheme.onBackground, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(step.description, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(40.dp))
            SpatialFlowCard(onClick = { onStepAction(currentStep) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(currentStep.actionLabel, style = MaterialTheme.typography.titleMedium, fontFamily = GoogleSansRounded, fontWeight = FontWeight.SemiBold, color = scheme.primary)
                    Icon(Icons.Rounded.ArrowForward, null, tint = scheme.primary)
                }
            }
            if (currentStep == SetupStep.LIBRARY_SCAN && progress > 0f) {
                Spacer(Modifier.height(24.dp))
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator({ progress }, Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)), color = scheme.primary, trackColor = scheme.surfaceContainerHighest)
                    Spacer(Modifier.height(8.dp))
                    Text("${(progress * 100).toInt()}% complete", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun PermissionCard(title: String, description: String, icon: ImageVector, isGranted: Boolean, onRequest: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val shape = AbsoluteSmoothCornerShape(CardCorners.RadiusDp.dp, CardCorners.Smoothness, CardCorners.RadiusDp.dp, CardCorners.Smoothness, CardCorners.RadiusDp.dp, CardCorners.Smoothness, CardCorners.RadiusDp.dp, CardCorners.Smoothness)
    Card(modifier.fillMaxWidth().padding(horizontal = 16.dp), shape, CardDefaults.cardColors(if (isGranted) scheme.primaryContainer.copy(alpha = 0.3f) else scheme.surfaceContainerHigh), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(scheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(24.dp), tint = scheme.onPrimaryContainer) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontFamily = GoogleSansRounded, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                Text(description, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            if (isGranted) Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = "Granted", modifier = Modifier.size(28.dp), tint = scheme.primary)
            else FilledTonalButton(onClick = onRequest, shape = RoundedCornerShape(20.dp)) { Text("Grant") }
        }
    }
}
