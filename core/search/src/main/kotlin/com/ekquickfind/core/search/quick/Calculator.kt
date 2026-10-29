package com.ekquickfind.core.search.quick

data class Calculation(
    val expression: String,
    val value: Double,
    val formatted: String,
)

/**
 * Evaluates + - * / ^ % and parentheses. "X% of Y" means X/100*Y, and "A + X%" adds X percent of A,
 * like a pocket calculator. Returns null for anything that isn't a real calculation.
 */
object Calculator {
    private val allowed = Regex("^[0-9.eE+\\-−*/×÷^%() ]*$")

    fun evaluate(input: String): Calculation? {
        val expression = input.trim()
        val prepared = expression.replace(Regex("%\\s*of\\s*", RegexOption.IGNORE_CASE), "%*")
        if (prepared.isEmpty() || !allowed.matches(prepared)) return null
        val tokens = tokenize(prepared) ?: return null
        val parser = Parser(tokens)
        val result = runCatching { parser.parseAll() }.getOrNull() ?: return null
        if (!parser.sawOperator || !result.value.isFinite()) return null
        return Calculation(expression, result.value, formatNumber(result.value, maxDecimals = 10))
    }

    private sealed interface Token {
        data class Number(
            val value: Double,
        ) : Token

        data class Op(
            val symbol: Char,
        ) : Token
    }

    private val numberPattern = Regex("\\d+(\\.\\d+)?([eE][+-]?\\d+)?|\\.\\d+")

    private fun tokenize(text: String): List<Token>? {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == ' ' -> i++
                c.isDigit() || c == '.' -> {
                    val match = numberPattern.matchAt(text, i) ?: return null
                    tokens += Token.Number(match.value.toDouble())
                    i += match.value.length
                }
                else -> {
                    tokens +=
                        Token.Op(
                            when (c) {
                                '×' -> '*'
                                '÷' -> '/'
                                '−' -> '-'
                                'e', 'E' -> return null
                                else -> c
                            },
                        )
                    i++
                }
            }
        }
        return tokens
    }

    private data class Value(
        val value: Double,
        val isPercent: Boolean = false,
    )

    private class Parser(
        private val tokens: List<Token>,
    ) {
        private var pos = 0
        var sawOperator = false
            private set

        fun parseAll(): Value {
            val value = expression()
            check(pos == tokens.size)
            return value
        }

        private fun peekOp(): Char? = (tokens.getOrNull(pos) as? Token.Op)?.symbol

        private fun expression(): Value {
            var left = term()
            while (peekOp() == '+' || peekOp() == '-') {
                val op = peekOp()
                pos++
                sawOperator = true
                val right = term()
                val amount = if (right.isPercent && !left.isPercent) left.value * right.value else right.value
                left = Value(if (op == '+') left.value + amount else left.value - amount)
            }
            return left
        }

        private fun term(): Value {
            var left = power()
            while (peekOp() == '*' || peekOp() == '/') {
                val op = peekOp()
                pos++
                sawOperator = true
                val right = power()
                left = Value(if (op == '*') left.value * right.value else left.value / right.value)
            }
            return left
        }

        // Right-associative: 2^3^2 = 2^9.
        private fun power(): Value {
            val base = unary()
            if (peekOp() != '^') return base
            pos++
            sawOperator = true
            return Value(Math.pow(base.value, power().value))
        }

        private fun unary(): Value =
            when (peekOp()) {
                '-' -> {
                    pos++
                    val v = unary()
                    v.copy(value = -v.value)
                }
                '+' -> {
                    pos++
                    unary()
                }
                else -> postfix()
            }

        private fun postfix(): Value {
            val v = primary()
            if (peekOp() != '%') return v
            pos++
            sawOperator = true
            return Value(v.value / 100, isPercent = true)
        }

        private fun primary(): Value =
            when (val token = tokens.getOrNull(pos)) {
                is Token.Number -> {
                    pos++
                    Value(token.value)
                }
                is Token.Op -> {
                    check(token.symbol == '(')
                    pos++
                    val inner = expression()
                    check(peekOp() == ')')
                    pos++
                    inner.copy(isPercent = false)
                }
                null -> error("unexpected end")
            }
    }
}
