package com.example.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
  uiState: CalculatorUiState,
  isDarkTheme: Boolean,
  onDigit: (Char) -> Unit,
  onDecimal: () -> Unit,
  onOperator: (CalcOperator) -> Unit,
  onParenthesis: () -> Unit,
  onPercent: () -> Unit,
  onBackspace: () -> Unit,
  onClear: () -> Unit,
  onEquals: () -> Unit,
  onToggleHistory: () -> Unit,
  onDismissHistory: () -> Unit,
  onClearHistory: () -> Unit,
  onSelectHistoryItem: (CalculationRecord) -> Unit,
  onToggleTheme: (Boolean) -> Unit,
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  var showCopiedBadge by remember { mutableStateOf(false) }

  LaunchedEffect(showCopiedBadge) {
    if (showCopiedBadge) {
      delay(1500)
      showCopiedBadge = false
    }
  }

  val copyTextToClipboard: () -> Unit = {
    val textToCopy =
      uiState.previewResult.ifEmpty { uiState.formattedExpression }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Calculator result", textToCopy))
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    showCopiedBadge = true
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
    ) {
      val boxMaxWidth = maxWidth
      val boxMaxHeight = maxHeight
      val isWideOrLandscape = boxMaxWidth >= 600.dp || boxMaxWidth > boxMaxHeight

      if (isWideOrLandscape) {
        // Canonical 2-pane adaptive layout for Landscape, Foldables, and Tablets
        Row(
          modifier =
            Modifier.fillMaxSize()
              .padding(horizontal = 20.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
          Column(
            modifier = Modifier.weight(0.46f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            CalculatorTopBar(
              historyCount = uiState.history.size,
              isDarkTheme = isDarkTheme,
              showCopiedBadge = showCopiedBadge,
              showHistoryButton = false,
              onCopyClick = copyTextToClipboard,
              onHistoryClick = onToggleHistory,
              onThemeToggleClick = { onToggleTheme(isDarkTheme) },
            )
            DisplayCard(
              uiState = uiState,
              compactMode = true,
              onCopyClick = copyTextToClipboard,
              modifier = Modifier.fillMaxWidth().weight(0.48f),
            )
            InlineHistoryPanel(
              history = uiState.history,
              onClearHistory = onClearHistory,
              onSelectRecord = onSelectHistoryItem,
              modifier = Modifier.fillMaxWidth().weight(0.52f),
            )
          }

          Box(
            modifier = Modifier.weight(0.54f).fillMaxHeight(),
            contentAlignment = Alignment.Center,
          ) {
            CalculatorKeypad(
              onDigit = onDigit,
              onDecimal = onDecimal,
              onOperator = onOperator,
              onParenthesis = onParenthesis,
              onPercent = onPercent,
              onBackspace = onBackspace,
              onClear = onClear,
              onEquals = onEquals,
              compactMode = boxMaxHeight < 520.dp,
              modifier = Modifier.widthIn(max = 520.dp).fillMaxSize(),
            )
          }
        }
      } else {
        // Portrait handheld layout
        Column(
          modifier =
            Modifier.fillMaxSize()
              .widthIn(max = 600.dp)
              .align(Alignment.Center)
              .padding(horizontal = 16.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          CalculatorTopBar(
            historyCount = uiState.history.size,
            isDarkTheme = isDarkTheme,
            showCopiedBadge = showCopiedBadge,
            showHistoryButton = true,
            onCopyClick = copyTextToClipboard,
            onHistoryClick = onToggleHistory,
            onThemeToggleClick = { onToggleTheme(isDarkTheme) },
          )

          DisplayCard(
            uiState = uiState,
            compactMode = false,
            onCopyClick = copyTextToClipboard,
            modifier = Modifier.fillMaxWidth().weight(0.35f),
          )

          CalculatorKeypad(
            onDigit = onDigit,
            onDecimal = onDecimal,
            onOperator = onOperator,
            onParenthesis = onParenthesis,
            onPercent = onPercent,
            onBackspace = onBackspace,
            onClear = onClear,
            onEquals = onEquals,
            compactMode = false,
            modifier = Modifier.fillMaxWidth().weight(0.65f),
          )
        }
      }
    }
  }

  if (uiState.isHistoryOpen) {
    BackHandler { onDismissHistory() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = onDismissHistory,
      sheetState = sheetState,
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.onSurface,
      shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
      HistorySheetContent(
        history = uiState.history,
        onClearHistory = onClearHistory,
        onSelectRecord = onSelectHistoryItem,
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
      )
    }
  }
}

@Composable
private fun CalculatorTopBar(
  historyCount: Int,
  isDarkTheme: Boolean,
  showCopiedBadge: Boolean,
  showHistoryButton: Boolean,
  onCopyClick: () -> Unit,
  onHistoryClick: () -> Unit,
  onThemeToggleClick: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box(
        modifier =
          Modifier.size(10.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
      )
      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
      )
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      AnimatedVisibility(
        visible = showCopiedBadge,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(250)),
      ) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          shape = RoundedCornerShape(50),
        ) {
          Text(
            text = stringResource(R.string.copied_to_clipboard),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      IconButton(
        onClick = onCopyClick,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("btn_copy"),
      ) {
        Icon(
          imageVector = if (showCopiedBadge) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
          contentDescription = stringResource(R.string.cd_copy_result),
          tint =
            if (showCopiedBadge) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (showHistoryButton) {
        IconButton(
          onClick = onHistoryClick,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("btn_history"),
        ) {
          BadgedBox(
            badge = {
              if (historyCount > 0) {
                Badge(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                  Text(text = if (historyCount > 99) "99+" else historyCount.toString())
                }
              }
            }
          ) {
            Icon(
              imageVector = Icons.Outlined.History,
              contentDescription = stringResource(R.string.cd_history),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      IconButton(
        onClick = onThemeToggleClick,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("btn_theme_toggle"),
      ) {
        Icon(
          imageVector =
            if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun DisplayCard(
  uiState: CalculatorUiState,
  compactMode: Boolean,
  onCopyClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val scrollState = rememberScrollState()

  LaunchedEffect(uiState.formattedExpression) {
    scrollState.animateScrollTo(scrollState.maxValue)
  }

  val targetFontSize =
    when {
      compactMode && uiState.formattedExpression.length <= 9 -> 42f
      compactMode && uiState.formattedExpression.length <= 14 -> 32f
      compactMode -> 24f
      uiState.formattedExpression.length <= 8 -> 58f
      uiState.formattedExpression.length <= 12 -> 46f
      uiState.formattedExpression.length <= 16 -> 36f
      else -> 28f
    }
  val animatedFontSize by
    animateFloatAsState(
      targetValue = targetFontSize,
      animationSpec = tween(durationMillis = 120),
      label = "display_font_size",
    )

  val operatorColor = colorScheme.primary
  val parenColor = colorScheme.tertiary
  val defaultTextColor =
    if (uiState.error != null && uiState.isResultDisplayed) colorScheme.error
    else colorScheme.onSurface

  val styledExpression =
    remember(uiState.formattedExpression, operatorColor, parenColor, defaultTextColor) {
      buildAnnotatedString {
        for (ch in uiState.formattedExpression) {
          when (ch) {
            '+', '−', '×', '÷', '%' ->
              withStyle(SpanStyle(color = operatorColor, fontWeight = FontWeight.SemiBold)) {
                append(ch)
              }
            '(', ')' ->
              withStyle(SpanStyle(color = parenColor, fontWeight = FontWeight.Medium)) {
                append(ch)
              }
            else ->
              withStyle(SpanStyle(color = defaultTextColor)) {
                append(ch)
              }
          }
        }
      }
    }

  val cardGradient =
    Brush.verticalGradient(
      colors = listOf(colorScheme.surfaceContainerLow, colorScheme.surfaceContainer),
    )

  Surface(
    modifier =
      modifier
        .clip(RoundedCornerShape(28.dp))
        .clickable(onClick = onCopyClick),
    shape = RoundedCornerShape(28.dp),
    color = Color.Transparent,
    border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.45f)),
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(cardGradient)
          .padding(horizontal = 22.dp, vertical = if (compactMode) 14.dp else 20.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End,
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .horizontalScroll(scrollState),
          contentAlignment = Alignment.CenterEnd,
        ) {
          Text(
            text = styledExpression,
            style =
              MaterialTheme.typography.displayLarge.copy(
                fontSize = animatedFontSize.sp,
                lineHeight = (animatedFontSize * 1.15f).sp,
                fontWeight = if (uiState.isResultDisplayed) FontWeight.Bold else FontWeight.Medium,
              ),
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.End,
            modifier = Modifier.testTag("display_expression"),
          )
        }

        Spacer(modifier = Modifier.height(if (compactMode) 6.dp else 10.dp))

        val errorText =
          when (uiState.error) {
            CalcError.DivideByZero -> stringResource(R.string.error_divide_by_zero)
            CalcError.InvalidExpression -> stringResource(R.string.error_invalid_expression)
            CalcError.Overflow -> stringResource(R.string.error_overflow)
            null -> null
          }

        AnimatedContent(
          targetState = errorText ?: uiState.previewResult,
          transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(120)) },
          label = "preview_or_error",
        ) { bottomText ->
          if (bottomText.isNotEmpty()) {
            val isErr = errorText != null
            Text(
              text = if (isErr) bottomText else "= $bottomText",
              style =
                if (isErr) {
                  MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                } else {
                  MaterialTheme.typography.headlineMedium.copy(
                    fontSize = if (compactMode) 22.sp else 26.sp,
                  )
                },
              color = if (isErr) colorScheme.error else colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              textAlign = TextAlign.End,
              modifier = Modifier.fillMaxWidth().testTag("display_preview"),
            )
          } else {
            Spacer(modifier = Modifier.height(if (compactMode) 24.dp else 32.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun InlineHistoryPanel(
  history: List<CalculationRecord>,
  onClearHistory: () -> Unit,
  onSelectRecord: (CalculationRecord) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
  ) {
    HistorySheetContent(
      history = history,
      onClearHistory = onClearHistory,
      onSelectRecord = onSelectRecord,
      modifier = Modifier.fillMaxSize().padding(top = 12.dp),
    )
  }
}

@Composable
private fun HistorySheetContent(
  history: List<CalculationRecord>,
  onClearHistory: () -> Unit,
  onSelectRecord: (CalculationRecord) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column {
        Text(
          text = stringResource(R.string.history_title),
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        if (history.isNotEmpty()) {
          Text(
            text = stringResource(R.string.tap_to_reuse),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      if (history.isNotEmpty()) {
        TextButton(
          onClick = onClearHistory,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("btn_clear_history"),
        ) {
          Icon(
            imageVector = Icons.Outlined.DeleteSweep,
            contentDescription = stringResource(R.string.cd_clear_history),
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = stringResource(R.string.history_clear_all))
        }
      }
    }

    HorizontalDivider(
      modifier = Modifier.padding(top = 8.dp),
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )

    if (history.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 36.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(40.dp),
          )
          Text(
            text = stringResource(R.string.history_empty_title),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = stringResource(R.string.history_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxWidth().height(340.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(history, key = { it.id }) { record ->
          Surface(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onSelectRecord(record) },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
              horizontalAlignment = Alignment.End,
            ) {
              Text(
                text = record.formattedExpression,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "= ${record.formattedResult}",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}
