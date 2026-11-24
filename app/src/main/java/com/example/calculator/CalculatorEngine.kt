package com.example.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class CalcOperator(val symbol: Char) {
  ADD('+'),
  SUBTRACT('−'),
  MULTIPLY('×'),
  DIVIDE('÷'),
}

sealed class CalcError {
  data object DivideByZero : CalcError()
  data object InvalidExpression : CalcError()
  data object Overflow : CalcError()
}

sealed class EvalResult {
  data class Success(val value: BigDecimal, val formatted: String, val plain: String) : EvalResult()
  data class Failure(val error: CalcError) : EvalResult()
}

object CalculatorEngine {

  private val mathContext = MathContext(16, RoundingMode.HALF_UP)
  private val maxAbsValue = BigDecimal("1E100")
  private val scientificUpperBound = BigDecimal("1E12")
  private val scientificLowerBound = BigDecimal("1E-9")

  private sealed class Token {
    data class Number(val value: BigDecimal, val isPercent: Boolean = false) : Token()
    data class Operator(val op: CalcOperator) : Token()
    data object LeftParen : Token()
    data object RightParen : Token()
  }

  private class Parser(private val tokens: List<Token>) {
    private var pos = 0

    private fun peek(): Token? = tokens.getOrNull(pos)
    private fun consume(): Token = tokens[pos++]

    fun parse(): BigDecimal {
      val value = parseAddSub()
      if (pos != tokens.size) {
        throw IllegalArgumentException("Unexpected trailing tokens")
      }
      return value
    }

    private fun parseAddSub(): BigDecimal {
      var left = parseMulDiv()
      while (true) {
        val next = peek()
        if (next is Token.Operator && (next.op == CalcOperator.ADD || next.op == CalcOperator.SUBTRACT)) {
          consume()
          val rightToken = peek()
          val afterRight = tokens.getOrNull(pos + 1)
          val isSimplePercentTerm =
            rightToken is Token.Number &&
              rightToken.isPercent &&
              (afterRight == null ||
                afterRight is Token.RightParen ||
                (afterRight is Token.Operator &&
                  (afterRight.op == CalcOperator.ADD || afterRight.op == CalcOperator.SUBTRACT)))

          val right =
            if (isSimplePercentTerm) {
              consume()
              val pct = rightToken.value.divide(BigDecimal(100), mathContext)
              left.multiply(pct, mathContext)
            } else {
              parseMulDiv()
            }

          left =
            if (next.op == CalcOperator.ADD) left.add(right, mathContext)
            else left.subtract(right, mathContext)
        } else {
          break
        }
      }
      return left
    }

    private fun parseMulDiv(): BigDecimal {
      var left = parseUnary()
      while (true) {
        val next = peek()
        if (next is Token.Operator && (next.op == CalcOperator.MULTIPLY || next.op == CalcOperator.DIVIDE)) {
          consume()
          val right = parseUnary()
          left =
            if (next.op == CalcOperator.MULTIPLY) {
              left.multiply(right, mathContext)
            } else {
              if (right.compareTo(BigDecimal.ZERO) == 0) {
                throw ArithmeticException("Division by zero")
              }
              left.divide(right, mathContext)
            }
        } else {
          break
        }
      }
      return left
    }

    private fun parseUnary(): BigDecimal {
      val next = peek()
      if (next is Token.Operator && next.op == CalcOperator.SUBTRACT) {
        consume()
        return parseUnary().negate(mathContext)
      }
      if (next is Token.Operator && next.op == CalcOperator.ADD) {
        consume()
        return parseUnary()
      }
      return parsePrimary()
    }

    private fun parsePrimary(): BigDecimal {
      return when (val token = peek()) {
        is Token.Number -> {
          consume()
          if (token.isPercent) token.value.divide(BigDecimal(100), mathContext)
          else token.value
        }
        is Token.LeftParen -> {
          consume()
          val inner = parseAddSub()
          if (peek() is Token.RightParen) {
            consume()
          }
          inner
        }
        else -> throw IllegalArgumentException("Unexpected token: $token")
      }
    }
  }

  fun evaluate(rawExpression: String): EvalResult {
    val trimmed = rawExpression.trim()
    if (trimmed.isEmpty()) {
      return EvalResult.Success(BigDecimal.ZERO, "0", "0")
    }

    return try {
      val balanced = balanceParentheses(trimmed)
      val tokens = tokenize(balanced)
      if (tokens.isEmpty()) {
        return EvalResult.Failure(CalcError.InvalidExpression)
      }
      val normalizedTokens = insertImplicitMultiplication(tokens)
      val value = Parser(normalizedTokens).parse()
      if (value.abs() > maxAbsValue) {
        return EvalResult.Failure(CalcError.Overflow)
      }
      val cleaned = cleanBigDecimal(value)
      EvalResult.Success(
        value = cleaned,
        formatted = formatDisplayNumber(cleaned),
        plain = formatPlainNumber(cleaned),
      )
    } catch (_: ArithmeticException) {
      EvalResult.Failure(CalcError.DivideByZero)
    } catch (_: IllegalArgumentException) {
      EvalResult.Failure(CalcError.InvalidExpression)
    }
  }

  fun hasOperatorOrPercentOrParen(expression: String): Boolean {
    return expression.anyIndexed { index, ch ->
      when (ch) {
        '+', '×', '÷', '%', '(', ')' -> true
        '−', '-' -> {
          val prev = expression.getOrNull(index - 1)
          prev != 'E' && prev != 'e' && index > 0
        }
        else -> false
      }
    }
  }

  private inline fun String.anyIndexed(predicate: (Int, Char) -> Boolean): Boolean {
    for (i in indices) {
      if (predicate(i, this[i])) return true
    }
    return false
  }

  fun formatExpressionForDisplay(expression: String): String {
    if (expression.isEmpty()) return "0"
    val out = StringBuilder(expression.length + 8)
    var i = 0
    while (i < expression.length) {
      val ch = expression[i]
      if (ch.isDigit() || ch == '.') {
        val start = i
        var hasExponent = false
        while (i < expression.length) {
          val c = expression[i]
          if (c.isDigit() || c == '.') {
            i++
          } else if (c == 'E' || c == 'e') {
            hasExponent = true
            i++
            if (i < expression.length && (expression[i] == '+' || expression[i] == '-' || expression[i] == '−')) {
              i++
            }
            while (i < expression.length && expression[i].isDigit()) {
              i++
            }
            break
          } else {
            break
          }
        }
        val numSlice = expression.substring(start, i)
        out.append(if (hasExponent) numSlice else formatPartialNumberToken(numSlice))
      } else {
        out.append(ch)
        i++
      }
    }
    return out.toString()
  }

  private fun formatPartialNumberToken(token: String): String {
    val dotIndex = token.indexOf('.')
    val intPart = if (dotIndex >= 0) token.substring(0, dotIndex) else token
    val fracPart = if (dotIndex >= 0) token.substring(dotIndex) else ""
    if (intPart.length <= 3) return token

    val grouped = StringBuilder()
    var count = 0
    for (idx in intPart.length - 1 downTo 0) {
      grouped.append(intPart[idx])
      count++
      if (count == 3 && idx > 0) {
        grouped.append(',')
        count = 0
      }
    }
    return grouped.reverse().toString() + fracPart
  }

  private fun balanceParentheses(expr: String): String {
    var openCount = 0
    for (ch in expr) {
      if (ch == '(') openCount++
      else if (ch == ')' && openCount > 0) openCount--
    }
    return if (openCount > 0) expr + ")".repeat(openCount) else expr
  }

  private fun tokenize(expr: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var i = 0
    while (i < expr.length) {
      val ch = expr[i]
      when {
        ch.isWhitespace() -> i++
        ch.isDigit() || ch == '.' -> {
          val start = i
          var dotSeen = ch == '.'
          i++
          while (i < expr.length) {
            val c = expr[i]
            if (c.isDigit()) {
              i++
            } else if (c == '.' && !dotSeen) {
              dotSeen = true
              i++
            } else if (c == 'E' || c == 'e') {
              i++
              if (i < expr.length && (expr[i] == '+' || expr[i] == '-' || expr[i] == '−')) i++
              while (i < expr.length && expr[i].isDigit()) i++
              break
            } else {
              break
            }
          }
          var numStr = expr.substring(start, i).replace('−', '-')
          if (numStr == ".") numStr = "0"
          if (numStr.startsWith(".")) numStr = "0$numStr"
          val bd = numStr.toBigDecimalOrNull() ?: throw IllegalArgumentException("Invalid number: $numStr")
          var isPercent = false
          if (i < expr.length && expr[i] == '%') {
            isPercent = true
            i++
          }
          tokens.add(Token.Number(bd, isPercent))
        }
        ch == '%' -> {
          if (tokens.lastOrNull() is Token.RightParen) {
            tokens.add(Token.Operator(CalcOperator.MULTIPLY))
            tokens.add(Token.Number(BigDecimal("0.01"), isPercent = false))
          } else {
            throw IllegalArgumentException("Unexpected %")
          }
          i++
        }
        ch == '+' -> {
          tokens.add(Token.Operator(CalcOperator.ADD))
          i++
        }
        ch == '−' || ch == '-' -> {
          tokens.add(Token.Operator(CalcOperator.SUBTRACT))
          i++
        }
        ch == '×' || ch == '*' -> {
          tokens.add(Token.Operator(CalcOperator.MULTIPLY))
          i++
        }
        ch == '÷' || ch == '/' -> {
          tokens.add(Token.Operator(CalcOperator.DIVIDE))
          i++
        }
        ch == '(' -> {
          tokens.add(Token.LeftParen)
          i++
        }
        ch == ')' -> {
          tokens.add(Token.RightParen)
          i++
        }
        else -> throw IllegalArgumentException("Unexpected character: $ch")
      }
    }
    return tokens
  }

  private fun insertImplicitMultiplication(tokens: List<Token>): List<Token> {
    if (tokens.size < 2) return tokens
    val result = mutableListOf<Token>()
    for (idx in tokens.indices) {
      val current = tokens[idx]
      if (idx > 0) {
        val prev = tokens[idx - 1]
        val needsMultiply =
          (prev is Token.Number && current is Token.LeftParen) ||
            (prev is Token.RightParen && current is Token.LeftParen) ||
            (prev is Token.RightParen && current is Token.Number)
        if (needsMultiply) {
          result.add(Token.Operator(CalcOperator.MULTIPLY))
        }
      }
      result.add(current)
    }
    return result
  }

  private fun cleanBigDecimal(value: BigDecimal): BigDecimal {
    if (value.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
    val rounded = value.setScale(12, RoundingMode.HALF_UP).stripTrailingZeros()
    return if (rounded.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO else rounded
  }

  fun formatDisplayNumber(value: BigDecimal): String {
    if (value.compareTo(BigDecimal.ZERO) == 0) return "0"
    val absVal = value.abs()
    if (absVal >= scientificUpperBound || (absVal > BigDecimal.ZERO && absVal < scientificLowerBound)) {
      val symbols = DecimalFormatSymbols(Locale.US)
      return DecimalFormat("0.######E0", symbols).format(value).replace('-', '−')
    }

    val plain = value.toPlainString()
    val isNegative = plain.startsWith("-")
    val unsigned = if (isNegative) plain.substring(1) else plain
    val dotIdx = unsigned.indexOf('.')
    val intPart = if (dotIdx >= 0) unsigned.substring(0, dotIdx) else unsigned
    val fracPart = if (dotIdx >= 0) unsigned.substring(dotIdx) else ""

    val grouped = StringBuilder()
    var count = 0
    for (i in intPart.length - 1 downTo 0) {
      grouped.append(intPart[i])
      count++
      if (count == 3 && i > 0) {
        grouped.append(',')
        count = 0
      }
    }
    val intFormatted = grouped.reverse().toString()
    return (if (isNegative) "−" else "") + intFormatted + fracPart
  }

  fun formatPlainNumber(value: BigDecimal): String {
    if (value.compareTo(BigDecimal.ZERO) == 0) return "0"
    val absVal = value.abs()
    if (absVal >= scientificUpperBound || (absVal > BigDecimal.ZERO && absVal < scientificLowerBound)) {
      val symbols = DecimalFormatSymbols(Locale.US)
      return DecimalFormat("0.######E0", symbols).format(value).replace('-', '−')
    }
    return value.toPlainString().replace('-', '−')
  }
}
