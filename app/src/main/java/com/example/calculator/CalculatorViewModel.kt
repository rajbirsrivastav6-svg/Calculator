package com.example.calculator

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CalculationRecord(
  val id: Long,
  val formattedExpression: String,
  val rawExpression: String,
  val formattedResult: String,
  val plainResult: String,
)

data class CalculatorUiState(
  val rawExpression: String = "",
  val formattedExpression: String = "0",
  val previewResult: String = "",
  val isResultDisplayed: Boolean = false,
  val error: CalcError? = null,
  val history: List<CalculationRecord> = emptyList(),
  val isHistoryOpen: Boolean = false,
  val darkThemeOverride: Boolean? = null,
)

class CalculatorViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(CalculatorUiState())
  val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

  private var nextRecordId = 1L

  fun onDigit(digit: Char) {
    if (digit !in '0'..'9') return
    _uiState.update { state ->
      val baseExpr =
        if (state.isResultDisplayed || state.error != null) "" else state.rawExpression

      val currentNumber = currentNumberSlice(baseExpr)
      val nextExpr =
        when {
          currentNumber == "0" && digit == '0' -> baseExpr
          currentNumber == "0" && digit != '0' -> baseExpr.dropLast(1) + digit
          baseExpr.lastOrNull() == '%' -> "${baseExpr}×$digit"
          else -> baseExpr + digit
        }
      recomputeState(state.copy(rawExpression = nextExpr, isResultDisplayed = false, error = null))
    }
  }

  fun onDecimal() {
    _uiState.update { state ->
      if (state.isResultDisplayed || state.error != null) {
        return@update recomputeState(
          state.copy(rawExpression = "0.", isResultDisplayed = false, error = null)
        )
      }
      val expr = state.rawExpression
      val currentNumber = currentNumberSlice(expr)
      if (currentNumber.contains('.')) return@update state

      val last = expr.lastOrNull()
      val nextExpr =
        when {
          expr.isEmpty() || isOperatorChar(last) || last == '(' -> "${expr}0."
          last == ')' || last == '%' -> "${expr}×0."
          else -> "$expr."
        }
      recomputeState(state.copy(rawExpression = nextExpr, isResultDisplayed = false, error = null))
    }
  }

  fun onOperator(op: CalcOperator) {
    _uiState.update { state ->
      if (state.error != null) return@update state
      val expr = state.rawExpression
      val sym = op.symbol

      if (expr.isEmpty()) {
        val nextExpr = if (op == CalcOperator.SUBTRACT) sym.toString() else "0$sym"
        return@update recomputeState(state.copy(rawExpression = nextExpr, isResultDisplayed = false))
      }

      val last = expr.last()
      val nextExpr =
        when {
          expr == "−" && op != CalcOperator.SUBTRACT -> ""
          last == '(' -> if (op == CalcOperator.SUBTRACT) "$expr$sym" else expr
          isOperatorChar(last) -> {
            // Allow negative sign after × or ÷ (e.g. 5×−2), otherwise replace trailing operator(s)
            if (op == CalcOperator.SUBTRACT && (last == '×' || last == '÷')) {
              "$expr$sym"
            } else {
              var trimmed = expr
              while (trimmed.isNotEmpty() && isOperatorChar(trimmed.last())) {
                // Don't strip if only leading unary minus before nothing? Handled above.
                val prevBeforeLast = trimmed.dropLast(1).lastOrNull()
                if (trimmed.length == 1 && trimmed.last() == '−') break
                if (prevBeforeLast == '(' && trimmed.last() == '−') break
                trimmed = trimmed.dropLast(1)
              }
              if (trimmed.isEmpty() || trimmed.last() == '(') expr else "$trimmed$sym"
            }
          }
          else -> "$expr$sym"
        }

      recomputeState(state.copy(rawExpression = nextExpr, isResultDisplayed = false, error = null))
    }
  }

  fun onParenthesis() {
    _uiState.update { state ->
      if (state.isResultDisplayed || state.error != null) {
        return@update recomputeState(
          state.copy(rawExpression = "(", isResultDisplayed = false, error = null)
        )
      }
      val expr = state.rawExpression
      val openCount = expr.count { it == '(' }
      val closeCount = expr.count { it == ')' }
      val last = expr.lastOrNull()

      val canClose =
        openCount > closeCount &&
          last != null &&
          (last.isDigit() || last == '.' || last == ')' || last == '%')

      val nextExpr = if (canClose) "$expr)" else "$expr("
      recomputeState(state.copy(rawExpression = nextExpr, isResultDisplayed = false, error = null))
    }
  }

  fun onPercent() {
    _uiState.update { state ->
      if (state.error != null) return@update state
      val expr = state.rawExpression
      val last = expr.lastOrNull() ?: return@update state
      if (!last.isDigit() && last != ')') return@update state

      val nextExpr = "$expr%"
      recomputeState(state.copy(rawExpression = nextExpr, isResultDisplayed = false, error = null))
    }
  }

  fun onBackspace() {
    _uiState.update { state ->
      if (state.error != null) {
        return@update state.copy(
          rawExpression = "",
          formattedExpression = "0",
          previewResult = "",
          isResultDisplayed = false,
          error = null,
        )
      }
      if (state.rawExpression.isEmpty()) return@update state
      val nextExpr = state.rawExpression.dropLast(1)
      recomputeState(state.copy(rawExpression = nextExpr, isResultDisplayed = false, error = null))
    }
  }

  fun onClear() {
    _uiState.update { state ->
      state.copy(
        rawExpression = "",
        formattedExpression = "0",
        previewResult = "",
        isResultDisplayed = false,
        error = null,
      )
    }
  }

  fun onEquals() {
    _uiState.update { state ->
      val expr = state.rawExpression
      if (expr.isEmpty() || state.error != null) return@update state

      val last = expr.last()
      if (isOperatorChar(last) || last == '(') {
        return@update state
      }

      when (val result = CalculatorEngine.evaluate(expr)) {
        is EvalResult.Success -> {
          val formattedExpr = CalculatorEngine.formatExpressionForDisplay(expr)
          val updatedHistory =
            if (CalculatorEngine.hasOperatorOrPercentOrParen(expr)) {
              val record =
                CalculationRecord(
                  id = nextRecordId++,
                  formattedExpression = formattedExpr,
                  rawExpression = expr,
                  formattedResult = result.formatted,
                  plainResult = result.plain,
                )
              (listOf(record) + state.history).take(50)
            } else {
              state.history
            }

          state.copy(
            rawExpression = result.plain,
            formattedExpression = result.formatted,
            previewResult = "",
            isResultDisplayed = true,
            error = null,
            history = updatedHistory,
          )
        }
        is EvalResult.Failure -> {
          state.copy(previewResult = "", error = result.error)
        }
      }
    }
  }

  fun onToggleHistory() {
    _uiState.update { it.copy(isHistoryOpen = !it.isHistoryOpen) }
  }

  fun onDismissHistory() {
    _uiState.update { it.copy(isHistoryOpen = false) }
  }

  fun onClearHistory() {
    _uiState.update { it.copy(history = emptyList()) }
  }

  fun onSelectHistoryItem(record: CalculationRecord, useResult: Boolean = true) {
    _uiState.update { state ->
      if (useResult) {
        val nextExpr =
          if (state.rawExpression.isNotEmpty() && isOperatorChar(state.rawExpression.last()) && !state.isResultDisplayed) {
            state.rawExpression + record.plainResult
          } else {
            record.plainResult
          }
        recomputeState(
          state.copy(
            rawExpression = nextExpr,
            isResultDisplayed = false,
            error = null,
            isHistoryOpen = false,
          )
        )
      } else {
        recomputeState(
          state.copy(
            rawExpression = record.rawExpression,
            isResultDisplayed = false,
            error = null,
            isHistoryOpen = false,
          )
        )
      }
    }
  }

  fun onToggleTheme(currentlyDark: Boolean) {
    _uiState.update { it.copy(darkThemeOverride = !currentlyDark) }
  }

  private fun recomputeState(state: CalculatorUiState): CalculatorUiState {
    val expr = state.rawExpression
    val formattedExpr = CalculatorEngine.formatExpressionForDisplay(expr)
    if (expr.isEmpty()) {
      return state.copy(formattedExpression = "0", previewResult = "", error = null)
    }

    val last = expr.last()
    val canPreview =
      CalculatorEngine.hasOperatorOrPercentOrParen(expr) &&
        !isOperatorChar(last) &&
        last != '('

    if (!canPreview) {
      return state.copy(formattedExpression = formattedExpr, previewResult = "", error = null)
    }

    return when (val eval = CalculatorEngine.evaluate(expr)) {
      is EvalResult.Success -> {
        state.copy(
          formattedExpression = formattedExpr,
          previewResult = eval.formatted,
          error = null,
        )
      }
      is EvalResult.Failure -> {
        val previewError = if (eval.error == CalcError.DivideByZero) eval.error else null
        state.copy(
          formattedExpression = formattedExpr,
          previewResult = "",
          error = previewError,
        )
      }
    }
  }

  private fun currentNumberSlice(expr: String): String {
    var idx = expr.length - 1
    while (idx >= 0) {
      val ch = expr[idx]
      if (ch.isDigit() || ch == '.') {
        idx--
      } else {
        break
      }
    }
    return expr.substring(idx + 1)
  }

  private fun isOperatorChar(ch: Char?): Boolean {
    return ch == '+' || ch == '−' || ch == '-' || ch == '×' || ch == '÷'
  }
}
