/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pedronveloso.a11ybutton.R
import com.pedronveloso.a11ybutton.ui.MainScreenState
import com.pedronveloso.a11ybutton.ui.preview.ThemePreviews
import com.pedronveloso.a11ybutton.ui.theme.A11YButtonTheme
import com.pedronveloso.a11ybutton.ui.theme.a11YButtonStatusPalette

const val ONBOARDING_PRIMARY_BUTTON_TAG = "onboarding_primary_button"
const val ONBOARDING_SKIP_BUTTON_TAG = "onboarding_skip_button"
const val ONBOARDING_BACK_BUTTON_TAG = "onboarding_back_button"
const val ONBOARDING_SKIP_ACTION_BUTTON_TAG = "onboarding_skip_action_button"

/**
 * First-run setup, one requirement per step. The current [step] is owned by the caller so it
 * survives leaving for the picker or system settings. Steps that were not done when first shown
 * advance on their own once the requirement is met.
 */
@Composable
fun OnboardingScreen(
    screenState: MainScreenState,
    step: OnboardingStep,
    onStepChange: (OnboardingStep) -> Unit,
    onAcceptDisclosure: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onChooseApp: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val isDone = step.isDone(screenState)
  val previous = step.previous
  val next = step.next
  val isLastStep = next == null
  val isComplete = isLastStep && isDone
  var showSkipConfirmation by rememberSaveable { mutableStateOf(false) }

  BackHandler(enabled = previous != null) { previous?.let(onStepChange) }

  val wasDoneOnEntry = rememberSaveable(step) { isDone }
  LaunchedEffect(step, isDone) { if (isDone && !wasDoneOnEntry && next != null) onStepChange(next) }

  val progress by
      animateFloatAsState(
          targetValue = if (isComplete) 1f else (step.ordinal + 1f) / OnboardingStep.entries.size,
          label = "onboarding_progress",
      )

  Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
    Column(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 24.dp),
    ) {
      Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
      ) {
        Text(
            text =
                stringResource(
                    R.string.onboarding_step_counter,
                    step.ordinal + 1,
                    OnboardingStep.entries.size,
                ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!isLastStep) {
          TextButton(
              onClick = { showSkipConfirmation = true },
              modifier = Modifier.testTag(ONBOARDING_SKIP_BUTTON_TAG),
          ) {
            Text(text = stringResource(R.string.onboarding_skip))
          }
        }
      }
      LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier.fillMaxWidth(),
      )

      AnimatedContent(
          targetState = step,
          transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            val direction = if (forward) 1 else -1
            (slideInHorizontally { width -> direction * width / 4 } + fadeIn()) togetherWith
                (slideOutHorizontally { width -> -direction * width / 4 } + fadeOut())
          },
          label = "onboarding_step",
          modifier = Modifier.weight(1f).fillMaxWidth(),
      ) { displayedStep ->
        val displayedDone = displayedStep.isDone(screenState)
        StepBody(
            step = displayedStep,
            isDone = displayedDone,
            isComplete = displayedStep.next == null && displayedDone,
            serviceMenuHint = screenState.serviceMenuHint,
            showXiaomiNote =
                screenState.backgroundProtection.requiresRecentsLock &&
                    !screenState.backgroundProtection.recentsLockConfirmed,
        )
      }

      Column(
          verticalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.padding(bottom = 16.dp),
      ) {
        Button(
            onClick = {
              when {
                isComplete -> onFinish()
                isDone -> next?.let(onStepChange)
                else ->
                    when (step) {
                      OnboardingStep.Welcome -> onAcceptDisclosure()
                      OnboardingStep.Service -> onOpenAccessibilitySettings()
                      OnboardingStep.ButtonAction -> onChooseApp()
                      OnboardingStep.Battery -> onRequestBatteryExemption()
                    }
              }
            },
            modifier = Modifier.fillMaxWidth().testTag(ONBOARDING_PRIMARY_BUTTON_TAG),
        ) {
          Text(text = stringResource(primaryLabel(step, isDone)))
        }
        if (isLastStep && !isDone) {
          TextButton(
              onClick = onFinish,
              modifier = Modifier.fillMaxWidth().testTag(ONBOARDING_SKIP_ACTION_BUTTON_TAG),
          ) {
            Text(text = stringResource(R.string.onboarding_skip_for_now))
          }
        }
        if (previous != null) {
          TextButton(
              onClick = { onStepChange(previous) },
              modifier = Modifier.fillMaxWidth().testTag(ONBOARDING_BACK_BUTTON_TAG),
          ) {
            Text(text = stringResource(R.string.onboarding_back))
          }
        }
      }
    }
  }
  if (showSkipConfirmation) {
    AlertDialog(
        onDismissRequest = { showSkipConfirmation = false },
        title = { Text(text = stringResource(R.string.onboarding_skip_confirmation_title)) },
        text = { Text(text = stringResource(R.string.onboarding_skip_confirmation_body)) },
        confirmButton = {
          TextButton(
              onClick = {
                showSkipConfirmation = false
                onFinish()
              },
              colors =
                  ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
          ) {
            Text(text = stringResource(R.string.onboarding_skip_for_now))
          }
        },
        dismissButton = {
          TextButton(onClick = { showSkipConfirmation = false }) {
            Text(text = stringResource(R.string.onboarding_skip_cancel))
          }
        },
    )
  }
}

@Composable
private fun StepBody(
    step: OnboardingStep,
    isDone: Boolean,
    isComplete: Boolean,
    serviceMenuHint: ServiceMenuHint,
    showXiaomiNote: Boolean,
) {
  val palette = a11YButtonStatusPalette()
  Column(
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 40.dp),
  ) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier.size(72.dp)
                .background(
                    color =
                        if (isComplete) palette.positiveContainer
                        else MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                ),
    ) {
      Icon(
          imageVector = if (isComplete) Icons.Filled.CheckCircle else stepIcon(step),
          contentDescription = null,
          tint =
              if (isComplete) palette.positiveContent
              else MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(36.dp),
      )
    }
    Text(
        text =
            stringResource(if (isComplete) R.string.onboarding_complete_title else titleRes(step)),
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.semantics { heading() },
    )
    Text(
        // Strings carry <b> tags escaped as entities, since getString() drops real markup.
        text =
            AnnotatedString.fromHtml(
                stringResource(
                    if (isComplete) R.string.onboarding_complete_body
                    else bodyRes(step, serviceMenuHint)
                )
            ),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (step == OnboardingStep.Welcome) {
      AccessibilityButtonExamples()
      Text(
          text = stringResource(R.string.main_disclosure_body),
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(bottom = 8.dp),
      )
    }
    if (isComplete && showXiaomiNote) {
      Text(
          text = stringResource(R.string.onboarding_complete_xiaomi),
          style = MaterialTheme.typography.bodyMedium,
      )
    }
    if (isDone && !isComplete) {
      Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = palette.positiveContent,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.status_state_done),
            style = MaterialTheme.typography.titleSmall,
            color = palette.positiveContent,
        )
      }
    }
  }
}

@Composable
private fun AccessibilityButtonExamples() {
  Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(24.dp),
      modifier = Modifier.fillMaxWidth(),
  ) {
    ButtonExample(
        imageRes = R.drawable.onboarding_navigation_bar_button,
        captionRes = R.string.onboarding_navigation_bar_caption,
        descriptionRes = R.string.onboarding_navigation_bar_description,
        maxWidth = 320.dp,
    )
    ButtonExample(
        imageRes = R.drawable.onboarding_floating_button,
        captionRes = R.string.onboarding_floating_caption,
        descriptionRes = R.string.onboarding_floating_description,
        maxWidth = 124.dp,
        croppedAspectRatio = 272f / 260f,
    )
  }
}

@Composable
private fun ButtonExample(
    @DrawableRes imageRes: Int,
    @StringRes captionRes: Int,
    @StringRes descriptionRes: Int,
    maxWidth: Dp,
    croppedAspectRatio: Float? = null,
) {
  val painter = painterResource(imageRes)
  Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Image(
        painter = painter,
        contentDescription = stringResource(descriptionRes),
        contentScale = ContentScale.Crop,
        modifier =
            Modifier.widthIn(max = maxWidth)
                .fillMaxWidth()
                .aspectRatio(
                    croppedAspectRatio ?: painter.intrinsicSize.width / painter.intrinsicSize.height
                )
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithCache {
                  val stops =
                      arrayOf(
                          0f to Color.Transparent,
                          0.12f to Color.Black,
                          0.88f to Color.Black,
                          1f to Color.Transparent,
                      )
                  val horizontal = Brush.horizontalGradient(*stops, endX = size.width)
                  val vertical = Brush.verticalGradient(*stops, endY = size.height)
                  onDrawWithContent {
                    drawContent()
                    drawRect(horizontal, blendMode = BlendMode.DstIn)
                    drawRect(vertical, blendMode = BlendMode.DstIn)
                  }
                },
    )
    Text(
        text = stringResource(captionRes),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

private fun stepIcon(step: OnboardingStep): ImageVector =
    when (step) {
      OnboardingStep.Welcome -> Icons.Filled.TouchApp
      OnboardingStep.Service -> Icons.Filled.Accessibility
      OnboardingStep.ButtonAction -> Icons.Filled.Apps
      OnboardingStep.Battery -> Icons.Filled.BatteryChargingFull
    }

private fun titleRes(step: OnboardingStep): Int =
    when (step) {
      OnboardingStep.Welcome -> R.string.onboarding_welcome_title
      OnboardingStep.Service -> R.string.onboarding_service_title
      OnboardingStep.ButtonAction -> R.string.onboarding_action_title
      OnboardingStep.Battery -> R.string.onboarding_battery_title
    }

private fun bodyRes(step: OnboardingStep, serviceMenuHint: ServiceMenuHint): Int =
    when (step) {
      OnboardingStep.Welcome -> R.string.onboarding_welcome_body
      OnboardingStep.Service ->
          when (serviceMenuHint) {
            ServiceMenuHint.Xiaomi -> R.string.onboarding_service_body_xiaomi
            ServiceMenuHint.Samsung -> R.string.onboarding_service_body_samsung
            ServiceMenuHint.Pixel -> R.string.onboarding_service_body_pixel
            ServiceMenuHint.Other -> R.string.onboarding_service_body
          }
      OnboardingStep.ButtonAction -> R.string.onboarding_action_body
      OnboardingStep.Battery -> R.string.onboarding_battery_body
    }

private fun primaryLabel(
    step: OnboardingStep,
    isDone: Boolean,
): Int =
    when {
      step.next == null && isDone -> R.string.onboarding_finish
      isDone -> R.string.onboarding_continue
      else ->
          when (step) {
            OnboardingStep.Welcome -> R.string.onboarding_welcome_action
            OnboardingStep.Service -> R.string.main_action_open_settings
            OnboardingStep.ButtonAction -> R.string.home_selected_app_choose
            OnboardingStep.Battery -> R.string.background_protection_request_battery_exemption
          }
    }

@ThemePreviews
@Composable
private fun OnboardingWelcomeStepPreview() {
  A11YButtonTheme {
    OnboardingScreen(
        screenState = MainScreenState(),
        step = OnboardingStep.Welcome,
        onStepChange = {},
        onAcceptDisclosure = {},
        onOpenAccessibilitySettings = {},
        onChooseApp = {},
        onRequestBatteryExemption = {},
        onFinish = {},
    )
  }
}

@ThemePreviews
@Composable
private fun OnboardingServiceStepPreview() {
  A11YButtonTheme {
    OnboardingScreen(
        screenState = MainScreenState(disclosureAccepted = true),
        step = OnboardingStep.Service,
        onStepChange = {},
        onAcceptDisclosure = {},
        onOpenAccessibilitySettings = {},
        onChooseApp = {},
        onRequestBatteryExemption = {},
        onFinish = {},
    )
  }
}
