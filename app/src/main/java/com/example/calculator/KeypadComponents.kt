package com.example.calculator

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

enum class KeyRole {
  DIGIT,
  UTILITY,
  OPERATOR,
  CLEAR,
  EQUALS,
}

@Composable
fun CalculatorKeypad(
  onDigit: (Char) -> Unit,
  onDecimal: () -> Unit,
  onOperator: (CalcOperator) -> Unit,
  onParenthesis: () -> Unit,
  onPercent: () -> Unit,
  onBackspace: () -> Unit,
  onClear: () -> Unit,
  onEquals: () -> Unit,
  compactMode: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val spacing = if (compactMode) 8.dp else 11.dp
  val cornerRadius = if (compactMode) 18.dp else 24.dp

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(spacing),
  ) {
    // Row 1: AC, ( ), %, ÷
    Row(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
      CalculatorKeyButton(
        label = "AC",
        role = KeyRole.CLEAR,
        contentDescription = stringResource(R.string.cd_clear),
        testTag = "key_clear",
        cornerRadius = cornerRadius,
        compactMode = compactMode,
        onClick = onClear,
        modifier = Modifier.weight(1f).fillMaxHeight(),
      )
      CalculatorKeyButton(
        label = "( )",
        role = KeyRole.UTILITY,
        contentDescription = stringResource(R.string.cd_parentheses),
        testTag = "key_paren",
        cornerRadius = cornerRadius,
        compactMode = compactMode,
        onClick = onParenthesis,
        modifier = Modifier.weight(1f).fillMaxHeight(),
      )
      CalculatorKeyButton(
        label = "%",
        role = KeyRole.UTILITY,
        contentDescription = stringResource(R.string.cd_percent),
        testTag = "key_percent",
        cornerRadius = cornerRadius,
        compactMode = compactMode,
        onClick = onPercent,
        modifier = Modifier.weight(1f).fillMaxHeight(),
      )
      CalculatorKeyButton(
        label = "÷",
        role = KeyRole.OPERATOR,
        contentDescription = stringResource(R.string.cd_divide),
        testTag = "key_divide",
        cornerRadius = cornerRadius,
        compactMode = compactMode,
        onClick = { onOperator(CalcOperator.DIVIDE) },
        modifier = Modifier.weight(1f).fillMaxHeight(),
      )
    }

    // Row 2: 7, 8, 9, ×
    Row(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
      DigitKey('7', cornerRadius, compactMode, onDigit, Modifier.weight(1f).fillMaxHeight())
      DigitKey('8', cornerRadius, compactMode, onDigit, Modifier.weight(1f).fillMaxHeight())
      DigitKey('9', cornerRadius, compactMode, onDigit, Modifier.weight(1f).fillMaxHeight())
      CalculatorKeyButton(
        label = "×",
        role = KeyRole.OPERATOR,
        contentDescription = stringResource(R.string.cd_multiply),
        testTag = "key_multiply",
        cornerRadius = cornerRadius,
        compactMode = compactMode,
        onClick = { onOperator(CalcOperator.MULTIPLY) },
        modifier = Modifier.weight(1f).fillMaxHeight(),
      )
    }

    // Row 3: 4, 5, 6, −
    Row(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
      DigitKey('4', cornerRadius, compactMode, onDigit, Modifier.weight(1f).fillMaxHeight())
      DigitKey('5', cornerRadius, compactMode, onDigit, Modifier.weight(1f).fillMaxHeight())
      DigitKey('6', cornerRadius, compactMode, onDigit, Modifier.weight(1f).fillMaxHeight())
      CalculatorKeyButton(
        label = "−",
        role = KeyRole.OPERATOR,
        contentDescription = stringResource(R.string.cd_subtract),
        testTag = "key_subtract",
        cornerRadius = cornerRadius,
        compactMode = compactMode,
        onClick = { onOperator(CalcOperator.SUBTRACT) },
        modifier = Modifier.weight(1f).fillMaxHeight(),
      )
    }

    // Row 4: 1, 2, 3, +
    Row(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
      DigitKey('1', cornerRadius, compactMode, onDigit, Modifier.weight(1f).fillMaxHeight())
      DigitKey('2', cornerRadius, compactMode, onDigit, Modifier.weight(1f).fillMaxHeight())
      DigitKey('3', cornerRadius, compactMode, onDigit, Modifier.weight(1f).fillMaxHeight())
      CalculatorKeyButton(
        label = "+",
        role = KeyRole.OPERATOR,
        contentDescription = stringResource(R.string.cd_add),
        testTag = "key_add",
        cornerRadius = cornerRadius,
        compactMode = compactMode,
        onClick = { onOperator(CalcOperator.ADD) },
        modifier = Modifier.weight(1f).fillMaxHeight(),
      )
    }

    // Row 5: 0, ., ⌫, =
    Row(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
      DigitKey('0', cornerRadius, compactMode, onDigit, Modifier.weight(1f).fillMaxHeight())
      CalculatorKeyButton(
        label = ".",
        role = KeyRole.DIGIT,
        contentDescription = stringResource(R.string.cd_decimal),
        testTag = "key_decimal",
        cornerRadius = cornerRadius,
        compactMode = compactMode,
        onClick = onDecimal,
        modifier = Modifier.weight(1f).fillMaxHeight(),
      )
      CalculatorKeyButton(
        label = null,
        icon = Icons.AutoMirrored.Outlined.Backspace,
        role = KeyRole.DIGIT,
        contentDescription = stringResource(R.string.cd_backspace),
        testTag = "key_backspace",
        cornerRadius = cornerRadius,
        compactMode = compactMode,
        onClick = onBackspace,
        onLongClick = onClear,
        modifier = Modifier.weight(1f).fillMaxHeight(),
      )
      CalculatorKeyButton(
        label = "=",
        role = KeyRole.EQUALS,
        contentDescription = stringResource(R.string.cd_equals),
        testTag = "key_equals",
        cornerRadius = cornerRadius,
        compactMode = compactMode,
        onClick = onEquals,
        modifier = Modifier.weight(1f).fillMaxHeight(),
      )
    }
  }
}

@Composable
private fun DigitKey(
  digit: Char,
  cornerRadius: Dp,
  compactMode: Boolean,
  onDigit: (Char) -> Unit,
  modifier: Modifier = Modifier,
) {
  CalculatorKeyButton(
    label = digit.toString(),
    role = KeyRole.DIGIT,
    contentDescription = digit.toString(),
    testTag = "key_$digit",
    cornerRadius = cornerRadius,
    compactMode = compactMode,
    onClick = { onDigit(digit) },
    modifier = modifier,
  )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalculatorKeyButton(
  label: String?,
  role: KeyRole,
  contentDescription: String,
  testTag: String,
  cornerRadius: Dp,
  compactMode: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  onLongClick: (() -> Unit)? = null,
) {
  val haptic = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (isPressed) 0.93f else 1f,
      animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
      label = "key_scale",
    )

  val colorScheme = MaterialTheme.colorScheme
  val containerColor: Color
  val contentColor: Color
  val borderColor: Color

  when (role) {
    KeyRole.DIGIT -> {
      containerColor = colorScheme.surfaceContainerHigh
      contentColor = colorScheme.onSurface
      borderColor = colorScheme.outlineVariant.copy(alpha = 0.35f)
    }
    KeyRole.UTILITY -> {
      containerColor = colorScheme.surfaceContainerHighest
      contentColor = colorScheme.onSurfaceVariant
      borderColor = colorScheme.outlineVariant.copy(alpha = 0.3f)
    }
    KeyRole.OPERATOR -> {
      containerColor = colorScheme.secondaryContainer
      contentColor = colorScheme.onSecondaryContainer
      borderColor = colorScheme.outlineVariant.copy(alpha = 0.25f)
    }
    KeyRole.CLEAR -> {
      containerColor = colorScheme.errorContainer
      contentColor = colorScheme.onErrorContainer
      borderColor = colorScheme.error.copy(alpha = 0.2f)
    }
    KeyRole.EQUALS -> {
      containerColor = colorScheme.primary
      contentColor = colorScheme.onPrimary
      borderColor = Color.Transparent
    }
  }

  val shape = RoundedCornerShape(cornerRadius)
  val fontSize =
    when {
      compactMode && role == KeyRole.CLEAR -> 17.sp
      compactMode && role == KeyRole.UTILITY -> 18.sp
      compactMode -> 22.sp
      role == KeyRole.CLEAR -> 21.sp
      role == KeyRole.UTILITY -> 22.sp
      role == KeyRole.OPERATOR || role == KeyRole.EQUALS -> 30.sp
      else -> 27.sp
    }

  Surface(
    modifier =
      modifier
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .clip(shape)
        .border(width = 1.dp, color = borderColor, shape = shape)
        .combinedClickable(
          interactionSource = interactionSource,
          indication = ripple(bounded = true),
          role = Role.Button,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
          },
          onLongClick =
            onLongClick?.let { longAction ->
              {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                longAction()
              }
            },
        )
        .semantics { this.contentDescription = contentDescription }
        .testTag(testTag),
    shape = shape,
    color = containerColor,
    contentColor = contentColor,
    tonalElevation = if (role == KeyRole.DIGIT) 1.dp else 0.dp,
  ) {
    Box(modifier = Modifier.fillMaxSize().padding(4.dp), contentAlignment = Alignment.Center) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(if (compactMode) 22.dp else 26.dp),
          tint = contentColor,
        )
      } else if (label != null) {
        Text(
          text = label,
          style =
            MaterialTheme.typography.titleMedium.copy(
              fontSize = fontSize,
              fontWeight =
                if (role == KeyRole.EQUALS || role == KeyRole.OPERATOR || role == KeyRole.CLEAR) {
                  FontWeight.SemiBold
                } else {
                  FontWeight.Medium
                },
            ),
          maxLines = 1,
          softWrap = false,
        )
      }
    }
  }
}
