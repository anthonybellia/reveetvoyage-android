package be.reveetvoyage.app.ui.screens.devis_wizard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import be.reveetvoyage.app.ui.components.IOSButton
import be.reveetvoyage.app.ui.components.IOSButtonStyle
import be.reveetvoyage.app.ui.theme.*

/**
 * Wizard 7 étapes "Demander un voyage". Rendu en plein écran via [Dialog]
 * pour pouvoir être lancé depuis n'importe quel écran sans modifier la
 * NavHost globale.
 *
 * @param onClose ferme le wizard (retour à l'écran précédent)
 * @param onOpenVoyages action optionnelle appelée depuis l'écran de succès
 *                     (par défaut équivalent à [onClose])
 */
@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
fun DevisWizardScreen(
    onClose: () -> Unit,
    onOpenVoyages: () -> Unit = onClose,
    vm: DevisWizardViewModel = hiltViewModel(),
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        DevisWizardContent(
            vm = vm,
            onClose = onClose,
            onOpenVoyages = onOpenVoyages,
        )
    }
}

@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
private fun DevisWizardContent(
    vm: DevisWizardViewModel,
    onClose: () -> Unit,
    onOpenVoyages: () -> Unit,
) {
    val draft by vm.draft.collectAsState()
    val stepIndex by vm.stepIndex.collectAsState()
    val submitState by vm.submitState.collectAsState()
    val user by vm.currentUser.collectAsState()
    val step = WizardStep.fromIndex(stepIndex)
    val snackbarHostState = remember { SnackbarHostState() }

    // Surface error from submit as snackbar
    LaunchedEffect(submitState) {
        if (submitState is SubmitState.Error) {
            snackbarHostState.showSnackbar((submitState as SubmitState.Error).message)
            vm.consumeError()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RevBackground,
    ) {
        if (submitState is SubmitState.Success) {
            SuccessScreen(
                onOpenVoyages = onOpenVoyages,
                onClose = {
                    vm.reset()
                    onClose()
                },
            )
            return@Surface
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(RevYellow.copy(alpha = 0.10f), RevBackground)
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                // Top bar with back/close + progress
                WizardTopBar(
                    stepIndex = stepIndex,
                    onClose = onClose,
                    onBack = if (stepIndex == 0) null else { { vm.previous() } },
                )

                // Step content with horizontal slide animation
                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = stepIndex,
                        transitionSpec = {
                            val forward = targetState > initialState
                            (slideInHorizontally(animationSpec = tween(280)) {
                                if (forward) it else -it
                            } + fadeIn(animationSpec = tween(220))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(280)) {
                                    if (forward) -it else it
                                } + fadeOut(animationSpec = tween(200)))
                        },
                        label = "wizardStep",
                    ) { idx ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp)
                                .padding(top = 8.dp, bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            when (WizardStep.fromIndex(idx)) {
                                WizardStep.Identite -> Step1IdentiteScreen(draft, user, vm::update)
                                WizardStep.Voyageurs -> Step2VoyageursScreen(draft, vm)
                                WizardStep.Dates -> Step3DatesScreen(draft, vm::update)
                                WizardStep.Destination -> Step4DestinationScreen(draft, vm)
                                WizardStep.Sejour -> Step5SejourScreen(draft, vm::update)
                                WizardStep.Activites -> Step6ActivitesScreen(draft, vm::update)
                                WizardStep.Details -> Step7DetailsScreen(draft, vm::update)
                            }
                        }
                    }
                }

                // Bottom action bar
                BottomActionBar(
                    step = step,
                    canAdvance = vm.canAdvance(step, user),
                    isSubmitting = submitState is SubmitState.Submitting,
                    onNext = { vm.next() },
                    onSubmit = { vm.submit() },
                    onBack = if (stepIndex == 0) null else { { vm.previous() } },
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp),
            )
        }
    }
}

@Composable
private fun WizardTopBar(
    stepIndex: Int,
    onClose: () -> Unit,
    onBack: (() -> Unit)?,
) {
    val total = WizardStep.TOTAL
    val progress = (stepIndex + 1f) / total
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 350),
        label = "progress",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = RevBrown,
                    )
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Demande de voyage",
                    color = RevBrown,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Text(
                    "Étape ${stepIndex + 1} sur $total",
                    color = RevTextSecondary,
                    fontSize = 11.sp,
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Fermer", tint = RevBrown)
            }
        }
        Spacer(Modifier.height(8.dp))
        @Suppress("DEPRECATION")
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = RevOrange,
            trackColor = Color(0x14000000),
        )
    }
}

@Composable
private fun BottomActionBar(
    step: WizardStep,
    canAdvance: Boolean,
    isSubmitting: Boolean,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
    onBack: (() -> Unit)?,
) {
    val isLast = step == WizardStep.Details
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = RevBackground,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HorizontalDivider(color = Color(0x0F000000), thickness = 0.5.dp)
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onBack != null) {
                    IOSButton(
                        text = "Retour",
                        onClick = onBack,
                        style = IOSButtonStyle.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                }
                IOSButton(
                    text = if (isLast) "Envoyer à Matilda" else "Continuer",
                    onClick = if (isLast) onSubmit else onNext,
                    style = IOSButtonStyle.Primary,
                    modifier = Modifier.weight(2f),
                    enabled = canAdvance,
                    isLoading = isSubmitting,
                )
            }
        }
    }
}

@Composable
private fun SuccessScreen(
    onOpenVoyages: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(RevYellow.copy(alpha = 0.18f), RevBackground)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(RevYellow, RevOrange, RevRed))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp),
                )
            }
            Text(
                "Matilda a reçu votre demande",
                color = RevBrown,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                "Notre équipe vous contactera très bientôt pour préparer un voyage sur mesure.",
                color = RevTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            IOSButton(
                text = "Voir mes voyages",
                onClick = onOpenVoyages,
                style = IOSButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
            IOSButton(
                text = "Fermer",
                onClick = onClose,
                style = IOSButtonStyle.Ghost,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
