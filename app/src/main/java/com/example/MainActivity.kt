package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calculator.CalculatorScreen
import com.example.calculator.CalculatorViewModel
import com.example.ui.theme.CalculatorTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: CalculatorViewModel = viewModel()
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()
      val systemDark = isSystemInDarkTheme()
      val isDark = uiState.darkThemeOverride ?: systemDark

      CalculatorTheme(darkTheme = isDark) {
        CalculatorScreen(
          uiState = uiState,
          isDarkTheme = isDark,
          onDigit = viewModel::onDigit,
          onDecimal = viewModel::onDecimal,
          onOperator = viewModel::onOperator,
          onParenthesis = viewModel::onParenthesis,
          onPercent = viewModel::onPercent,
          onBackspace = viewModel::onBackspace,
          onClear = viewModel::onClear,
          onEquals = viewModel::onEquals,
          onToggleHistory = viewModel::onToggleHistory,
          onDismissHistory = viewModel::onDismissHistory,
          onClearHistory = viewModel::onClearHistory,
          onSelectHistoryItem = { record -> viewModel.onSelectHistoryItem(record) },
          onToggleTheme = viewModel::onToggleTheme,
        )
      }
    }
  }
}
