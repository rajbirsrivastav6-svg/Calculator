package com.example.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {

  @Test
  fun basicArithmetic_respectsPrecedence() {
    val res = CalculatorEngine.evaluate("2+3×4−6÷2") as EvalResult.Success
    assertEquals("11", res.formatted)
  }

  @Test
  fun decimalPrecision_avoidsFloatingPointDrift() {
    val res = CalculatorEngine.evaluate("0.1+0.2") as EvalResult.Success
    assertEquals("0.3", res.formatted)
  }

  @Test
  fun percentage_worksInAdditionAndMultiplication() {
    val addPct = CalculatorEngine.evaluate("200+10%") as EvalResult.Success
    assertEquals("220", addPct.formatted)

    val subPct = CalculatorEngine.evaluate("200−15%") as EvalResult.Success
    assertEquals("170", subPct.formatted)

    val mulPct = CalculatorEngine.evaluate("200×10%") as EvalResult.Success
    assertEquals("20", mulPct.formatted)

    val standalone = CalculatorEngine.evaluate("50%") as EvalResult.Success
    assertEquals("0.5", standalone.formatted)
  }

  @Test
  fun parenthesesAndImplicitMultiplication_evaluateCorrectly() {
    val res = CalculatorEngine.evaluate("2(3+4)") as EvalResult.Success
    assertEquals("14", res.formatted)

    val unclosed = CalculatorEngine.evaluate("(5+5)×(2+1") as EvalResult.Success
    assertEquals("30", unclosed.formatted)
  }

  @Test
  fun divisionByZero_returnsDivideByZeroError() {
    val res = CalculatorEngine.evaluate("42÷0")
    assertTrue(res is EvalResult.Failure && res.error == CalcError.DivideByZero)
  }

  @Test
  fun viewModel_computesAndRecordsHistory() {
    val vm = CalculatorViewModel()
    vm.onDigit('1')
    vm.onDigit('2')
    vm.onOperator(CalcOperator.MULTIPLY)
    vm.onDigit('5')
    assertEquals("60", vm.uiState.value.previewResult)

    vm.onEquals()
    assertEquals("60", vm.uiState.value.formattedExpression)
    assertEquals(1, vm.uiState.value.history.size)
    assertEquals("12×5", vm.uiState.value.history.first().formattedExpression)
  }
}
